package uk.ac.nott.mrl.openrecipe.model

import okhttp3.HttpUrl

class Video(val _id: String) {
	val name: String = ""
	val file: String = ""
	val start: Long = 0
	val end: Long = 0
	val recipe: String? = null
	val zoom: Float? = null
	val x: Float? = null
	val y: Float? = null

	fun getURL(urlRoot: HttpUrl, offset: Long? = null): String {
		val url = urlRoot.newBuilder()
				.addPathSegment("videos")
				.addPathSegment(file)
		offset?.let {
			url.addQueryParameter("t", it.toString())
		}
		zoom?.let {
			url.addQueryParameter("z", it.toString())
		}
		x?.let {
			url.addQueryParameter("x", it.toString())
		}
		y?.let {
			url.addQueryParameter("y", it.toString())
		}
		return url.build().toString()
	}

	override fun equals(other: Any?): Boolean {
		if(other is Video) {
			return other._id == _id
		}
		return false
	}

	override fun hashCode(): Int {
		return _id.hashCode()
	}
}