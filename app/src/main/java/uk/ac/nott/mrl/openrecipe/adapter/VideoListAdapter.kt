package uk.ac.nott.mrl.openrecipe.adapter

import android.app.LoaderManager
import android.content.ClipData
import android.content.Context
import android.content.Loader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.video_item.view.*
import uk.ac.nott.mrl.openrecipe.GlideApp
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.loader.VideoListLoader

class VideoListAdapter(private val context: Context) : RecyclerView.Adapter<VideoListAdapter.ViewHolder>(), LoaderManager.LoaderCallbacks<List<Uri>> {
	inner class ViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {

		fun setMedia(item: Uri) {
			root.mediaName.text = item.lastPathSegment
			GlideApp.with(context)
					.load(item)
					.fitCenter()
					.placeholder(R.drawable.ic_backdrop)
					.fallback(R.drawable.ic_backdrop)
					.into(root.imageView)
			root.isClickable = true
			root.setOnLongClickListener {
				val data = ClipData.newPlainText("", "")
				val shadowBuilder = View.DragShadowBuilder(it.imageView)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					it.startDragAndDrop(data, shadowBuilder, item, 0)
				} else {
					@Suppress("DEPRECATION")
					it.startDrag(data, shadowBuilder, item, 0)
				}
				true
			}
		}
	}

	var videoList: List<Uri> = ArrayList()
		set(value) {
			field = value
			notifyDataSetChanged()
		}

	override fun getItemCount(): Int {
		return videoList.size
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.video_item, parent, false))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.setMedia(videoList[position])
	}

	override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Uri>> {
		Log.i("VideoListAdapter", "Create VideoListLoader")
		return VideoListLoader(context)
	}

	override fun onLoadFinished(loader: Loader<List<Uri>>, data: List<Uri>) {
		videoList = data
		notifyDataSetChanged()
	}

	override fun onLoaderReset(loader: Loader<List<Uri>>) {
	}
}
