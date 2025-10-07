package com.devpush.yoga.features.auth.service

import com.devpush.yoga.features.auth.dto.OAuthUserInfo
import com.devpush.yoga.entity.OAuthProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration

@Service
class GoogleTokenValidator(
    private val webClient: WebClient.Builder
) {
    
    private val logger = LoggerFactory.getLogger(GoogleTokenValidator::class.java)
    
    @Value("\${oauth.google.client-id:}")
    private lateinit var googleClientId: String
    
    private val googleTokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo"
    
    /**
     * Validates a Google ID token and extracts user information
     */
    fun validateToken(idToken: String): OAuthUserInfo {
        logger.debug("Validating Google ID token")
        
        try {
            val response = webClient.build()
                .get()
                .uri { uriBuilder ->
                    uriBuilder
                        .scheme("https")
                        .host("oauth2.googleapis.com")
                        .path("/tokeninfo")
                        .queryParam("id_token", idToken)
                        .build()
                }
                .retrieve()
                .bodyToMono(GoogleTokenResponse::class.java)
                .timeout(Duration.ofSeconds(10))
                .block()
                
            if (response == null) {
                logger.error("Received null response from Google token validation")
                throw GoogleTokenValidationException("Invalid response from Google")
            }
            
            if (googleClientId.isNotBlank() && response.aud != googleClientId) {
                logger.error("Token audience mismatch. Expected: $googleClientId, Got: ${response.aud}")
                throw GoogleTokenValidationException("Invalid token audience")
            }
            
            if (response.email_verified != "true") {
                logger.error("Google email not verified for user: ${response.email}")
                throw GoogleTokenValidationException("Email not verified")
            }
            
            logger.info("Successfully validated Google token for user: ${response.email}")
            
            return OAuthUserInfo(
                providerId = response.sub,
                email = response.email,
                name = response.name,
                profilePicture = response.picture,
                provider = OAuthProvider.GOOGLE
            )
            
        } catch (ex: WebClientResponseException) {
            logger.error("Google token validation failed with status: ${ex.statusCode}, body: ${ex.responseBodyAsString}")
            when (ex.statusCode.value()) {
                400 -> throw GoogleTokenValidationException("Invalid token format")
                401 -> throw GoogleTokenValidationException("Token expired or invalid")
                else -> throw GoogleTokenValidationException("Google service error: ${ex.statusCode}")
            }
        } catch (ex: Exception) {
            logger.error("Unexpected error during Google token validation", ex)
            throw GoogleTokenValidationException("Token validation failed: ${ex.message}")
        }
    }
    
    /**
     * Data class representing Google's token info response
     */
    private data class GoogleTokenResponse(
        val sub: String,           // User ID
        val email: String,         // User email
        val email_verified: String, // Email verification status
        val name: String?,         // User full name
        val picture: String?,      // Profile picture URL
        val aud: String,           // Audience (client ID)
        val iss: String,           // Issuer
        val exp: String            // Expiration time
    )
}

/**
 * Exception thrown when Google token validation fails
 */
class GoogleTokenValidationException(message: String, cause: Throwable? = null) : 
    RuntimeException(message, cause)