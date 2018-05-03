package uk.ac.nott.mrl.openrecipe.adapter

import android.app.LoaderManager
import android.content.Context
import android.content.Intent
import android.content.Loader
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import kotlinx.android.synthetic.main.recipe_edit_step.view.*
import uk.ac.nott.mrl.openrecipe.GlideApp
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.activity.VideoActivity
import uk.ac.nott.mrl.openrecipe.loader.RecipeLoader
import uk.ac.nott.mrl.openrecipe.model.Recipe
import uk.ac.nott.mrl.openrecipe.model.RecipeStep

class RecipeEditAdapter(private val context: Context) : RecyclerView.Adapter<RecipeEditAdapter.ViewHolder>(), LoaderManager.LoaderCallbacks<Recipe> {
	inner class ViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {
		private fun restoreImage(item: RecipeStep) {
			GlideApp.with(context)
					.load(item.uri)
					.fallback(R.drawable.ic_backdrop)
					.into(root.stillImage)
		}

		fun setMedia(item: RecipeStep) {
			root.stepTitle.text = item.name
			root.editText.setText(item.text)
			root.editText.addTextChangedListener(object: TextWatcher {
				override fun afterTextChanged(s: Editable?) {
					item.text = s.toString()
				}

				override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
				}

				override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				}
			})
			root.isClickable = true
			restoreImage(item)
			root.stillImage.setOnClickListener { _ ->
				if(item.uri != null) {
					recipe?.let {
						root.context.startActivity(
								Intent(root.context, VideoActivity::class.java)
										.putExtra("recipe", it.id)
										.putExtra("step", it.steps.indexOf(item)))
					}
				}
			}
			root.setOnDragListener { _, event ->
				val video = event.localState as Uri?
				when (event.action) {
					DragEvent.ACTION_DRAG_ENTERED -> video?.let {
						GlideApp.with(context)
								.load(it)
								.fallback(R.drawable.ic_backdrop)
								.into(root.stillImage)
					}
					DragEvent.ACTION_DROP -> item.uri = video.toString()
					DragEvent.ACTION_DRAG_EXITED -> restoreImage(item)
					DragEvent.ACTION_DRAG_ENDED -> restoreImage(item)
					else -> {
						// Do nothing
					}
				}
				true
			}
		}
	}

	var recipe: Recipe? = null
	private val gson = Gson()

	override fun getItemCount(): Int {
		return recipe?.steps?.size ?: 0
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recipe_edit_step, parent, false))
	}

	fun save() {
		recipe?.let {
			val sharedPref = context.getSharedPreferences(RecipeEditAdapter.RECIPE_PREFERENCE, Context.MODE_PRIVATE)
			sharedPref.edit()
					.putString(it.id, gson.toJson(it))
					.apply()
		}
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.setMedia(recipe!!.steps[position])
	}

	override fun onCreateLoader(id: Int, args: Bundle?): Loader<Recipe> {
		return RecipeLoader(context)
	}

	override fun onLoadFinished(loader: Loader<Recipe>, data: Recipe?) {
		recipe = data
		notifyDataSetChanged()
	}

	override fun onLoaderReset(loader: Loader<Recipe>) {
	}

	companion object {
		const val RECIPE_PREFERENCE = "recipes"
	}
}
