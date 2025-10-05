package com.devpush.yoga.util

import jakarta.servlet.http.HttpServletRequest

/**
 * Utility class for extracting client IP addresses from HTTP requests
 */
object ClientIpExtractor {
    
    private val IP_HEADERS = listOf(
        "X-Forwarded-For",
        "X-Real-IP",
        "X-Originating-IP",
        "CF-Connecting-IP",
        "True-Client-IP"
    )
    
    /**
     * Extract the real client IP address from the HTTP request
     * Handles various proxy headers and load balancer configurations
     */
    fun getClientIp(request: HttpServletRequest): String {
        // Check proxy headers first
        for (header in IP_HEADERS) {
            val ip = request.getHeader(header)
            if (!ip.isNullOrBlank() && !ip.equals("unknown", ignoreCase = true)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                return ip.split(",")[0].trim()
            }
        }
        
        // Fall back to remote address
        return request.remoteAddr ?: "unknown"
    }
}