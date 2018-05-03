package uk.ac.nott.mrl.openrecipe.loader

import android.content.AsyncTaskLoader
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File

class VideoListLoader(context: Context) : AsyncTaskLoader<List<Uri>>(context) {
	override fun loadInBackground(): List<Uri> {
		return handleDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), ArrayList())
	}

	private fun handleFile(file: File, sourceList: MutableList<Uri>) {
		if (file.extension in VIDEO_EXTENSIONS) {
			Log.i("VideoListLoader", "Adding " + file.absolutePath)
			val uri = Uri.fromFile(file)
			sourceList.add(uri)
		}
	}

	private fun handleDirectory(directory: File, videos: MutableList<Uri>): List<Uri> {
		Log.i("VideoListLoader", "Examining " + directory.absolutePath + ": " + directory.listFiles().size)
		val children = directory.listFiles()
		if (children != null) {
			for (child in children) {
				if (child.isDirectory) {
					handleDirectory(child, videos)
				} else {
					handleFile(child, videos)
				}
			}
		}
		return videos
	}

	override fun onStartLoading() {
		super.onStartLoading()
		forceLoad()
	}

	companion object {
		private val VIDEO_EXTENSIONS = listOf("mp4", "mkv", "ogv", "3gp", "webm") /// avi?
	}
}