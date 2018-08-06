package uk.ac.nott.mrl.openrecipe.model

import uk.ac.nott.mrl.openrecipe.OpenRecipe

class RecipeStep(var name: String = "") {
	var text = ""
	var video: Video? = null
	var start: Long = 0
	var end: Long = 0

	fun getURL(): String? {
		return video?.getURL(OpenRecipe.server.urlRoot, start)
	}
}