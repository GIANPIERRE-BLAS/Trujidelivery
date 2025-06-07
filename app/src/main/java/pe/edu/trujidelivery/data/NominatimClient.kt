package pe.edu.trujidelivery.data

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimService {
    @GET("reverse?format=json")
    fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Call<Map<String, Any>>
}

object NominatimClient {
    val api: NominatimService by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimService::class.java)
    }
}
