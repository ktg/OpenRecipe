package uk.ac.nott.mrl.openrecipe.model

class Recipe(val id: String, var name: String? = null) {
	val steps: MutableList<RecipeStep> = ArrayList()
}