package uk.ac.nott.mrl.openrecipe.model

class RecipeStep(var name: String = "") {
	var text = ""
	var uri: String? = null
	var start: Long = 0
	var end: Long = 0
}