package uk.ac.nott.mrl.openrecipe.model

class Recipe(val _id: String, var name: String? = null) {
	var parent: String? = null
	var author: String? = null
	val steps: MutableList<RecipeStep> = ArrayList()
}