package uk.ac.nott.mrl.openrecipe.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.TextureView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import com.google.gson.Gson
import kotlinx.android.synthetic.main.video.*
import uk.ac.nott.mrl.openrecipe.OpenRecipe
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.model.Recipe
import uk.ac.nott.mrl.openrecipe.model.RecipeStep
import java.util.concurrent.TimeUnit

class VideoActivity : AppCompatActivity() {
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
			if (setEnd) {
				if (it.end == 0L) {
					it.end = duration
				}
				setEnd = false
			}

			playerProgress.setAdGroupTimesMs(longArrayOf(it.start, it.end), booleanArrayOf(false, false), 2)
		}

	}

	private var setEnd = true
	var matrix = Matrix()
	private var recipeStep: RecipeStep? = null
		set(value) {
			field = value
			editAnnotation.setText(value?.text)
			val dataSourceFactory = DefaultDataSourceFactory(this, USER_AGENT)
			val extractorMediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
			if (value != null) {
				value.video?.let {
					val uri = Uri.parse(it.getURL(OpenRecipe.server.urlRoot))
					val extractedSource = extractorMediaSource.createMediaSource(uri)
					println("End: ${it.end}")
					val end = if (it.end == 0L) {
						C.TIME_END_OF_SOURCE
					} else {
						it.end * 1000
					}
					val clippedSource = ClippingMediaSource(extractedSource, it.start * 1000, end)
					(playerView.videoSurfaceView as TextureView).setTransform(matrix)
					exoPlayer?.prepare(clippedSource)
				}
			}
			//updateCurrent()
		}

	private val trackSelector = DefaultTrackSelector(DefaultBandwidthMeter())
	private val playerListener = object : Player.DefaultEventListener() {
		override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
			println("onPlayerStateChanged")
			(playerView.videoSurfaceView as TextureView).setTransform(matrix)
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

		doneButton.setOnClickListener {
			done()
		}

		editAnnotation.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(text: Editable?) {
				recipeStep?.text = text.toString()
			}

			override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
			override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
		})

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

	private fun done() {
		setResult(Activity.RESULT_OK, Intent()
				.putExtra("step", gson.toJson(recipeStep))
				.putExtra("index", intent.getIntExtra("index", 0)))
		finish()
	}

	override fun onResume() {
		super.onResume()
		val renderersFactory = DefaultRenderersFactory(this)
		exoPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector)
		exoPlayer?.addListener(playerListener)
		exoPlayer?.addVideoListener(object : VideoListener {
			override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
				println("Video size changed")
				recipeStep?.video?.zoom?.let {
					matrix = Matrix()
					val centerXRatio = recipeStep?.video?.x ?: 0.5f
					val centerYRatio = recipeStep?.video?.y ?: 0.5f

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
					(playerView.videoSurfaceView as TextureView).setTransform(matrix)
				}
			}

			override fun onRenderedFirstFrame() {
				(playerView.videoSurfaceView as TextureView).setTransform(matrix)
			}

		})

		playerView.player = exoPlayer
		load()
	}

	private fun load() {
		recipeStep = gson.fromJson(intent.getStringExtra("step"), RecipeStep::class.java)
	}

	override fun onPause() {
		super.onPause()
		exoPlayer?.release()
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			android.R.id.home -> {
				done()
				return true
			}
		}

		return super.onOptionsItemSelected(item)
	}


	override fun onSaveInstanceState(savedInstanceState: Bundle) {
		savedInstanceState.putLong(STATE_PROGRESS, exoPlayer?.contentPosition ?: 0)
		super.onSaveInstanceState(savedInstanceState)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
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

	companion object {
		private val gson = Gson()
		private val TAG = VideoActivity::class.java.simpleName
		const val USER_AGENT = "OpenRecipe"
		private const val STATE_PROGRESS = "progress"

		fun start(context: Activity, recipe: Recipe, step: Int, request: Int) {
			context.startActivityForResult(intent(context, recipe, step), request)
		}

		private fun intent(context: Context, recipe: Recipe, step: Int): Intent {
			return Intent(context, VideoActivity::class.java)
					.putExtra("step", gson.toJson(recipe.steps[step]))
					.putExtra("index", step)
		}
	}
}