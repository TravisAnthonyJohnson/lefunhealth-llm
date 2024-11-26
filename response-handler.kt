package com.lefunhealth.llm

import android.util.LruCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ResponseHandler {
    companion object {
        private const val CACHE_SIZE = 100
    }

    private val cache = LruCache<String, String>(CACHE_SIZE)
    private val mutex = Mutex()

    suspend fun formatResponse(response: String): String = withLock {
        // Basic cleaning and formatting
        response.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\n\\s*\\n"), "\n")
    }

    suspend fun cacheResponse(query: String, response: String) = mutex.withLock {
        cache.put(normalizeQuery(query), response)
    }

    suspend fun getCachedResponse(query: String): String? = mutex.withLock {
        cache.get(normalizeQuery(query))
    }

    private fun normalizeQuery(query: String): String {
        return query.trim().lowercase()
    }

    suspend fun clearCache() = mutex.withLock {
        cache.evictAll()
    }
}