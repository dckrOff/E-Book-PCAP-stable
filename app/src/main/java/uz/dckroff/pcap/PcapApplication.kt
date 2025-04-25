package uz.dckroff.pcap

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.executor.GlideExecutor
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.io.InputStream
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class PcapApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Инициализация Firebase
        FirebaseApp.initializeApp(this)

        // Инициализация Timber для логирования только в debug режиме
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        initializeGlide()
    }
    
    private fun initializeGlide() {
        // Настройка Glide для глобального кэширования и оптимизации
        val memoryCacheSizeBytes = 1024 * 1024 * 20 // 20 МБ
        val diskCacheSizeBytes = 1024 * 1024 * 100 // 100 МБ

        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .format(DecodeFormat.PREFER_RGB_565) // Более эффективный формат для изображений
            .skipMemoryCache(false)

        Glide.get(this).registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory()
        )

        // Настройка размеров кэша
        Glide.get(this).setMemoryCategory(com.bumptech.glide.MemoryCategory.NORMAL)

//        GlideBuilder()
//            .setDefaultRequestOptions(options)
//            .setMemoryCache(LruResourceCache(memoryCacheSizeBytes.toLong()))
//            .setDiskCache(InternalCacheDiskCacheFactory(this, diskCacheSizeBytes.toLong()))
//            .setIsActiveResourceRetentionAllowed(true)
//            .setSourceExecutor(GlideExecutor.newSourceExecutor())
//            .setDiskCacheExecutor(GlideExecutor.newDiskCacheExecutor())
//            .build()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Включаем поддержку MultiDex, если необходимо
        MultiDex.install(this)
    }
}

/**
 * Кастомный модуль для Glide для более тонкой настройки
 */
@GlideModule
class PcapGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Настраиваем OkHttp клиент с таймаутами для загрузки изображений
        val client = okhttp3.OkHttpClient.Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(20, TimeUnit.SECONDS)
            .build()
            
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(client)
        )
    }
    
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Применяем более эффективные настройки по умолчанию
        builder.setDefaultRequestOptions(
            RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .format(DecodeFormat.PREFER_RGB_565)
                .dontAnimate()
        )
    }
} 