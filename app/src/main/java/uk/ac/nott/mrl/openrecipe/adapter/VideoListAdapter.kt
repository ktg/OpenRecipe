package uk.ac.nott.mrl.openrecipe.adapter

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.video_item.view.*
import uk.ac.nott.mrl.openrecipe.OpenRecipe
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.loader.VideoListLoader
import uk.ac.nott.mrl.openrecipe.model.Video

class VideoListAdapter(private val context: Context, private val recipe: String) : RecyclerView.Adapter<VideoListAdapter.ViewHolder>(), LoaderManager.LoaderCallbacks<List<Video>> {
	inner class ViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {

		var video: Video? = null

		fun setMedia(item: Video) {
			root.imageView.setImageResource(R.drawable.bg_video)
			Picasso.get()
					.load(item.getURL(OpenRecipe.server.urlRoot, item.start))
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
			}
		}
	}

	private var videoList: List<Video> = ArrayList()
		set(value) {
			if (field != value) {
				field = value
				notifyDataSetChanged()
			}
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

	override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Video>> {
		return VideoListLoader(context, recipe)
	}

	override fun onLoadFinished(loader: Loader<List<Video>>, data: List<Video>) {
		videoList = data
	}

	override fun onLoaderReset(loader: Loader<List<Video>>) {}
}
