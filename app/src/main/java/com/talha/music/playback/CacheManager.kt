package com.talha.music.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object CacheManager {
    private var instance: SimpleCache? = null

    fun getCache(context: Context): SimpleCache {
        if (instance == null) {
            val cacheDir = File(context.cacheDir, "music_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024) // 500MB
            val databaseProvider = StandaloneDatabaseProvider(context)
            instance = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return instance!!
    }

    fun clear(context: Context) {
        instance?.release()
        instance = null
        File(context.cacheDir, "music_cache").deleteRecursively()
    }
}
