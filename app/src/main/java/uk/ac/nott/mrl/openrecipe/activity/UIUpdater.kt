package uk.ac.nott.mrl.openrecipe.activity

import android.os.Handler
import android.os.Looper

class UIUpdater(private val updater: () -> Unit, private val interval: Long = 500L) {
	private val handler = Handler(Looper.getMainLooper())

	private val callback = object : Runnable {
		override fun run() {
			// Run the passed runnable
			updater()
			// Re-run it after the update interval
			handler.postDelayed(this, interval)
		}
	}

	@Synchronized
	fun start() {
		callback.run()
	}

	@Synchronized
	fun stop() {
		handler.removeCallbacks(callback)
	}

	fun run() {
		updater()
	}
}