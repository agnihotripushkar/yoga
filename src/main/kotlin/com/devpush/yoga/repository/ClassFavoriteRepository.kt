package com.devpush.yoga.repository

import com.devpush.yoga.entity.ClassFavorite
import com.devpush.yoga.entity.User
import com.devpush.yoga.entity.YogaClass
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ClassFavoriteRepository : JpaRepository<ClassFavorite, Long> {
    
    fun findByUserAndYogaClass(user: User, yogaClass: YogaClass): Optional<ClassFavorite>
    
    fun findByUserOrderByCreatedAtDesc(user: User): List<ClassFavorite>
    
    fun findByUserOrderByCreatedAtDesc(user: User, pageable: Pageable): Page<ClassFavorite>
    
    fun existsByUserAndYogaClass(user: User, yogaClass: YogaClass): Boolean
    
    fun deleteByUserAndYogaClass(user: User, yogaClass: YogaClass): Int
    
    fun countByUser(user: User): Long
    
    @Query("""
        SELECT cf.yogaClass FROM ClassFavorite cf 
        WHERE cf.user = :user 
        ORDER BY cf.createdAt DESC
    """)
    fun findFavoriteClassesByUser(@Param("user") user: User): List<YogaClass>
    
    @Query("""
        SELECT cf.yogaClass FROM ClassFavorite cf 
        WHERE cf.user = :user 
        ORDER BY cf.createdAt DESC
    """)
    fun findFavoriteClassesByUser(@Param("user") user: User, pageable: Pageable): Page<YogaClass>
}