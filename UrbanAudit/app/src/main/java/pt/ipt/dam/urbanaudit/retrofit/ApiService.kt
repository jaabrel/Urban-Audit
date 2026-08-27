package pt.ipt.dam.urbanaudit.retrofit

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import pt.ipt.dam.urbanaudit.models.Ocorrencia
import pt.ipt.dam.urbanaudit.models.LoginRequest
import pt.ipt.dam.urbanaudit.models.LoginResponse

interface ApiService {

    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("ocorrencias")
    fun getOcorrencias(@Header("Authorization") token: String): Call<List<Ocorrencia>>

    @POST("ocorrencias")
    fun criarOcorrencia(
        @Header("Authorization") token: String,
        @Body ocorrencia: Ocorrencia
    ): Call<Ocorrencia>
}