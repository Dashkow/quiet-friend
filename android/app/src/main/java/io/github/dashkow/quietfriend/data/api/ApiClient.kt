package io.github.dashkow.quietfriend.data.api

import io.github.dashkow.quietfriend.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        // Версия приложения в каждом запросе: бэкенд отвечает 426, если она ниже минимальной
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("X-App-Version", BuildConfig.VERSION_NAME)
                    .build()
            )
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(httpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val users: UsersApi = retrofit.create(UsersApi::class.java)

    /** Разбирает тело ошибки нашего формата; null, если формат другой. */
    fun parseError(body: String?): ErrorBody? =
        body?.let { runCatching { json.decodeFromString(ErrorResponse.serializer(), it).error }.getOrNull() }
}
