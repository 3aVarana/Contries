package com.example.contris.data.remote

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds `Authorization: Bearer <key>` to requests for the API host only. The same OkHttp client also
 * downloads flag images from a CDN, which must never receive the key. The key is never a query parameter.
 */
class AuthInterceptor(
    private val apiHost: String,
    private val apiKeyProvider: () -> String,
) : Interceptor {

    constructor(baseUrl: HttpUrl, apiKeyProvider: () -> String) : this(baseUrl.host, apiKeyProvider)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val key = apiKeyProvider()
        if (key.isBlank() || !request.url.host.equals(apiHost, ignoreCase = true)) {
            return chain.proceed(request)
        }
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer $key").build())
    }
}
