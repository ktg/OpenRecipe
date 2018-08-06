package uk.ac.nott.mrl.openrecipe.adapter

import android.content.Context
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.recipe_step.view.*
import uk.ac.nott.mrl.openrecipe.OpenRecipe
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.activity.VideoActivity
import uk.ac.nott.mrl.openrecipe.loader.RecipeLoader
import uk.ac.nott.mrl.openrecipe.model.Recipe
import uk.ac.nott.mrl.openrecipe.model.RecipeStep

class RecipeAdapter(private val context: Context, private val id: String) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>(), LoaderManager.LoaderCallbacks<Recipe> {
	inner class ViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {
		private var recipeStep: RecipeStep? = null
		private var matrix = Matrix()

		fun setMedia(item: RecipeStep) {
			this.recipeStep = item
			root.stepTitle.text = item.name
			root.stepText.text = item.text
			//root.playerView.player = exoPlayer
			if (item.video == null) {
				root.videoThumb.visibility = View.GONE
				root.playerView.visibility = View.GONE
			} else {
				root.playerView.visibility = View.GONE
				item.video?.let {
					Picasso.get()
							.load(it.getURL(OpenRecipe.server.urlRoot, item.start))
							.error(R.drawable.ic_local_movies_black_64dp)
							.placeholder(R.drawable.ic_local_movies_black_64dp)
							.into(root.videoThumb)

					root.videoThumb.isClickable = true
					root.videoThumb.setOnClickListener {
						item.video?.let {
							currentPlaying?.stopPlaying()
							root.playerView.visibility = View.VISIBLE
							root.playerView.player = exoPlayer
							val uri = Uri.parse(it.getURL(OpenRecipe.server.urlRoot))
							val source = extractorMediaSource.createMediaSource(uri)
							val end = if (item.end == 0L) {
								C.TIME_END_OF_SOURCE
							} else {
								item.end * 1000
							}
							val mediaSource = ClippingMediaSource(source, item.start * 1000, end)
							exoPlayer.prepare(mediaSource, true, true)
							exoPlayer.playWhenReady = true
							currentPlaying = this
						}
					}
				}
			}
		}

		fun updateMatrix() {
			(root.playerView.videoSurfaceView as TextureView).setTransform(matrix)
		}

		fun calcMatrix(width: Int, height: Int) {
			recipeStep?.video?.zoom?.let {
				val centerXRatio = recipeStep?.video?.x ?: 0.5f
				val centerYRatio = recipeStep?.video?.y ?: 0.5f

				val surface = root.playerView.videoSurfaceView as TextureView
				val viewAspectRatio = surface.width / surface.height.toFloat()
				val aspectRatio = width / height.toFloat()
				println("Pixel Ratio: $width / $height = $aspectRatio, ${surface.width} / ${surface.height} = $viewAspectRatio")

				if (viewAspectRatio > aspectRatio) {
					matrix.setScale(it, it, surface.height * aspectRatio * centerXRatio, surface.height * centerYRatio)
				} else {
					matrix.setScale(it, it, surface.width * centerXRatio, surface.width / aspectRatio * centerYRatio)
				}

				println("Set scale: $matrix")
			}
			(root.playerView.videoSurfaceView as TextureView).setTransform(matrix)
		}

		private fun stopPlaying() {
			exoPlayer.playWhenReady = false
			root.playerView.player = null
			root.playerView.visibility = View.GONE
			root.videoThumb.visibility = View.VISIBLE
		}
	}

	private val trackSelector = DefaultTrackSelector(DefaultBandwidthMeter())
	private val renderersFactory = DefaultRenderersFactory(context)
	private val dataSourceFactory = DefaultDataSourceFactory(context, VideoActivity.USER_AGENT)
	private val exoPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector)
	private val extractorMediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
	private var currentPlaying: ViewHolder? = null
	private var recipe: Recipe? = null

	init {
		exoPlayer.addListener(object : Player.DefaultEventListener() {
			override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
				println("onPlayerStateChanged")
				currentPlaying?.updateMatrix()
			}
		})
		exoPlayer?.addVideoListener(object : VideoListener {
			override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
				println("Video size changed")
				currentPlaying?.calcMatrix(width, height)
			}

			override fun onRenderedFirstFrame() {
				currentPlaying?.updateMatrix()
			}

		})
	}

	override fun getItemCount(): Int {
		return recipe?.steps?.size ?: 0
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recipe_step, parent, false))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.setMedia(recipe!!.steps[position])
	}

	override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<Recipe> {
		return RecipeLoader(context, id)
	}

	override fun onLoadFinished(loader: Loader<Recipe>, data: Recipe?) {
		recipe = data
		notifyDataSetChanged()
	}

	override fun onLoaderReset(loader: Loader<Recipe>) {
	}
}
