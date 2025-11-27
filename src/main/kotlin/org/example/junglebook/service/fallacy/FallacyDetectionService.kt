package org.example.junglebook.service.fallacy

import org.example.junglebook.web.dto.FallacyDetectionRequest
import org.example.junglebook.web.dto.FallacyDetectionResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException
import java.util.concurrent.CompletableFuture

@Service
class FallacyDetectionService(
    private val restTemplate: RestTemplate
) {
    companion object {
        private val logger = LoggerFactory.getLogger(FallacyDetectionService::class.java)
    }

    @Value("\${fallacy.detection.service.url}")
    private val serviceUrl: String = "http://localhost:8000/api/v1"

    @Value("\${fallacy.detection.service.timeout:5000}")
    private val timeout: Int = 5000

    fun detectFallacy(
        text: String, 
        language: String = "ko",
        topicTitle: String? = null,
        topicDescription: String? = null
    ): FallacyDetectionResponse? {
        return try {
            val request = mutableMapOf<String, Any>(
                "text" to text,
                "language" to language
            )
            
            topicTitle?.let { request["topic_title"] = it }
            topicDescription?.let { request["topic_description"] = it }
            
            val url = "$serviceUrl/detect"
            
            logger.debug("Calling fallacy detection service: $url with topic context")
            
            val responseMap = restTemplate.postForObject(
                url,
                request,
                Map::class.java
            ) as? Map<*, *>
            
            if (responseMap != null) {
                val response = FallacyDetectionResponse(
                    hasFallacy = (responseMap["has_fallacy"] as? Boolean) ?: false,
                    fallacyType = responseMap["fallacy_type"] as? String,
                    confidence = (responseMap["confidence"] as? Number)?.toDouble() ?: 0.0,
                    explanation = (responseMap["explanation"] as? String) ?: ""
                )
                logger.debug("Fallacy detection result: hasFallacy=${response.hasFallacy}, type=${response.fallacyType}")
                response
            } else {
                null
            }
            
        } catch (e: HttpClientErrorException) {
            logger.error("HTTP error calling fallacy detection service: ${e.statusCode} - ${e.message}", e)
            null
        } catch (e: Exception) {
            logger.error("Error calling fallacy detection service: ${e.message}", e)
            null
        }
    }

    fun detectFallacyAsync(
        text: String, 
        language: String = "ko",
        topicTitle: String? = null,
        topicDescription: String? = null
    ): CompletableFuture<FallacyDetectionResponse?> {
        return CompletableFuture.supplyAsync {
            detectFallacy(text, language, topicTitle, topicDescription)
        }
    }

    fun batchDetectFallacy(texts: List<String>, language: String = "ko"): List<FallacyDetectionResponse> {
        return try {
            val url = "$serviceUrl/detect/batch"
            val request = mapOf(
                "texts" to texts,
                "language" to language
            )
            
            logger.debug("Calling batch fallacy detection service: $url")
            
            val response = restTemplate.postForObject(
                url,
                request,
                Map::class.java
            )
            
            val results = response?.get("results") as? List<Map<*, *>>
            results?.map { result ->
                FallacyDetectionResponse(
                    hasFallacy = (result["has_fallacy"] as? Boolean) ?: false,
                    fallacyType = result["fallacy_type"] as? String,
                    confidence = (result["confidence"] as? Number)?.toDouble() ?: 0.0,
                    explanation = (result["explanation"] as? String) ?: ""
                )
            } ?: emptyList()
            
        } catch (e: Exception) {
            logger.error("Error calling batch fallacy detection service: ${e.message}", e)
            emptyList()
        }
    }

    fun healthCheck(): Boolean {
        return try {
            val url = "$serviceUrl/health"
            val response = restTemplate.getForObject(url, Map::class.java)
            val status = response?.get("status") as? String
            status == "healthy"
        } catch (e: Exception) {
            logger.error("Health check failed: ${e.message}", e)
            false
        }
    }
}

