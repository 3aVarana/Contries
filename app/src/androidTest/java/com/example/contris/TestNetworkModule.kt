package com.example.contris

import com.example.contris.di.ApiKey
import com.example.contris.di.BaseUrl
import com.example.contris.di.NetworkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import okhttp3.mockwebserver.MockWebServer

/**
 * Replaces only the base URL / API key bindings so the real OkHttp + Retrofit stack talks to a MockWebServer.
 * Tests never hit the live REST Countries API (and never spend quota).
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule.UrlModule::class])
object TestNetworkModule {

    val server: MockWebServer by lazy { startedServer.first }

    /** Hilt providers run on the main thread; starting a socket and resolving its host there trips StrictMode. */
    private val startedServer: Pair<MockWebServer, String> by lazy {
        var result: Pair<MockWebServer, String>? = null
        var error: Throwable? = null
        Thread {
            runCatching {
                val s = MockWebServer().also { it.start() }
                s to s.url("/").toString()
            }.onSuccess { result = it }.onFailure { error = it }
        }.apply { start(); join() }
        error?.let { throw it }
        result!!
    }

    @Provides
    @BaseUrl
    fun provideBaseUrl(): String = startedServer.second

    @Provides
    @ApiKey
    fun provideApiKey(): String = "test-key"
}
