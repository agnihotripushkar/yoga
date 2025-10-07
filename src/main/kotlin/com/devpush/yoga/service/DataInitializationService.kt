package com.devpush.yoga.service

import com.devpush.yoga.entity.DifficultyLevel
import com.devpush.yoga.entity.YogaClass
import com.devpush.yoga.repository.YogaClassRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Profile("!prod") // Only run in non-production environments
class DataInitializationService(
    private val yogaClassRepository: YogaClassRepository
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DataInitializationService::class.java)

    @Transactional
    override fun run(vararg args: String?) {
        if (yogaClassRepository.count() == 0L) {
            logger.info("Initializing database with sample yoga classes...")
            createSampleYogaClasses()
            logger.info("Sample data initialization completed. Created ${yogaClassRepository.count()} yoga classes.")
        } else {
            logger.info("Database already contains ${yogaClassRepository.count()} yoga classes. Skipping initialization.")
        }
    }

    private fun createSampleYogaClasses() {
        val sampleClasses = mutableListOf<YogaClass>()

        // Beginner Classes
        sampleClasses.add(
            YogaClass(
                title = "Gentle Morning Flow",
                description = "A gentle 20-minute morning yoga flow perfect for beginners. Start your day with mindful movement and breath work to energize your body and calm your mind.",
                durationMinutes = 20,
                difficultyLevel = DifficultyLevel.BEGINNER,
                instructor = "Sarah Johnson",
                videoUrl = "https://example.com/videos/gentle-morning-flow",
                thumbnailUrl = "https://example.com/thumbnails/gentle-morning-flow.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Basic Hatha Yoga",
                description = "Learn fundamental yoga poses with proper alignment in this beginner-friendly Hatha class. Focus on building strength, flexibility, and body awareness.",
                durationMinutes = 30,
                difficultyLevel = DifficultyLevel.BEGINNER,
                instructor = "Michael Chen",
                videoUrl = "https://example.com/videos/basic-hatha-yoga",
                thumbnailUrl = "https://example.com/thumbnails/basic-hatha-yoga.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Yoga for Stress Relief",
                description = "A calming 25-minute practice designed to reduce stress and tension. Includes gentle stretches, breathing exercises, and relaxation techniques.",
                durationMinutes = 25,
                difficultyLevel = DifficultyLevel.BEGINNER,
                instructor = "Emma Williams",
                videoUrl = "https://example.com/videos/stress-relief-yoga",
                thumbnailUrl = "https://example.com/thumbnails/stress-relief-yoga.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Chair Yoga for Beginners",
                description = "Accessible yoga practice that can be done entirely from a chair. Perfect for office workers or those with limited mobility.",
                durationMinutes = 15,
                difficultyLevel = DifficultyLevel.BEGINNER,
                instructor = "David Martinez",
                videoUrl = "https://example.com/videos/chair-yoga-beginners",
                thumbnailUrl = "https://example.com/thumbnails/chair-yoga-beginners.jpg"
            )
        )

        // Intermediate Classes
        sampleClasses.add(
            YogaClass(
                title = "Vinyasa Flow Power",
                description = "Dynamic 45-minute vinyasa flow linking breath with movement. Build strength, flexibility, and endurance through flowing sequences.",
                durationMinutes = 45,
                difficultyLevel = DifficultyLevel.INTERMEDIATE,
                instructor = "Lisa Thompson",
                videoUrl = "https://example.com/videos/vinyasa-flow-power",
                thumbnailUrl = "https://example.com/thumbnails/vinyasa-flow-power.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Core Strength Yoga",
                description = "Targeted 35-minute practice focusing on building core strength and stability. Includes challenging poses and sequences to strengthen your center.",
                durationMinutes = 35,
                difficultyLevel = DifficultyLevel.INTERMEDIATE,
                instructor = "James Rodriguez",
                videoUrl = "https://example.com/videos/core-strength-yoga",
                thumbnailUrl = "https://example.com/thumbnails/core-strength-yoga.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Hip Opening Flow",
                description = "Release tension and increase mobility in your hips with this 40-minute intermediate practice. Perfect for desk workers and athletes.",
                durationMinutes = 40,
                difficultyLevel = DifficultyLevel.INTERMEDIATE,
                instructor = "Rachel Green",
                videoUrl = "https://example.com/videos/hip-opening-flow",
                thumbnailUrl = "https://example.com/thumbnails/hip-opening-flow.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Warrior Sequence",
                description = "Build strength and confidence with this empowering 30-minute warrior-focused practice. Develop balance, focus, and inner strength.",
                durationMinutes = 30,
                difficultyLevel = DifficultyLevel.INTERMEDIATE,
                instructor = "Alex Kumar",
                videoUrl = "https://example.com/videos/warrior-sequence",
                thumbnailUrl = "https://example.com/thumbnails/warrior-sequence.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Backbend Preparation",
                description = "Safely prepare your body for deeper backbends with this 50-minute intermediate class. Focus on opening the chest and strengthening the back.",
                durationMinutes = 50,
                difficultyLevel = DifficultyLevel.INTERMEDIATE,
                instructor = "Sophie Anderson",
                videoUrl = "https://example.com/videos/backbend-preparation",
                thumbnailUrl = "https://example.com/thumbnails/backbend-preparation.jpg"
            )
        )

        // Advanced Classes
        sampleClasses.add(
            YogaClass(
                title = "Advanced Arm Balances",
                description = "Challenge yourself with complex arm balances and inversions in this 60-minute advanced practice. Requires significant upper body strength.",
                durationMinutes = 60,
                difficultyLevel = DifficultyLevel.ADVANCED,
                instructor = "Marcus Lee",
                videoUrl = "https://example.com/videos/advanced-arm-balances",
                thumbnailUrl = "https://example.com/thumbnails/advanced-arm-balances.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Power Ashtanga Primary",
                description = "Traditional Ashtanga Primary Series for advanced practitioners. 75 minutes of challenging, flowing sequences with deep breathing.",
                durationMinutes = 75,
                difficultyLevel = DifficultyLevel.ADVANCED,
                instructor = "Priya Sharma",
                videoUrl = "https://example.com/videos/power-ashtanga-primary",
                thumbnailUrl = "https://example.com/thumbnails/power-ashtanga-primary.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Advanced Inversions",
                description = "Master headstands, handstands, and forearm stands in this 55-minute advanced inversion workshop. Focus on alignment and safety.",
                durationMinutes = 55,
                difficultyLevel = DifficultyLevel.ADVANCED,
                instructor = "Daniel Kim",
                videoUrl = "https://example.com/videos/advanced-inversions",
                thumbnailUrl = "https://example.com/thumbnails/advanced-inversions.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Deep Backbend Flow",
                description = "Explore advanced backbends including wheel pose variations and scorpion pose in this challenging 65-minute practice.",
                durationMinutes = 65,
                difficultyLevel = DifficultyLevel.ADVANCED,
                instructor = "Isabella Garcia",
                videoUrl = "https://example.com/videos/deep-backbend-flow",
                thumbnailUrl = "https://example.com/thumbnails/deep-backbend-flow.jpg"
            )
        )

        // Specialized Classes
        sampleClasses.add(
            YogaClass(
                title = "Prenatal Gentle Flow",
                description = "Safe and nurturing yoga practice designed specifically for expecting mothers. Focus on breath, gentle movement, and relaxation.",
                durationMinutes = 35,
                difficultyLevel = DifficultyLevel.BEGINNER,
                instructor = "Maria Santos",
                videoUrl = "https://example.com/videos/prenatal-gentle-flow",
                thumbnailUrl = "https://example.com/thumbnails/prenatal-gentle-flow.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Yin Yoga Deep Stretch",
                description = "Passive, meditative practice holding poses for 3-5 minutes each. Perfect for deep tissue release and mental relaxation.",
                durationMinutes = 60,
                difficultyLevel = DifficultyLevel.INTERMEDIATE,
                instructor = "Thomas Wilson",
                videoUrl = "https://example.com/videos/yin-yoga-deep-stretch",
                thumbnailUrl = "https://example.com/thumbnails/yin-yoga-deep-stretch.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Hot Yoga Flow",
                description = "Intense 90-minute heated yoga practice combining strength, flexibility, and endurance. Prepare to sweat and detoxify.",
                durationMinutes = 90,
                difficultyLevel = DifficultyLevel.ADVANCED,
                instructor = "Carlos Mendez",
                videoUrl = "https://example.com/videos/hot-yoga-flow",
                thumbnailUrl = "https://example.com/thumbnails/hot-yoga-flow.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Restorative Evening Practice",
                description = "Wind down with this gentle 40-minute restorative practice using props. Perfect for evening relaxation and better sleep.",
                durationMinutes = 40,
                difficultyLevel = DifficultyLevel.BEGINNER,
                instructor = "Jennifer Taylor",
                videoUrl = "https://example.com/videos/restorative-evening-practice",
                thumbnailUrl = "https://example.com/thumbnails/restorative-evening-practice.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Power Yoga Sculpt",
                description = "High-intensity 50-minute practice combining yoga with light weights. Build lean muscle while improving flexibility and balance.",
                durationMinutes = 50,
                difficultyLevel = DifficultyLevel.INTERMEDIATE,
                instructor = "Ryan Mitchell",
                videoUrl = "https://example.com/videos/power-yoga-sculpt",
                thumbnailUrl = "https://example.com/thumbnails/power-yoga-sculpt.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Meditation and Breathwork",
                description = "Focus on mindfulness, meditation techniques, and pranayama breathing exercises in this calming 30-minute practice.",
                durationMinutes = 30,
                difficultyLevel = DifficultyLevel.BEGINNER,
                instructor = "Zen Master Liu",
                videoUrl = "https://example.com/videos/meditation-breathwork",
                thumbnailUrl = "https://example.com/thumbnails/meditation-breathwork.jpg"
            )
        )

        sampleClasses.add(
            YogaClass(
                title = "Athletic Recovery Yoga",
                description = "Designed for athletes and active individuals. 45-minute practice focusing on muscle recovery, injury prevention, and flexibility.",
                durationMinutes = 45,
                difficultyLevel = DifficultyLevel.INTERMEDIATE,
                instructor = "Coach Amanda Brown",
                videoUrl = "https://example.com/videos/athletic-recovery-yoga",
                thumbnailUrl = "https://example.com/thumbnails/athletic-recovery-yoga.jpg"
            )
        )

        try {
            yogaClassRepository.saveAll(sampleClasses)
            logger.info("Successfully saved ${sampleClasses.size} sample yoga classes")
        } catch (e: Exception) {
            logger.error("Error saving sample yoga classes: ${e.message}", e)
            throw e
        }
    }
}