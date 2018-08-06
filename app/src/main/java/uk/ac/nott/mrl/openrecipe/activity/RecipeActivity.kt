package uk.ac.nott.mrl.openrecipe.activity

import android.content.Intent
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.TextureView
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.android.synthetic.main.recipe.*
import uk.ac.nott.mrl.openrecipe.OpenRecipe
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.adapter.RecipeListAdapter
import uk.ac.nott.mrl.openrecipe.adapter.RecipeSectionListAdapter
import uk.ac.nott.mrl.openrecipe.loader.RecipeLoader
import uk.ac.nott.mrl.openrecipe.model.Recipe
import uk.ac.nott.mrl.openrecipe.model.RecipeStep

class RecipeActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Recipe> {

	private val trackSelector = DefaultTrackSelector(DefaultBandwidthMeter())
	private var exoPlayer: ExoPlayer? = null
	private var recipe: Recipe? = null
	private var sectionAdapter: RecipeSectionListAdapter? = null
	private var childAdapter: RecipeListAdapter? = null
	private var fullscreen = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.recipe)

		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		val fullscreenButton = findViewById<ImageButton>(R.id.exo_fullscreen)
		fullscreenButton?.setOnClickListener {
			fullscreen = !fullscreen
			if (fullscreen) {
				childListView.visibility = View.GONE
				sectionView.visibility = View.GONE
				toolbar.visibility = View.GONE
				fullscreenButton.setImageResource(R.drawable.ic_fullscreen_exit_24dp)
			} else {
				childListView.visibility = View.VISIBLE
				sectionView.visibility = View.VISIBLE
				toolbar.visibility = View.VISIBLE
				fullscreenButton.setImageResource(R.drawable.ic_fullscreen_24dp)
			}
		}

		load()
	}

	private fun load() {
		val loaderManager = LoaderManager.getInstance(this)
		sectionAdapter = RecipeSectionListAdapter()
		sectionView.layoutManager = LinearLayoutManager(this)
		sectionView.adapter = sectionAdapter
		loaderManager.initLoader(0, intent.extras, this)

		sectionAdapter?.itemSelected = {
			exoPlayer?.seekTo(it, 0L)
			recipe?.steps?.get(it)?.let {
				stepText.text = it.text
				//stepTitle.text = it.name
			}
		}

		val recipeID = intent.getStringExtra("recipe")
		childAdapter = RecipeListAdapter(this, recipeID)
		childListView.layoutManager = LinearLayoutManager(this)
		childListView.adapter = childAdapter
		loaderManager.initLoader(1, intent.extras, childAdapter!!)

	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.recipe_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			android.R.id.home -> {
				println(parent)
				val parent = recipe?.parent
				val intent = if (parent == null) {
					Intent(this, RecipeRootActivity::class.java)
				} else {
					Intent(this, RecipeActivity::class.java).apply {
						putExtra("recipe", parent)
					}
				}
				NavUtils.navigateUpTo(this, intent)
				return true
			}
			R.id.edit -> {
				startActivity(Intent(this, RecipeEditActivity::class.java).apply {
					putExtra("recipe", recipe?._id)
				})
			}
		}

		return super.onOptionsItemSelected(item)
	}

	override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<Recipe> {
		return RecipeLoader(this, args!!.getString("recipe"))
	}

	override fun onLoadFinished(loader: Loader<Recipe>, data: Recipe?) {
		data?.let {
			supportActionBar?.title = it.name
		}
		recipe = data
		sectionAdapter?.recipe = data
		//recipeAdapter?.onLoadFinished(loader, data)

		val renderersFactory = DefaultRenderersFactory(this)
		val dataSourceFactory = DefaultDataSourceFactory(this, VideoActivity.USER_AGENT)
		exoPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector)
		val extractorMediaSource = ExtractorMediaSource.Factory(dataSourceFactory)

		val concatSource = ConcatenatingMediaSource()
		if (data != null) {
			for (step in data.steps) {
				val video = step.video
				if (video == null) {
					val uri = Uri.parse("asset:///5-seconds-of-silence.mp3")
					val source = extractorMediaSource.createMediaSource(uri)
					concatSource.addMediaSource(source)
				} else {
					val uri = Uri.parse(video.getURL(OpenRecipe.server.urlRoot))
					val source = extractorMediaSource.createMediaSource(uri)
					val end = if (step.end == 0L) {
						C.TIME_END_OF_SOURCE
					} else {
						step.end * 1000
					}
					val mediaSource = ClippingMediaSource(source, step.start * 1000, end)
					concatSource.addMediaSource(mediaSource)
				}
			}
		}
		exoPlayer?.addListener(object : Player.EventListener {
			override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}
			override fun onSeekProcessed() {}
			override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}
			override fun onPlayerError(error: ExoPlaybackException?) {}
			override fun onLoadingChanged(isLoading: Boolean) {}
			override fun onPositionDiscontinuity(reason: Int) {
				println(exoPlayer?.currentWindowIndex)
				val sectionIndex = (exoPlayer?.currentWindowIndex ?: 0)
				sectionAdapter?.selectedIndex = sectionIndex
				recipe?.steps?.get(sectionIndex)?.let {
					stepText.text = it.text
					//stepTitle.text = it.name
				}

			}

			override fun onRepeatModeChanged(repeatMode: Int) {}
			override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
			override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {}
			override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {}
		})
		exoPlayer?.prepare(concatSource)
		sectionAdapter?.selectedIndex = 0
		recipe?.steps?.get(0)?.let {
			stepText.text = it.text
			//stepTitle.text = it.name
		}
		playerView.player = exoPlayer
	}

	fun calcMatrix(recipeStep: RecipeStep, width: Int, height: Int) {
		val matrix = Matrix()
		recipeStep.video?.zoom?.let {
			val centerXRatio = recipeStep.video?.x ?: 0.5f
			val centerYRatio = recipeStep.video?.y ?: 0.5f

			val surface = playerView.videoSurfaceView as TextureView
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
		(playerView.videoSurfaceView as TextureView).setTransform(matrix)
	}

	override fun onLoaderReset(loader: Loader<Recipe>) {
		sectionAdapter?.recipe = null
	}
}
