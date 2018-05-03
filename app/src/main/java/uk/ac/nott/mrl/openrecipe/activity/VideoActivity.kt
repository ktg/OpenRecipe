package uk.ac.nott.mrl.openrecipe.activity

import android.app.LoaderManager
import android.content.Context
import android.content.Intent
import android.content.Loader
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.gson.Gson
import kotlinx.android.synthetic.main.video.*
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.adapter.RecipeEditAdapter
import uk.ac.nott.mrl.openrecipe.loader.RecipeLoader
import uk.ac.nott.mrl.openrecipe.model.Recipe
import uk.ac.nott.mrl.openrecipe.model.RecipeStep
import java.util.concurrent.TimeUnit

class VideoActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Recipe> {
	enum class Status {
		Loading,
		Idle,
		Playing
	}

	private var currentDuration = 0L
	private var exoPlayer: SimpleExoPlayer? = null
	private var exoProgressUpdater = UIUpdater({
		exoPlayer?.let {
			updateProgress(it.contentPosition, it.bufferedPosition, it.duration)
		}
	})

	var status: Status = Status.Idle
		set(value) {
			field = value
			Log.i(TAG, "Set status ${value.name}")
			if (value == Status.Loading) {
				spinner.visibility = View.VISIBLE
				playButton.visibility = View.GONE
				pauseButton.visibility = View.GONE
				playerProgress.isEnabled = false
			} else {
				spinner.visibility = View.GONE
				playerProgress.isEnabled = true

				if (value == Status.Idle) {
					playButton.visibility = View.VISIBLE
					playButton.isEnabled = true
					pauseButton.visibility = View.GONE
				} else if (value == Status.Playing) {
					playButton.visibility = View.GONE
					pauseButton.visibility = View.VISIBLE
				}
			}
		}

	private fun updateProgress(progress: Long, buffered: Long, duration: Long) {
		playerProgress.setDuration(duration)
		playerProgress.setBufferedPosition(buffered)
		playerProgress.setPosition(progress)
		playerDuration.text = formatElapsedTime(duration, duration)
		playerPosition.text = formatElapsedTime(progress, duration)
		currentDuration = duration
		recipeStep?.let {
			playerProgress.setAdGroupTimesMs(longArrayOf(it.start, it.end), booleanArrayOf(false, false), 2)
		}

	}

	private val gson = Gson()
	var recipe: Recipe? = null
	var recipeStep: RecipeStep? = null
		set(value) {
			field = value
			val dataSourceFactory = DefaultDataSourceFactory(this, USER_AGENT)
			val extractorMediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
			if (value != null) {
				val uri = Uri.parse(value.uri)
				exoPlayer?.prepare(extractorMediaSource.createMediaSource(uri))
				playerMediaName.text = value.name
			}
			//updateCurrent()
		}

	private val trackSelector = DefaultTrackSelector(DefaultBandwidthMeter())
	private val playerListener = object : Player.DefaultEventListener() {
		override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
			println("onPlayerStateChanged")
			when (playbackState) {
				Player.STATE_BUFFERING -> {
					status = Status.Loading
					exoProgressUpdater.stop()
				}
				Player.STATE_READY -> {
					if (playWhenReady) {
						status = Status.Playing
						exoProgressUpdater.start()
					} else {
						status = Status.Idle
						exoProgressUpdater.run()
						exoProgressUpdater.stop()
					}
				}
				else -> {
					status = Status.Idle
					exoProgressUpdater.stop()
				}
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.video)

		window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				or View.SYSTEM_UI_FLAG_IMMERSIVE)

		//setSupportActionBar(toolbar)
		//supportActionBar?.setDisplayHomeAsUpEnabled(true)
		//supportActionBar?.setDisplayShowTitleEnabled(false)

		playButton.setOnClickListener {
			exoPlayer?.playWhenReady = true
		}

		pauseButton.setOnClickListener {
			exoPlayer?.playWhenReady = false
		}

