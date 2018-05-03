package uk.ac.nott.mrl.openrecipe.loader

import android.content.AsyncTaskLoader
import android.content.Context
import com.google.gson.Gson
import uk.ac.nott.mrl.openrecipe.adapter.RecipeEditAdapter
import uk.ac.nott.mrl.openrecipe.model.Recipe
import uk.ac.nott.mrl.openrecipe.model.RecipeStep

class RecipeLoader(context: Context, private val id: String = "test") : AsyncTaskLoader<Recipe>(context) {
	private val gson = Gson()

	override fun loadInBackground(): Recipe {
		val sharedPref = context.getSharedPreferences(RecipeEditAdapter.RECIPE_PREFERENCE, Context.MODE_PRIVATE)
		val json = sharedPref.getString(id, null)
		return if(json == null) {
			val recipe = Recipe(id, "Test")
			recipe.steps.add(RecipeStep("Preparation"))
			recipe.steps.add(RecipeStep("Cooking"))
			recipe
		} else {
			gson.fromJson(json, Recipe::class.java)
		}
	}

	override fun onStartLoading() {
		super.onStartLoading()
		forceLoad()
	}
}