package com.devpush.yoga.repository

import com.devpush.yoga.entity.DifficultyLevel
import com.devpush.yoga.entity.YogaClass
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import jakarta.persistence.QueryHint

@Repository
interface YogaClassRepository : JpaRepository<YogaClass, Long> {
    
    // Optimized difficulty level query using index
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findByDifficultyLevel(difficultyLevel: DifficultyLevel, pageable: Pageable): Page<YogaClass>
    
    // Optimized duration range query using index
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findByDurationMinutesBetween(minDuration: Int, maxDuration: Int, pageable: Pageable): Page<YogaClass>
    
    // Optimized instructor search using lowercase index
    @Query("""
        SELECT c FROM YogaClass c 
        WHERE LOWER(c.instructor) LIKE LOWER(CONCAT('%', :instructor, '%'))
        ORDER BY c.title
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findByInstructorContainingIgnoreCase(@Param("instructor") instructor: String, pageable: Pageable): Page<YogaClass>
    
    // Optimized multi-filter query using composite indexes
    @Query("""
        SELECT c FROM YogaClass c 
        WHERE (:difficultyLevel IS NULL OR c.difficultyLevel = :difficultyLevel)
        AND (:minDuration IS NULL OR c.durationMinutes >= :minDuration)
        AND (:maxDuration IS NULL OR c.durationMinutes <= :maxDuration)
        AND (:instructor IS NULL OR LOWER(c.instructor) LIKE LOWER(CONCAT('%', :instructor, '%')))
        ORDER BY c.title
    """)
    @QueryHints(
        QueryHint(name = "org.hibernate.cacheable", value = "true"),
        QueryHint(name = "org.hibernate.fetchSize", value = "50")
    )
    fun findByFilters(
        @Param("difficultyLevel") difficultyLevel: DifficultyLevel?,
        @Param("minDuration") minDuration: Int?,
        @Param("maxDuration") maxDuration: Int?,
        @Param("instructor") instructor: String?,
        pageable: Pageable
    ): Page<YogaClass>
    
    // Optimized text search using lowercase indexes
    @Query("""
        SELECT c FROM YogaClass c 
        WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(c.instructor) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY 
            CASE 
                WHEN LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) THEN 1
                WHEN LOWER(c.instructor) LIKE LOWER(CONCAT('%', :searchTerm, '%')) THEN 2
                ELSE 3
            END,
            c.title
    """)
    @QueryHints(
        QueryHint(name = "org.hibernate.cacheable", value = "true"),
        QueryHint(name = "org.hibernate.fetchSize", value = "50")
    )
    fun searchByTitleDescriptionOrInstructor(
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable
    ): Page<YogaClass>
    
    // ============================================================================
    // ADVANCED SEARCH AND ANALYTICS QUERIES
    // ============================================================================
    
    // PostgreSQL full-text search (falls back to LIKE for H2)
    @Query(value = """
        SELECT * FROM yoga_classes c
        WHERE to_tsvector('english', c.title || ' ' || COALESCE(c.description, '') || ' ' || COALESCE(c.instructor, ''))
        @@ plainto_tsquery('english', :searchTerm)
        ORDER BY ts_rank(to_tsvector('english', c.title || ' ' || COALESCE(c.description, '') || ' ' || COALESCE(c.instructor, '')), 
                         plainto_tsquery('english', :searchTerm)) DESC
    """, 
    countQuery = """
        SELECT COUNT(*) FROM yoga_classes c
        WHERE to_tsvector('english', c.title || ' ' || COALESCE(c.description, '') || ' ' || COALESCE(c.instructor, ''))
        @@ plainto_tsquery('english', :searchTerm)
    """,
    nativeQuery = true)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun fullTextSearch(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<YogaClass>
    
    // Fallback search for H2 database
    @Query("""
        SELECT c FROM YogaClass c 
        WHERE LOWER(c.title || ' ' || COALESCE(c.description, '') || ' ' || COALESCE(c.instructor, '')) 
        LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY 
            CASE 
                WHEN LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) THEN 1
                WHEN LOWER(c.instructor) LIKE LOWER(CONCAT('%', :searchTerm, '%')) THEN 2
                ELSE 3
            END,
            c.title
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun fallbackTextSearch(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<YogaClass>
    
    // Popular classes based on session count
    @Query("""
        SELECT c, COUNT(s) as sessionCount
        FROM YogaClass c 
        LEFT JOIN YogaSession s ON s.yogaClass = c
        GROUP BY c
        ORDER BY sessionCount DESC, c.title
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findPopularClasses(pageable: Pageable): Page<Array<Any>>
    
    // Classes by difficulty with session statistics
    @Query("""
        SELECT c.difficultyLevel, COUNT(c) as classCount, AVG(c.durationMinutes) as avgDuration
        FROM YogaClass c 
        GROUP BY c.difficultyLevel
        ORDER BY c.difficultyLevel
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun getClassStatsByDifficulty(): List<Array<Any>>
    
    // Classes by instructor with counts
    @Query("""
        SELECT c.instructor, COUNT(c) as classCount, AVG(c.durationMinutes) as avgDuration
        FROM YogaClass c 
        WHERE c.instructor IS NOT NULL
        GROUP BY c.instructor
        ORDER BY classCount DESC, c.instructor
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun getClassStatsByInstructor(pageable: Pageable): Page<Array<Any>>
    
    // Recently added classes
    @Query("""
        SELECT c FROM YogaClass c 
        ORDER BY c.createdAt DESC, c.title
    """)
    @QueryHints(
        QueryHint(name = "org.hibernate.cacheable", value = "true"),
        QueryHint(name = "org.hibernate.fetchSize", value = "20")
    )
    fun findRecentlyAdded(pageable: Pageable): Page<YogaClass>
    
    // Classes similar to a given class (same difficulty and similar duration)
    @Query("""
        SELECT c FROM YogaClass c 
        WHERE c.id != :classId
        AND c.difficultyLevel = :difficultyLevel
        AND ABS(c.durationMinutes - :duration) <= :durationTolerance
        ORDER BY ABS(c.durationMinutes - :duration), c.title
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findSimilarClasses(
        @Param("classId") classId: Long,
        @Param("difficultyLevel") difficultyLevel: DifficultyLevel,
        @Param("duration") duration: Int,
        @Param("durationTolerance") durationTolerance: Int = 10,
        pageable: Pageable
    ): Page<YogaClass>
    
    // Classes recommended for user's fitness level
    @Query("""
        SELECT c FROM YogaClass c 
        WHERE (:userFitnessLevel = 'BEGINNER' AND c.difficultyLevel IN ('BEGINNER'))
        OR (:userFitnessLevel = 'INTERMEDIATE' AND c.difficultyLevel IN ('BEGINNER', 'INTERMEDIATE'))
        OR (:userFitnessLevel = 'ADVANCED' AND c.difficultyLevel IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
        ORDER BY c.difficultyLevel, c.durationMinutes, c.title
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findRecommendedForFitnessLevel(
        @Param("userFitnessLevel") userFitnessLevel: String,
        pageable: Pageable
    ): Page<YogaClass>
}