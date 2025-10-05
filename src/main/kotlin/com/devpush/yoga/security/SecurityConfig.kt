package com.devpush.yoga.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint
) {
    
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Disable CSRF as we're using JWT tokens
            .csrf { it.disable() }
            
            // Configure CORS
            .cors { it.configurationSource(corsConfigurationSource()) }
            
            // Configure session management - stateless for JWT
            .sessionManagement { 
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) 
            }
            
            // Configure authorization rules
            .authorizeHttpRequests { authz ->
                authz
                    // Public endpoints - no authentication required
                    .requestMatchers(
                        "/api/auth/google/login",
                        "/api/auth/apple/login",
                        "/api/auth/refresh",
                        "/actuator/health",
                        "/error"
                    ).permitAll()
                    
                    // All other endpoints require authentication
                    .anyRequest().authenticated()
            }
            
            // Configure exception handling
            .exceptionHandling { 
                it.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            }
            
            // Add JWT authentication filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        
        return http.build()
    }
    
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        
        // Allow requests from mobile app origins
        configuration.allowedOriginPatterns = listOf(
            "http://localhost:*",  // Development
            "https://*.devpush.com",  // Production domains
            "capacitor://localhost",  // Capacitor mobile apps
            "ionic://localhost"  // Ionic mobile apps
        )
        
        // Allow common HTTP methods
        configuration.allowedMethods = listOf(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        )
        
        // Allow common headers
        configuration.allowedHeaders = listOf(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        )
        
        // Allow credentials (cookies, authorization headers)
        configuration.allowCredentials = true
        
        // Cache preflight response for 1 hour
        configuration.maxAge = 3600L
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        
        return source
    }
}