-- Performance optimization indexes and constraints
-- Migration V3: Add database indexes for performance optimizations

-- ============================================================================
-- COMPOSITE INDEXES FOR PROGRESS ANALYTICS
-- ============================================================================

-- Composite index for user progress queries by date range
-- Optimizes queries like: SELECT * FROM yoga_sessions WHERE user_id = ? AND completed_at BETWEEN ? AND ?
CREATE INDEX idx_yoga_sessions_user_date_range ON yoga_sessions(user_id, completed_at DESC);

-- Composite index for progress analytics with duration
-- Optimizes aggregation queries: SELECT SUM(duration_minutes) FROM yoga_sessions WHERE user_id = ? AND completed_at >= ?
CREATE INDEX idx_yoga_sessions_user_date_duration ON yoga_sessions(user_id, completed_at DESC, duration_minutes);

-- Composite index for calorie tracking analytics
-- Optimizes queries: SELECT SUM(calories_burned) FROM yoga_sessions WHERE user_id = ? AND completed_at BETWEEN ? AND ?
CREATE INDEX idx_yoga_sessions_user_date_calories ON yoga_sessions(user_id, completed_at DESC, calories_burned);

-- Index for monthly/weekly aggregations (date truncation queries)
CREATE INDEX idx_yoga_sessions_completed_date_only ON yoga_sessions(DATE(completed_at), user_id);

-- ============================================================================
-- TEXT SEARCH INDEXES FOR YOGA CLASSES
-- ============================================================================

-- Full-text search index for class title and description (PostgreSQL specific)
-- Note: This will be ignored in H2 but works in PostgreSQL
CREATE INDEX IF NOT EXISTS idx_yoga_classes_text_search ON yoga_classes 
USING gin(to_tsvector('english', title || ' ' || COALESCE(description, '') || ' ' || COALESCE(instructor, '')));

-- Composite index for filtered class searches
-- Optimizes queries with multiple filters: difficulty + duration range
CREATE INDEX idx_yoga_classes_difficulty_duration ON yoga_classes(difficulty_level, duration_minutes);

-- Index for instructor-based searches with case-insensitive support
CREATE INDEX idx_yoga_classes_instructor_lower ON yoga_classes(LOWER(instructor));

-- Index for title searches with case-insensitive support
CREATE INDEX idx_yoga_classes_title_lower ON yoga_classes(LOWER(title));

-- ============================================================================
-- FAVORITES AND USER ACTIVITY INDEXES
-- ============================================================================

-- Composite index for user favorites with creation date for sorting
CREATE INDEX idx_class_favorites_user_created ON class_favorites(user_id, created_at DESC);

-- Index for popular classes analytics (count favorites per class)
CREATE INDEX idx_class_favorites_class_created ON class_favorites(class_id, created_at);

-- ============================================================================
-- USER PROFILE INDEXES
-- ============================================================================

-- Index for fitness level filtering (if we add features to find users by fitness level)
CREATE INDEX idx_users_fitness_level ON users(fitness_level) WHERE fitness_level IS NOT NULL;

-- Index for user activity tracking (last updated profiles)
CREATE INDEX idx_users_updated_at ON users(updated_at DESC);

-- ============================================================================
-- SESSION ANALYTICS INDEXES
-- ============================================================================

-- Index for class popularity analytics (sessions per class)
CREATE INDEX idx_yoga_sessions_class_completed ON yoga_sessions(class_id, completed_at DESC) WHERE class_id IS NOT NULL;

-- Index for duration-based analytics and filtering
CREATE INDEX idx_yoga_sessions_duration_completed ON yoga_sessions(duration_minutes, completed_at DESC);

-- Partial index for sessions with calories data (not all sessions may have this)
CREATE INDEX idx_yoga_sessions_calories_not_null ON yoga_sessions(user_id, calories_burned, completed_at) 
WHERE calories_burned IS NOT NULL;

-- ============================================================================
-- REFRESH TOKEN PERFORMANCE INDEXES
-- ============================================================================

-- Composite index for token cleanup queries (expired tokens)
CREATE INDEX idx_refresh_tokens_expiry_created ON refresh_tokens(expiry_date, created_at);

-- Index for active token lookups
CREATE INDEX idx_refresh_tokens_token_expiry ON refresh_tokens(token, expiry_date) WHERE expiry_date > CURRENT_TIMESTAMP;

-- ============================================================================
-- ADDITIONAL CONSTRAINTS FOR DATA INTEGRITY
-- ============================================================================

-- Add check constraint for valid difficulty levels
ALTER TABLE yoga_classes ADD CONSTRAINT chk_yoga_classes_difficulty 
CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'));

-- Add check constraint for valid fitness levels
ALTER TABLE users ADD CONSTRAINT chk_users_fitness_level 
CHECK (fitness_level IS NULL OR fitness_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'));

-- Add check constraint for valid OAuth providers
ALTER TABLE users ADD CONSTRAINT chk_users_provider 
CHECK (provider IN ('GOOGLE', 'APPLE'));

-- Ensure video URLs are not empty strings
ALTER TABLE yoga_classes ADD CONSTRAINT chk_yoga_classes_video_url 
CHECK (video_url IS NOT NULL AND LENGTH(TRIM(video_url)) > 0);

-- Ensure email is not empty
ALTER TABLE users ADD CONSTRAINT chk_users_email 
CHECK (email IS NOT NULL AND LENGTH(TRIM(email)) > 0);

-- ============================================================================
-- PERFORMANCE STATISTICS UPDATE
-- ============================================================================

-- Update table statistics for PostgreSQL query planner (PostgreSQL specific)
-- This helps the query planner make better decisions about index usage
-- Note: These commands will be ignored in H2

-- Analyze tables to update statistics
ANALYZE users;
ANALYZE yoga_classes;
ANALYZE yoga_sessions;
ANALYZE class_favorites;
ANALYZE refresh_tokens;