		markStartButton.setOnClickListener {
			exoPlayer?.let {
				recipeStep?.start = it.contentPosition
				updateProgress(it.contentPosition, it.bufferedPosition, it.duration)
			}
		}

		markEndButton.setOnClickListener {
			exoPlayer?.let {
				recipeStep?.end = it.contentPosition
				updateProgress(it.contentPosition, it.bufferedPosition, it.duration)
			}
		}

		playerProgress.addListener(object : TimeBar.OnScrubListener {
			override fun onScrubMove(timeBar: TimeBar?, position: Long) {
				playerPosition.text = formatElapsedTime(position, currentDuration)
			}

			override fun onScrubStart(timeBar: TimeBar?, position: Long) {}
			override fun onScrubStop(timeBar: TimeBar?, position: Long, canceled: Boolean) {
				if (!canceled) {
					exoPlayer?.seekTo(position)
				}
			}
		})

		status = Status.Loading
	}

	private fun load(bundle: Bundle?) {
		val intentBundle = intent.extras
		var loadBundle = bundle
		if (intentBundle != null && intentBundle.containsKey("recipe")) {
			loadBundle = intentBundle
		}

		loaderManager.initLoader(0, loadBundle, this)
	}

	override fun onResume() {
		super.onResume()
		val renderersFactory = DefaultRenderersFactory(this, null)
		exoPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector)
		exoPlayer?.addListener(playerListener)

		playerView.player = exoPlayer
		load(null)
	}

	override fun onPause() {
		super.onPause()
		exoPlayer?.release()
		recipe?.let {
			val sharedPref = getSharedPreferences(RecipeEditAdapter.RECIPE_PREFERENCE, Context.MODE_PRIVATE)
			sharedPref.edit()
					.putString(it.id, gson.toJson(it))
					.apply()
		}
	}

	public override fun onSaveInstanceState(savedInstanceState: Bundle) {
		savedInstanceState.putLong(STATE_PROGRESS, exoPlayer?.contentPosition ?: 0)
		super.onSaveInstanceState(savedInstanceState)
	}

	public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		exoPlayer?.seekTo(savedInstanceState.getLong(STATE_PROGRESS, 0))
	}

	private fun formatElapsedTime(time: Long, max: Long): String {
		return when {
			max > 3600000 -> {
				val hours = TimeUnit.MILLISECONDS.toHours(time)
				val mins = TimeUnit.MILLISECONDS.toMinutes(time) % 60
				val seconds = TimeUnit.MILLISECONDS.toSeconds(time) % 60
				String.format("%d:%02d:%02d", hours, mins, seconds)
			}
			max > 600000 -> {
				val mins = TimeUnit.MILLISECONDS.toMinutes(time) % 60
				val seconds = TimeUnit.MILLISECONDS.toSeconds(time) % 60
				String.format("%02d:%02d", mins, seconds)
			}
			else -> {
				val mins = TimeUnit.MILLISECONDS.toMinutes(time) % 60
				val seconds = TimeUnit.MILLISECONDS.toSeconds(time) % 60
				String.format("%d:%02d", mins, seconds)
			}
		}
	}

	override fun onCreateLoader(id: Int, args: Bundle?): Loader<Recipe> {

		return RecipeLoader(this)
	}

	override fun onLoadFinished(loader: Loader<Recipe>?, data: Recipe?) {
		this.recipe = data
		data?.let {
			val index = intent.getIntExtra("step", -1)
			if (index > -1) {
				this.recipeStep = it.steps[index]
			}
		}
	}

	override fun onLoaderReset(loader: Loader<Recipe>?) {
	}

	companion object {
		private val TAG = VideoActivity::class.java.simpleName
		const val USER_AGENT = "OpenRecipe"
		private const val STATE_PROGRESS = "progress"

		fun start(context: Context, video: RecipeStep) {
			context.startActivity(intent(context, video))
		}

		private fun intent(context: Context, video: RecipeStep): Intent {
			return Intent(context, VideoActivity::class.java)
					.putExtra("recipeStep", video.uri)
		}
	}
}