package com.example.contris.data

import com.example.contris.data.remote.AuthInterceptor
import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
    }

    @After
    fun tearDown() = server.shutdown()

    private fun headerSeen(interceptor: AuthInterceptor): String? {
        server.enqueue(MockResponse().setBody("{}"))
        OkHttpClient.Builder().addInterceptor(interceptor).build()
            .newCall(Request.Builder().url(server.url("/countries/v5")).build()).execute().close()
        return server.takeRequest().getHeader("Authorization")
    }

    @Test
    fun `adds bearer header for the API host`() {
        assertThat(headerSeen(AuthInterceptor(server.url("/")) { "secret" })).isEqualTo("Bearer secret")
    }

    @Test
    fun `never sends the key to other hosts such as the flag CDN`() {
        assertThat(headerSeen(AuthInterceptor("api.restcountries.com") { "secret" })).isNull()
    }

    @Test
    fun `blank key sends no header`() {
        assertThat(headerSeen(AuthInterceptor(server.url("/")) { "" })).isNull()
    }
}
