package uk.ac.nott.mrl.openrecipe.loader

import android.content.Context
import android.util.Log
import androidx.loader.content.AsyncTaskLoader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Request
import uk.ac.nott.mrl.openrecipe.OpenRecipe
import uk.ac.nott.mrl.openrecipe.model.Recipe


class RecipeChildLoader(context: Context, private val id: String?) : AsyncTaskLoader<List<Recipe>>(context) {
	private val gson = Gson()

	override fun loadInBackground(): List<Recipe> {
		try {
			val parentId = id ?: "root"
			val request = Request.Builder()
					.url(OpenRecipe.server.buildURL()
							.addPathSegments("api/recipes/")
							.addPathSegment(parentId)
							.addPathSegment("children")
							.build())
					.build()

			val response = OpenRecipe.server.call(request).execute()
			if (response.isSuccessful) {
				return gson.fromJson(response.body()?.string(), object : TypeToken<List<Recipe>>() {}.type)
			} else {
				Log.w("RecipeChildLoader", response.message())
			}
		} catch (e: Exception) {
			println(e)
		}
		return emptyList()
	}

	override fun onStartLoading() {
		super.onStartLoading()
		forceLoad()
	}
}