package com.devpush.yoga.features.wellness.controller

import com.devpush.yoga.features.wellness.dto.HealthRecordRequest
import com.devpush.yoga.features.wellness.dto.HealthRecordResponse
import com.devpush.yoga.features.wellness.service.WellnessService
import com.devpush.yoga.features.auth.service.JwtTokenManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/wellness")
@Tag(name = "Wellness Tracking", description = "Health and wellness data management")
class WellnessController(
    private val wellnessService: WellnessService,
    private val jwtTokenManager: JwtTokenManager
) {
    
    private val logger = LoggerFactory.getLogger(WellnessController::class.java)
    
    @Operation(
        summary = "Log Daily Health Data",
        description = "Create or update health record for a specific date (defaults to today)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Health data logged successfully",
                content = [Content(schema = Schema(implementation = HealthRecordResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @PostMapping("/log")
    fun logHealthData(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String,
        @Valid @RequestBody request: HealthRecordRequest
    ): ResponseEntity<HealthRecordResponse> {
        logger.info("Received health data log request")
        
        val userId = extractAndValidateUserId(authorization)
        val response = wellnessService.logHealthData(userId, request)
        
        return ResponseEntity.ok(response)
    }
    
    @Operation(
        summary = "Get Health History",
        description = "Retrieve health records for a date range",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Health history retrieved successfully",
                content = [Content(schema = Schema(implementation = Array<HealthRecordResponse>::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @GetMapping("/history")
    fun getHealthHistory(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String,
        @Parameter(description = "Start date (YYYY-MM-DD)")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "End date (YYYY-MM-DD)")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<HealthRecordResponse>> {
        logger.debug("Received health history request")
        
        val userId = extractAndValidateUserId(authorization)
        val history = wellnessService.getHealthRecords(userId, startDate, endDate)
        
        return ResponseEntity.ok(history)
    }
    
    @Operation(
        summary = "Get Latest Health Record",
        description = "Retrieve the most recent health record",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Latest record retrieved successfully",
                content = [Content(schema = Schema(implementation = HealthRecordResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "No health records found"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @GetMapping("/latest")
    fun getLatestHealthRecord(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<HealthRecordResponse> {
        logger.debug("Received latest health record request")
        
        val userId = extractAndValidateUserId(authorization)
        val record = wellnessService.getLatestHealthRecord(userId)
        
        return if (record != null) {
            ResponseEntity.ok(record)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    private fun extractAndValidateUserId(authorization: String): UUID {
        val token = if (authorization.startsWith("Bearer ", ignoreCase = true)) {
            authorization.substring(7)
        } else {
            throw RuntimeException("Invalid authorization header format")
        }
        
        val userId = jwtTokenManager.getUserIdFromToken(token)
            ?: throw RuntimeException("Invalid or expired access token")
        
        if (!jwtTokenManager.isAccessToken(token)) {
            throw RuntimeException("Invalid token type. Access token required")
        }
        
        return userId
    }
}
