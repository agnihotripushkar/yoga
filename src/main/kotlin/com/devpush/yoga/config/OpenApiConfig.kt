package com.devpush.yoga.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Value("\${server.port:8080}")
    private val serverPort: String = "8080"

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Yoga App API")
                    .description("""
                        Comprehensive REST API for the Yoga App backend system.
                        
                        ## Features
                        - **OAuth Authentication**: Google and Apple Sign-In support
                        - **Profile Management**: User profile customization with picture upload
                        - **Progress Tracking**: Session recording and analytics
                        - **Yoga Classes**: Video content library with search and favorites
                        - **Security**: JWT-based authentication with rate limiting
                        
                        ## Authentication
                        Most endpoints require authentication using JWT tokens. Include the token in the Authorization header:
                        ```
                        Authorization: Bearer <your-jwt-token>
                        ```
                        
                        ## Rate Limiting
                        API endpoints are rate-limited to ensure fair usage:
                        - Authentication endpoints: 10 requests/minute
                        - File uploads: 5 requests/minute
                        - General API calls: 60 requests/minute
                        
                        ## Error Handling
                        The API uses standard HTTP status codes and returns structured error responses:
                        ```json
                        {
                          "error": "ERROR_CODE",
                          "message": "Human-readable error message",
                          "timestamp": "2024-01-01T12:00:00Z"
                        }
                        ```
                    """.trimIndent())
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Yoga App Development Team")
                            .email("dev@yogaapp.com")
                            .url("https://yogaapp.com")
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:$serverPort")
                        .description("Development server"),
                    Server()
                        .url("https://api.yogaapp.com")
                        .description("Production server")
                )
            )
            .addSecurityItem(
                SecurityRequirement().addList("bearerAuth")
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT token obtained from authentication endpoints")
                    )
            )
    }
}