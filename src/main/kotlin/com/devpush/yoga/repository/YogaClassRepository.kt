package com.devpush.yoga.repository

import com.devpush.yoga.entity.DifficultyLevel
import com.devpush.yoga.entity.YogaClass
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface YogaClassRepository : JpaRepository<YogaClass, Long> {
    
    fun findByDifficultyLevel(difficultyLevel: DifficultyLevel, pageable: Pageable): Page<YogaClass>
    
    fun findByDurationMinutesBetween(minDuration: Int, maxDuration: Int, pageable: Pageable): Page<YogaClass>
    
    fun findByInstructorContainingIgnoreCase(instructor: String, pageable: Pageable): Page<YogaClass>
    
    @Query("""
        SELECT c FROM YogaClass c 
        WHERE (:difficultyLevel IS NULL OR c.difficultyLevel = :difficultyLevel)
        AND (:minDuration IS NULL OR c.durationMinutes >= :minDuration)
        AND (:maxDuration IS NULL OR c.durationMinutes <= :maxDuration)
        AND (:instructor IS NULL OR LOWER(c.instructor) LIKE LOWER(CONCAT('%', :instructor, '%')))
    """)
    fun findByFilters(
        @Param("difficultyLevel") difficultyLevel: DifficultyLevel?,
        @Param("minDuration") minDuration: Int?,
        @Param("maxDuration") maxDuration: Int?,
        @Param("instructor") instructor: String?,
        pageable: Pageable
    ): Page<YogaClass>
    
    @Query("""
        SELECT c FROM YogaClass c 
        WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        OR LOWER(c.instructor) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    """)
    fun searchByTitleDescriptionOrInstructor(
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable
    ): Page<YogaClass>
}