package com.devpush.yoga.features.auth.service

import com.devpush.yoga.features.auth.dto.OAuthUserInfo
import com.devpush.yoga.entity.OAuthProvider
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class AppleTokenValidator(
    private val webClient: WebClient.Builder,
    private val objectMapper: ObjectMapper
) {
    
    private val logger = LoggerFactory.getLogger(AppleTokenValidator::class.java)
    
    @Value("\${oauth.apple.team-id:}")
    private lateinit var appleTeamId: String
    
    @Value("\${oauth.apple.client-id:}")
    private lateinit var appleClientId: String
    
    private val appleKeysUrl = "https://appleid.apple.com/auth/keys"
    private val appleIssuer = "https://appleid.apple.com"
    
    // Cache for Apple's public keys
    private val publicKeysCache = ConcurrentHashMap<String, PublicKey>()
    private var keysCacheExpiry: Long = 0
    private val cacheValidityDuration = Duration.ofHours(1).toMillis()
    
    /**
     * Validates an Apple ID token and extracts user information
     */
    fun validateToken(idToken: String): OAuthUserInfo {
        logger.debug("Validating Apple ID token")
        
        try {
            val headerJson = extractJwtHeader(idToken)
            val keyId = headerJson.get("kid")?.asText()
                ?: throw AppleTokenValidationException("Missing key ID in token header")
            
            val publicKey = getApplePublicKey(keyId)
            
            val claims = Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(appleIssuer)
                .build()
                .parseSignedClaims(idToken)
                .payload
            
            if (appleClientId.isNotBlank()) {
                val audience = claims.audience
                if (audience.isEmpty() || !audience.contains(appleClientId)) {
                    logger.error("Token audience mismatch. Expected: $appleClientId, Got: $audience")
                    throw AppleTokenValidationException("Invalid token audience")
                }
            }
            
            val subject = claims.subject
                ?: throw AppleTokenValidationException("Missing subject in token")
            
            val email = claims.get("email", String::class.java)
                ?: throw AppleTokenValidationException("Missing email in token")
            
            val emailVerified = claims.get("email_verified", String::class.java)
            if (emailVerified != "true") {
                logger.error("Apple email not verified for user: $email")
                throw AppleTokenValidationException("Email not verified")
            }
            
            val name = claims.get("name", String::class.java)
            
            logger.info("Successfully validated Apple token for user: $email")
            
            return OAuthUserInfo(
                providerId = subject,
                email = email,
                name = name,
                profilePicture = null,
                provider = OAuthProvider.APPLE
            )
            
        } catch (ex: Exception) {
            when (ex) {
                is AppleTokenValidationException -> throw ex
                else -> {
                    logger.error("Unexpected error during Apple token validation", ex)
                    throw AppleTokenValidationException("Token validation failed: ${ex.message}")
                }
            }
        }
    }
    
    private fun extractJwtHeader(token: String): JsonNode {
        val parts = token.split(".")
        if (parts.size != 3) {
            throw AppleTokenValidationException("Invalid JWT token format")
        }
        
        val headerBytes = Base64.getUrlDecoder().decode(parts[0])
        return objectMapper.readTree(headerBytes)
    }
    
    private fun getApplePublicKey(keyId: String): PublicKey {
        if (System.currentTimeMillis() < keysCacheExpiry && publicKeysCache.containsKey(keyId)) {
            return publicKeysCache[keyId]!!
        }
        
        refreshApplePublicKeys()
        
        return publicKeysCache[keyId]
            ?: throw AppleTokenValidationException("Public key not found for key ID: $keyId")
    }
    
    private fun refreshApplePublicKeys() {
        logger.debug("Refreshing Apple public keys")
        
        try {
            val response = webClient.build()
                .get()
                .uri(appleKeysUrl)
                .retrieve()
                .bodyToMono(String::class.java)
                .timeout(Duration.ofSeconds(10))
                .block()
                
            if (response == null) {
                throw AppleTokenValidationException("Failed to fetch Apple public keys")
            }
            
            val keysJson = objectMapper.readTree(response)
            val keys = keysJson.get("keys")
            
            if (keys == null || !keys.isArray) {
                throw AppleTokenValidationException("Invalid keys response from Apple")
            }
            
            publicKeysCache.clear()
            
            keys.forEach { keyNode ->
                val keyId = keyNode.get("kid")?.asText()
                val kty = keyNode.get("kty")?.asText()
                val use = keyNode.get("use")?.asText()
                val alg = keyNode.get("alg")?.asText()
                val n = keyNode.get("n")?.asText()
                val e = keyNode.get("e")?.asText()
                
                if (keyId != null && kty == "RSA" && use == "sig" && alg == "RS256" && n != null && e != null) {
                    try {
                        val publicKey = createRSAPublicKey(n, e)
                        publicKeysCache[keyId] = publicKey
                        logger.debug("Cached Apple public key: $keyId")
                    } catch (ex: Exception) {
                        logger.warn("Failed to parse Apple public key: $keyId", ex)
                    }
                }
            }
            
            keysCacheExpiry = System.currentTimeMillis() + cacheValidityDuration
            
            logger.info("Successfully refreshed ${publicKeysCache.size} Apple public keys")
            
        } catch (ex: WebClientResponseException) {
            logger.error("Failed to fetch Apple public keys with status: ${ex.statusCode}")
            throw AppleTokenValidationException("Apple service error: ${ex.statusCode}")
        } catch (ex: Exception) {
            logger.error("Unexpected error fetching Apple public keys", ex)
            throw AppleTokenValidationException("Failed to fetch Apple public keys: ${ex.message}")
        }
    }
    
    private fun createRSAPublicKey(modulusBase64: String, exponentBase64: String): PublicKey {
        val modulus = BigInteger(1, Base64.getUrlDecoder().decode(modulusBase64))
        val exponent = BigInteger(1, Base64.getUrlDecoder().decode(exponentBase64))
        
        val keySpec = RSAPublicKeySpec(modulus, exponent)
        val keyFactory = KeyFactory.getInstance("RSA")
        
        return keyFactory.generatePublic(keySpec)
    }
}

/**
 * Exception thrown when Apple token validation fails
 */
class AppleTokenValidationException(message: String, cause: Throwable? = null) : 
    RuntimeException(message, cause)