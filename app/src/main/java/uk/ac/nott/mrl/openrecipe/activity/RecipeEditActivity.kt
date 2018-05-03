package uk.ac.nott.mrl.openrecipe.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.recipe_edit.*
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.adapter.RecipeEditAdapter
import uk.ac.nott.mrl.openrecipe.adapter.VideoListAdapter


class RecipeEditActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.recipe_edit)

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST)
		} else {
			load()
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		when (requestCode) {
			PERMISSION_REQUEST -> {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					load()
				}
			}
		}
	}

	override fun onPause() {
		super.onPause()
		recipeAdapter?.save()
	}

	private var recipeAdapter: RecipeEditAdapter? = null

	private fun load() {
		val videoListAdapter = VideoListAdapter(this)
		videoListView.layoutManager = LinearLayoutManager(this)
		videoListView.adapter = videoListAdapter
		loaderManager.initLoader(1, null, videoListAdapter)

		recipeAdapter = RecipeEditAdapter(this)
		recipeView.layoutManager = LinearLayoutManager(this)
		recipeView.adapter = recipeAdapter
		loaderManager.initLoader(0, null, recipeAdapter)
	}

	companion object {
		private const val PERMISSION_REQUEST = 3245
	}
}
