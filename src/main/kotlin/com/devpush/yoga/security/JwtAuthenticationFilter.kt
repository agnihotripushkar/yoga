package com.devpush.yoga.security

import com.devpush.yoga.features.auth.service.JwtTokenManager
import com.devpush.yoga.service.UserService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenManager: JwtTokenManager,
    private val userService: UserService
) : OncePerRequestFilter() {
    
    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    
    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        
        private val PUBLIC_ENDPOINTS = setOf(
            "/api/auth/google/login",
            "/api/auth/apple/login",
            "/api/auth/refresh",
            "/actuator/health",
            "/error"
        )
    }
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // Skip JWT processing for public endpoints
            if (isPublicEndpoint(request.requestURI)) {
                filterChain.doFilter(request, response)
                return
            }
            
            val jwt = extractJwtFromRequest(request)
            
            if (jwt != null && SecurityContextHolder.getContext().authentication == null) {
                authenticateUser(jwt, request)
            }
        } catch (ex: Exception) {
            logger.error("Cannot set user authentication for ${request.requestURI}", ex)
            // Clear security context on error
            SecurityContextHolder.clearContext()
        }
        
        filterChain.doFilter(request, response)
    }
    
    /**
     * Extract JWT token from Authorization header
     */
    private fun extractJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER)
        
        return if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }
    
    /**
     * Authenticate user based on JWT token
     */
    private fun authenticateUser(jwt: String, request: HttpServletRequest) {
        // Validate token and extract user information
        val userInfo = jwtTokenManager.getUserInfoFromAccessToken(jwt)
        
        if (userInfo != null) {
            // Verify user still exists in database
            val userOptional = userService.findById(userInfo.id)
            
            if (userOptional.isPresent) {
                val user = userOptional.get()
                
                // Create UserDetails for Spring Security
                val userDetails = createUserDetails(user)
                
                // Create authentication token
                val authToken = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.authorities
                )
                
                // Set authentication details
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                
                // Set authentication in security context
                SecurityContextHolder.getContext().authentication = authToken
                
                logger.debug("Successfully authenticated user: ${user.email}")
            } else {
                logger.warn("User with ID ${userInfo.id} not found in database")
            }
        } else {
            logger.debug("Invalid or expired JWT token")
        }
    }
    
    /**
     * Check if the request URI is a public endpoint that doesn't require authentication
     */
    private fun isPublicEndpoint(requestUri: String): Boolean {
        return PUBLIC_ENDPOINTS.contains(requestUri) || 
               requestUri.startsWith("/actuator/") ||
               requestUri == "/error"
    }
    
    /**
     * Create UserDetails from User entity
     */
    private fun createUserDetails(user: com.devpush.yoga.entity.User): UserDetails {
        return User.builder()
            .username(user.email)
            .password("") // No password needed for OAuth users
            .authorities(emptyList()) // No specific roles for now
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build()
    }
}