-- Initial database schema for yoga app

-- Create users table with enhanced profile fields
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    profile_picture VARCHAR(500),
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    bio VARCHAR(500),
    fitness_level VARCHAR(20),
    preferences VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id)
);

-- Create indexes for users table
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider ON users(provider);

-- Create yoga_classes table
CREATE TABLE yoga_classes (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    difficulty_level VARCHAR(20) NOT NULL,
    instructor VARCHAR(100),
    video_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for yoga_classes table
CREATE INDEX idx_yoga_classes_title ON yoga_classes(title);
CREATE INDEX idx_yoga_classes_instructor ON yoga_classes(instructor);
CREATE INDEX idx_yoga_classes_difficulty ON yoga_classes(difficulty_level);
CREATE INDEX idx_yoga_classes_duration ON yoga_classes(duration_minutes);

-- Create yoga_sessions table
CREATE TABLE yoga_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    class_id BIGINT,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    calories_burned INTEGER CHECK (calories_burned >= 0),
    completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500),
    CONSTRAINT fk_yoga_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_yoga_sessions_class FOREIGN KEY (class_id) REFERENCES yoga_classes(id) ON DELETE SET NULL
);

-- Create indexes for yoga_sessions table
CREATE INDEX idx_yoga_sessions_user_id ON yoga_sessions(user_id);
CREATE INDEX idx_yoga_sessions_completed_at ON yoga_sessions(completed_at);
CREATE INDEX idx_yoga_sessions_user_completed ON yoga_sessions(user_id, completed_at);

-- Create class_favorites table
CREATE TABLE class_favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_favorites_class FOREIGN KEY (class_id) REFERENCES yoga_classes(id) ON DELETE CASCADE,
    CONSTRAINT uk_class_favorites_user_class UNIQUE (user_id, class_id)
);

-- Create indexes for class_favorites table
CREATE INDEX idx_class_favorites_user_id ON class_favorites(user_id);
CREATE INDEX idx_class_favorites_class_id ON class_favorites(class_id);

-- Create refresh_tokens table (if not already exists from auth system)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index for refresh_tokens table
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry ON refresh_tokens(expiry_date);