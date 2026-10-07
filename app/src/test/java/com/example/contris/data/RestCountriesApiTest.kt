package com.example.contris.data

import com.example.contris.data.remote.AuthInterceptor
import com.example.contris.data.remote.OMITTED_FIELDS
import com.example.contris.data.remote.RestCountriesApi
import com.example.contris.testutil.Fixtures
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RestCountriesApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: RestCountriesApi

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(server.url("/")) { "test-key" }).build()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(Fixtures.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RestCountriesApi::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `sends bearer header and omit query, parses envelope`() = runTest {
        server.enqueue(MockResponse().setBody(Fixtures.raw("countries_page_0.json")))

        val page = api.getCountries(offset = 0)

        val request = server.takeRequest()
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-key")
        assertThat(request.requestUrl!!.encodedPath).isEqualTo("/countries/v5")
        assertThat(request.requestUrl!!.queryParameter("limit")).isEqualTo("100")
        assertThat(request.requestUrl!!.queryParameter("offset")).isEqualTo("0")
        assertThat(request.requestUrl!!.queryParameter("response_fields_omit")).isEqualTo(OMITTED_FIELDS)
        assertThat(request.requestUrl!!.queryParameter("key")).isNull()

        assertThat(page.data!!.meta!!.total).isEqualTo(7)
        assertThat(page.data!!.objects.first().names!!.common).isEqualTo("Canada")
    }

    @Test
    fun `unknown fields are ignored`() = runTest {
        server.enqueue(MockResponse().setBody("""{"data":{"objects":[{"uuid":"x","names":{"common":"X","brand_new":1}}],"meta":{"more":false,"new_meta":true}},"version":9}"""))
        val page = api.getCountries(offset = 0)
        assertThat(page.data!!.objects.single().uuid).isEqualTo("x")
        assertThat(page.data!!.meta!!.more).isFalse()
    }

    @Test
    fun `http errors surface as HttpException with code`() = runTest {
        for (code in listOf(401, 403, 429, 500)) {
            server.enqueue(MockResponse().setResponseCode(code).setBody(Fixtures.raw(if (code == 429) "error_429.json" else "error_401.json")))
            val error = runCatching { api.getCountries(offset = 0) }.exceptionOrNull()
            assertThat(error).isInstanceOf(HttpException::class.java)
            assertThat((error as HttpException).code()).isEqualTo(code)
        }
    }
}
