package pt.ipt.dam.urbanaudit.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import pt.ipt.dam.urbanaudit.data.local.TokenManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "http://192.168.1.157:8000/"

    fun getClient(tokenManager: TokenManager): Retrofit {
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