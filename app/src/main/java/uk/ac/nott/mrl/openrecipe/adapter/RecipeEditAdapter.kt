package uk.ac.nott.mrl.openrecipe.adapter

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.recipe_edit_step.view.*
import kotlinx.android.synthetic.main.recipe_section_edit.view.*
import okhttp3.*
import uk.ac.nott.mrl.openrecipe.OpenRecipe
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.activity.RecipeEditActivity
import uk.ac.nott.mrl.openrecipe.activity.VideoActivity
import uk.ac.nott.mrl.openrecipe.loader.RecipeLoader
import uk.ac.nott.mrl.openrecipe.model.Recipe
import uk.ac.nott.mrl.openrecipe.model.RecipeStep
import uk.ac.nott.mrl.openrecipe.model.Video
import java.io.IOException

class RecipeEditAdapter(private val context: Activity, private val id: String) : RecyclerView.Adapter<RecipeEditAdapter.ViewHolder>(), LoaderManager.LoaderCallbacks<Recipe> {
	inner class ViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {
		var recipeStep: RecipeStep? = null

		init {
			root.stepText.addTextChangedListener(object : TextWatcher {
				override fun afterTextChanged(s: Editable?) {
					recipeStep?.let {
						it.text = s.toString()
						val index = recipe?.steps?.indexOf(it)
						println("Index of editing: $index")
						save()
					}
				}

				override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
				override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			})
		}

		private fun restoreImage(item: RecipeStep) {
			val url = item.video?.getURL(OpenRecipe.server.urlRoot, item.start)
			if (url != null) {
				root.stillImage.setImageResource(R.drawable.bg_video)
				Picasso.get()
						.load(url)
						.into(root.stillImage)
				root.dropTarget.visibility = View.INVISIBLE
			} else {
				root.stillImage.setImageResource(R.drawable.bg_video)
				root.dropTarget.visibility = View.VISIBLE
			}
		}

		fun setMedia(item: RecipeStep) {
			recipeStep = null
			val index = recipe?.steps?.indexOf(item)
			if (item.name.isNotBlank()) {
				root.stepVideoCaption.text = item.name + " Clip"
				root.stillImage.contentDescription = item.name + " Clip"
			} else if (index != null && index > -1) {
				root.stepVideoCaption.text = "Step " + (index + 1) + " Clip"
				root.stillImage.contentDescription = "Step " + (index + 1) + " Clip"
			} else {
				root.stepVideoCaption.text = "Step Clip"
				root.stillImage.contentDescription = "Step Clip"
			}
			root.stepText.visibility = View.VISIBLE
			root.stepText.text = item.text

			restoreImage(item)
			root.stillImage.isClickable = true
			root.stillImage.setOnClickListener { _ ->
				if (item.video != null) {
					recipe?.let {
						saveNow(object : Callback {
							override fun onFailure(call: Call?, e: IOException?) {
								println("Save failed! ${e?.message}")
							}

							override fun onResponse(call: Call?, response: Response?) {
								VideoActivity.start(context, it, it.steps.indexOf(item), RecipeEditActivity.VIDEO_REQUEST)
							}
						})
					}
				}
			}
			root.setOnDragListener { _, event ->
				val video = event.localState as Video?
				when (event.action) {
					DragEvent.ACTION_DRAG_ENTERED -> video?.let {
						val url = it.getURL(OpenRecipe.server.urlRoot, it.start)
						Picasso.get()
								.load(url)
								.placeholder(R.drawable.ic_local_movies_black_64dp)
								.error(R.drawable.ic_local_movies_black_64dp)
								.into(root.stillImage)
					}
					DragEvent.ACTION_DROP -> {
						video?.let {
							item.video = video
							item.start = video.start
							item.end = video.end
							save()
						}
					}
					DragEvent.ACTION_DRAG_EXITED -> restoreImage(item)
					DragEvent.ACTION_DRAG_ENDED -> restoreImage(item)
					else -> {
						// Do nothing
					}
				}
				true
			}
			recipeStep = item
		}
	}

	var recipe: Recipe? = null
		set(value) {
			field = value
			notifyDataSetChanged()
		}
	private val gson = Gson()
	private val saver = Runnable {
		saveInternal(object : Callback {
			override fun onFailure(call: Call?, e: IOException?) {
				println("Save failed! ${e?.message}")
			}

			override fun onResponse(call: Call?, response: Response?) {
				println("Save success")
			}
		})
	}

	private val saveHandler = Handler()
	var delegate: RecyclerView.Adapter<*>? = null

	fun save() {
		saveHandler.removeCallbacks(saver)
		saveHandler.postDelayed(saver, SAVE_DELAY)
	}

	fun saveNow(callback: Callback) {
		saveHandler.removeCallbacks(saver)
		saveInternal(callback)
	}

	private fun saveInternal(callback: Callback) {
		val request = Request.Builder()
				.url(OpenRecipe.server.buildURL()
						.addPathSegments("api/recipes")
						.addPathSegment(id)
						.build())
				.put(RequestBody.create(JSON, gson.toJson(recipe)))
				.build()

		OpenRecipe.server.call(request).enqueue(callback)
	}

	override fun getItemCount(): Int {
		return recipe?.steps?.size ?: 0
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recipe_edit_step, parent, false))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.setMedia(recipe!!.steps[position])
	}

	override fun onCreateLoader(id: Int, args: Bundle?): Loader<Recipe> {
		return RecipeLoader(context, this.id)
	}

	override fun onLoadFinished(loader: Loader<Recipe>, data: Recipe?) {
		recipe = data
	}

	override fun onLoaderReset(loader: Loader<Recipe>) {
	}

	companion object {
		private const val SAVE_DELAY = 2000L
		private val JSON = MediaType.parse("application/json; charset=utf-8")
	}
}
