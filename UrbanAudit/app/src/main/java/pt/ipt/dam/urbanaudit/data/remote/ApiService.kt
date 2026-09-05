package pt.ipt.dam.urbanaudit.data.remote

import pt.ipt.dam.urbanaudit.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Interface Retrofit que declara os endpoints da API REST do Urban Audit.
 * Todas as funções são 'suspend', permitindo a sua invocação assíncrona dentro de Coroutines.
 */
interface ApiService {

    /** Autenticação de utilizador existente com devolução de token JWT */
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /** Registo de um novo utilizador na plataforma */
    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>

    /** Consulta da listagem global de ocorrências municipais */
    @GET("api/ocorrencias")
    suspend fun getOcorrencias(): Response<List<Ocorrencia>>

    /** Publicação de uma nova ocorrência com título, descrição, coordenadas e foto Base64 */
    @POST("api/ocorrencias")
    suspend fun createOcorrencia(@Body ocorrencia: OcorrenciaCreate): Response<Ocorrencia>

    /** Atualização dos dados ou estado de uma ocorrência existente */
    @PUT("api/ocorrencias/{id}")
    suspend fun updateOcorrencia(@Path("id") id: Int, @Body ocorrencia: OcorrenciaUpdate): Response<Ocorrencia>

    /** Remoção de uma ocorrência (apenas autor da ocorrência ou administrador) */
    @DELETE("api/ocorrencias/{id}")
    suspend fun deleteOcorrencia(@Path("id") id: Int): Response<Unit>
}