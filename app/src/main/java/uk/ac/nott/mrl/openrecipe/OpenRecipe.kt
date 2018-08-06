package uk.ac.nott.mrl.openrecipe

import android.app.Application
import android.content.Context
import com.jakewharton.disklrucache.DiskLruCache
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.File


class OpenRecipe : Application() {
	override fun onCreate() {
		super.onCreate()

		val cache = Cache(File(cacheDir, "http"), CACHE_SIZE)

		val httpClient = OkHttpClient.Builder()
				.cache(cache)
				.build()

		val sharedPrefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
		val urlRoot = HttpUrl.parse(sharedPrefs.getString("server", "http://192.168.0.1:8080/"))!!

		server = OpenRecipeServer(httpClient, urlRoot)

		val thumbCache = DiskLruCache.open(File(cacheDir, "thumbs"), 1, 1, CACHE_SIZE)

		val picassoBuilder = Picasso.Builder(this)
				.addRequestHandler(VideoRequestHandler(thumbCache))
				.downloader(OkHttp3Downloader(httpClient))
		val picasso = picassoBuilder.build()
		//picasso.setIndicatorsEnabled(true)
		Picasso.setSingletonInstance(picasso)
	}

	companion object {
		lateinit var server: OpenRecipeServer
		private const val CACHE_SIZE = 10 * 1024 * 1024L //10Mb
	}
}
