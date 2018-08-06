package uk.ac.nott.mrl.openrecipe.loader

import android.content.Context
import android.util.Log
import androidx.loader.content.AsyncTaskLoader
import com.google.gson.Gson
import okhttp3.Request
import uk.ac.nott.mrl.openrecipe.OpenRecipe
import uk.ac.nott.mrl.openrecipe.model.Recipe

class RecipeLoader(context: Context, private val id: String) : AsyncTaskLoader<Recipe>(context) {
	private val gson = Gson()

	override fun loadInBackground(): Recipe {
		val request = Request.Builder()
				.url(OpenRecipe.server.buildURL()
						.addPathSegments("api/recipes/")
						.addPathSegment(id)
						.build())
				.build()

		val response = OpenRecipe.server.call(request).execute()
		return if (response.isSuccessful) {
			gson.fromJson(response.body()?.string(), Recipe::class.java)
		} else {
			Log.w("RecipeLoader", response.message())
			//emptyList()
			Recipe("Fake")
		}
	}

	override fun onStartLoading() {
		super.onStartLoading()
		forceLoad()
	}
}