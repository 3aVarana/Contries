package com.example.contris.data.remote

import com.example.contris.data.remote.dto.CountriesPageDto
import retrofit2.http.GET
import retrofit2.http.Query

const val OMITTED_FIELDS = "names.translations,flag.colors.palette,assets,leaders"
const val PAGE_SIZE = 100

interface RestCountriesApi {
    /** No trailing slash: `/countries/v5/?…` returns 404 on the live API. */
    @GET("countries/v5")
    suspend fun getCountries(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("response_fields_omit") omit: String = OMITTED_FIELDS,
    ): CountriesPageDto
}
