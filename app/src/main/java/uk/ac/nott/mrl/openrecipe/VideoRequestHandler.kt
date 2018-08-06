package uk.ac.nott.mrl.openrecipe

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.jakewharton.disklrucache.DiskLruCache
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import okhttp3.HttpUrl
import java.io.IOException
import java.net.URISyntaxException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class VideoRequestHandler(private val cache: DiskLruCache) : RequestHandler() {

	override fun canHandleRequest(data: Request): Boolean {
		println(data.uri.toString())
		return data.uri.toString().startsWith(OpenRecipe.server.buildURL("video").toString())
	}

	@Throws(IOException::class)
	override fun load(request: Request, networkPolicy: Int): RequestHandler.Result? {
		println("Opening " + request.uri.toString())
		val hash = md5(request.uri.toString())
		val cacheEntry = cache.get(hash)
		if (cacheEntry != null) {
			try {
				val bitmap = BitmapFactory.decodeStream(cacheEntry.getInputStream(0))
				if (bitmap != null) {
					return RequestHandler.Result(bitmap, Picasso.LoadedFrom.DISK)
				}
			} catch (e: Exception) {
				println(e.message)
			}
		}
		val retriever = MediaMetadataRetriever()
		try {
			val time = (request.uri.getQueryParameter("t")?.toLongOrNull() ?: 0L) * 1000
			val zoom = request.uri.getQueryParameter("z")?.toFloatOrNull() ?: 1f
			val x = request.uri.getQueryParameter("x")?.toFloatOrNull() ?: 0f
			val y = request.uri.getQueryParameter("y")?.toFloatOrNull() ?: 0f

			retriever.setDataSource(removeQueryParameter(request.uri.toString()), emptyMap())
			var bitmap = retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
			if (bitmap == null) {
				bitmap = retriever.frameAtTime
			}
			if (bitmap != null) {
				try {
					val newCacheEntry = cache.edit(hash)
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, newCacheEntry.newOutputStream(0))
					newCacheEntry.commit()
				} catch (e: Exception) {
					println(e.message)
				}

				if (zoom != 1f) {
					val width = (bitmap.width / zoom).toInt()
					val height = (bitmap.height / zoom).toInt()
					val xOffset = ((bitmap.width - width) * x).toInt()
					val yOffset = ((bitmap.height - height) * y).toInt()
					bitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, width, height)
				}
				return RequestHandler.Result(bitmap, Picasso.LoadedFrom.NETWORK)
			}
		} catch (ex: Exception) {
			ex.printStackTrace()
		} finally {
			try {
				retriever.release()
			} catch (ex: RuntimeException) {
			}
		}
		return null
	}

	fun md5(s: String): String {
		try {
			// Create MD5 Hash
			val digest = MessageDigest.getInstance("MD5")
			digest.update(s.toByteArray())
			val messageDigest = digest.digest()

			// Create Hex String
			val hexString = StringBuffer()
			for (i in messageDigest.indices)
				hexString.append(Integer.toHexString(0xFF and messageDigest[i].toInt()))
			return hexString.toString()

		} catch (e: NoSuchAlgorithmException) {
			e.printStackTrace()
		}

		return ""
	}

	@Throws(URISyntaxException::class)
	private fun removeQueryParameter(url: String): String {
		val httpUrl = HttpUrl.parse(url)!!
		val uriBuilder = httpUrl.newBuilder()
		for (query in httpUrl.queryParameterNames()) {
			uriBuilder.setQueryParameter(query, null)
			uriBuilder.removeAllQueryParameters(query)
		}
		println("Cleaned URL = " + uriBuilder.build().toString())
		return uriBuilder.build().toString()
	}
}