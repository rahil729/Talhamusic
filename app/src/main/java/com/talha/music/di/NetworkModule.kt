package com.talha.music.di

import com.talha.music.BuildConfig
import com.talha.music.data.remote.LyricsApi
import com.talha.music.data.remote.AudiusApi
import com.talha.music.data.remote.YtDlpApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideLyricsApi(okHttpClient: OkHttpClient): LyricsApi {
        return Retrofit.Builder()
            .baseUrl(LyricsApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LyricsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAudiusApi(okHttpClient: OkHttpClient): AudiusApi {
        return Retrofit.Builder()
            .baseUrl(AudiusApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AudiusApi::class.java)
    }

    @Provides
    @Singleton
    fun provideYtDlpApi(okHttpClient: OkHttpClient): YtDlpApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.STREAM_BACKEND_URL)
            .client(okHttpClient.newBuilder().readTimeout(120, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YtDlpApi::class.java)
    }

}
