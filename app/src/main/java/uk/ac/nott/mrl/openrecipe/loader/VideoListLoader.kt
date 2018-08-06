package uk.ac.nott.mrl.openrecipe.loader

import android.content.Context
import android.util.Log
import androidx.loader.content.AsyncTaskLoader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Request
import uk.ac.nott.mrl.openrecipe.OpenRecipe
import uk.ac.nott.mrl.openrecipe.model.Video


class VideoListLoader(context: Context, private val recipe: String?) : AsyncTaskLoader<List<Video>>(context) {
	private val gson = Gson()
	override fun loadInBackground(): List<Video> {
		try {
			val urlBuilder = OpenRecipe.server.buildURL()
					.addPathSegments("api/videos/recipe")
			recipe?.let {
				urlBuilder.addPathSegment(it)
			}

			val request = Request.Builder()
					.url(urlBuilder.build())
					.build()

			val response = OpenRecipe.server.call(request).execute()
			if (response.isSuccessful) {
				return gson.fromJson(response.body()?.string(), object : TypeToken<List<Video>>() {}.type)
			} else {
				Log.w("VideoListLoader", response.message())
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return emptyList()
	}

	override fun onStartLoading() {
		super.onStartLoading()
		forceLoad()
	}
}