package pe.edu.trujidelivery.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimService {

    @GET("reverse")
    fun getDireccion(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json"
    ): Call<NominatimResult>

    data class NominatimResult(
        val display_name: String
    )
}
