package uk.ac.nott.mrl.openrecipe.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.android.synthetic.main.recipe_edit.*
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.adapter.RecipeEditAdapter
import uk.ac.nott.mrl.openrecipe.adapter.RecipeSectionEditListAdapter
import uk.ac.nott.mrl.openrecipe.adapter.VideoListAdapter
import uk.ac.nott.mrl.openrecipe.model.Recipe
import uk.ac.nott.mrl.openrecipe.model.RecipeStep


class RecipeEditActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Recipe> {

	private var sectionAdapter: RecipeSectionEditListAdapter? = null
	private var recipeAdapter: RecipeEditAdapter? = null
	private var videoListAdapter: VideoListAdapter? = null
	private val videoHandler = Handler()

	private val videoRunnable: Runnable = Runnable {
		updateVideo()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.recipe_edit)

		setSupportActionBar(toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		recipeEdit.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(s: Editable?) {
				recipeAdapter?.recipe?.name = s.toString()
				recipeAdapter?.save()
			}

			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
			}

			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
			}
		})

		load()
	}

	private fun updateVideo() {
		val loaderManager = LoaderManager.getInstance(this)
		loaderManager.restartLoader(1, null, videoListAdapter!!)
		videoHandler.postDelayed(videoRunnable, 5000)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == VIDEO_REQUEST) {
			if (resultCode == Activity.RESULT_OK) {
				val result = data?.getStringExtra("step")
				println("Result: $result")
				LoaderManager.getInstance(this).destroyLoader(0)
				val recipeStep = Gson().fromJson(result, RecipeStep::class.java)
				val index = data?.getIntExtra("index", -1)
				val recipe = recipeAdapter?.recipe
				if (index != null && index > -1 && recipeStep != null && recipe != null) {
					recipe.steps[index] = recipeStep
					sectionAdapter?.notifyItemChanged(index)
					recipeAdapter?.notifyItemChanged(index)
					recipeAdapter?.save()
				}
			}
			if (resultCode == Activity.RESULT_CANCELED) {
				//Write your code if there's no result
			}
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			android.R.id.home -> {
				println(intent.getStringExtra("recipe"))
				val intent = Intent(this, RecipeActivity::class.java).apply {
					putExtra("recipe", intent.getStringExtra("recipe"))
				}
				NavUtils.navigateUpTo(this, intent)
				return true
			}
		}

		return super.onOptionsItemSelected(item)
	}

	override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<Recipe> {
		return recipeAdapter!!.onCreateLoader(loaderId, args)
	}

	override fun onLoadFinished(loader: Loader<Recipe>, data: Recipe?) {
		data?.let {
			recipeEdit.setText(it.name)
		}
		recipeAdapter?.onLoadFinished(loader, data)
		sectionAdapter?.recipe = data
		recipeEdit.findFocus()
	}

	override fun onLoaderReset(loader: Loader<Recipe>) {
		recipeAdapter?.onLoaderReset(loader)
	}

	private fun load() {
		println("Load?")
		val loaderManager = LoaderManager.getInstance(this)
		val recipeID = intent.getStringExtra("recipe")
		videoListAdapter = VideoListAdapter(this, recipeID)
		videoListView.layoutManager = LinearLayoutManager(this, LinearLayout.HORIZONTAL, false)
		videoListView.adapter = videoListAdapter
		updateVideo()

		recipeAdapter = RecipeEditAdapter(this, recipeID)
		val recipeLayout = LinearLayoutManager(this)
		recipeView.layoutManager = recipeLayout
		recipeView.adapter = recipeAdapter

		sectionView.layoutManager = LinearLayoutManager(this)
		sectionAdapter = RecipeSectionEditListAdapter()
		sectionView.adapter = sectionAdapter
		recipeAdapter?.delegate = sectionAdapter

		sectionAdapter?.itemAdded = {
			sectionAdapter?.notifyItemInserted(it)
			recipeAdapter?.notifyItemInserted(it)
			recipeAdapter?.save()
			recipeLayout.smoothScrollToPosition(recipeView, RecyclerView.State(), it)
		}
		sectionAdapter?.itemSelected = {
			recipeLayout.smoothScrollToPosition(recipeView, RecyclerView.State(), it)
		}
		sectionAdapter?.itemEdited = {
			recipeAdapter?.notifyItemChanged(it)
			recipeAdapter?.save()
		}
		sectionAdapter?.itemDeleted = {
			val section = sectionAdapter?.recipe?.steps?.get(it)
			val index = it
			if (section != null) {
				sectionAdapter?.recipe?.steps?.removeAt(it)
				sectionAdapter?.notifyItemRemoved(it)
				recipeAdapter?.notifyItemRemoved(it)
				recipeAdapter?.save()

				Snackbar.make(root, R.string.section_removed, Snackbar.LENGTH_LONG)
						.setAction(R.string.undo) {
							sectionAdapter?.recipe?.steps?.add(index, section)
							sectionAdapter?.itemAdded?.invoke(index)
						}
						.show()
			}
		}

		loaderManager.initLoader(0, null, this)
	}

	override fun onPause() {
		super.onPause()
		videoHandler.removeCallbacks(videoRunnable)
	}

	companion object {
		const val VIDEO_REQUEST = 3245
	}
}
