package com.devpush.yoga.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.devpush.yoga.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class JwtAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {
    
    private val logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint::class.java)
    
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        logger.error("Unauthorized error: {}", authException.message)
        
        // Set response status and content type
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        
        // Create error response
        val errorResponse = ErrorResponse(
            error = "Unauthorized",
            message = "Authentication required to access this resource",
            status = HttpServletResponse.SC_UNAUTHORIZED,
            timestamp = LocalDateTime.now()
        )
        
        // Write error response to output stream
        response.outputStream.println(objectMapper.writeValueAsString(errorResponse))
    }
}