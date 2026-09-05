package pt.ipt.dam.urbanaudit.data.remote

import pt.ipt.dam.urbanaudit.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>

    @GET("api/ocorrencias")
    suspend fun getOcorrencias(): Response<List<Ocorrencia>>

    @POST("api/ocorrencias")
    suspend fun createOcorrencia(@Body ocorrencia: OcorrenciaCreate): Response<Ocorrencia>

    @PUT("api/ocorrencias/{id}")
    suspend fun updateOcorrencia(@Path("id") id: Int, @Body ocorrencia: OcorrenciaUpdate): Response<Ocorrencia>

    @DELETE("api/ocorrencias/{id}")
    suspend fun deleteOcorrencia(@Path("id") id: Int): Response<Unit>
}