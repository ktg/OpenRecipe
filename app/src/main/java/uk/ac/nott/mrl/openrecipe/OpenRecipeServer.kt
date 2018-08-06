package uk.ac.nott.mrl.openrecipe

import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class OpenRecipeServer(private val httpClient: OkHttpClient, var urlRoot: HttpUrl) {
	val cookingStation = "5b309770e4ab06000578b8b4"

	fun buildURL(): HttpUrl.Builder {
		return urlRoot.newBuilder()
	}

	fun buildURL(path: String): HttpUrl {
		return urlRoot.newBuilder().addPathSegments(path).build()
	}

	fun call(request: Request): Call {
		return httpClient.newCall(request)
	}
}
