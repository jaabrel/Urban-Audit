package pt.ipt.dam.urbanaudit.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import pt.ipt.dam.urbanaudit.data.local.TokenManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Cliente HTTP Singleton baseado na biblioteca Retrofit.
 * 
 * Funcionalidades:
 * - Configuração do endpoint base da API REST alojada no Railway.
 * - Injeção automática do cabeçalho HTTP "Authorization: Bearer <token>" através de um interceptor OkHttp.
 * - Deserialização automática de dados JSON utilizando GsonConverterFactory.
 */
object ApiClient {

    // URL base da API REST (FastAPI alojada em ambiente cloud no Railway)
    private const val BASE_URL = "https://urban-audit-api-production.up.railway.app/"

    /**
     * Retorna a instância configurada do Retrofit com o interceptor de autenticação Bearer.
     */
    fun getClient(tokenManager: TokenManager): Retrofit {
        // Interceptor OkHttp que adiciona o token JWT em todos os pedidos protegidos
        val interceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()

            tokenManager.getToken()?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
            chain.proceed(requestBuilder.build())
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}