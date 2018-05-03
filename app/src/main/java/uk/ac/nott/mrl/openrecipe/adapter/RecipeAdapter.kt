package uk.ac.nott.mrl.openrecipe.adapter

import android.app.LoaderManager
import android.content.Context
import android.content.Loader
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.gson.Gson
import kotlinx.android.synthetic.main.recipe_step.view.*
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.activity.VideoActivity
import uk.ac.nott.mrl.openrecipe.loader.RecipeLoader
import uk.ac.nott.mrl.openrecipe.model.Recipe
import uk.ac.nott.mrl.openrecipe.model.RecipeStep

class RecipeAdapter(private val context: Context) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>(), LoaderManager.LoaderCallbacks<Recipe> {
	inner class ViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {
		private val exoPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector)

		fun setMedia(item: RecipeStep) {
			root.stepTitle.text = item.name
			root.editText.text = item.text
			root.editText.addTextChangedListener(object : TextWatcher {
				override fun afterTextChanged(s: Editable?) {
					item.text = s.toString()
				}

				override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
				}

				override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				}
			})
			root.isClickable = true
			root.playerView.player = exoPlayer
			item.uri?.let {
				val uri = Uri.parse(it)
				exoPlayer?.prepare(extractorMediaSource.createMediaSource(uri))
			}
		}
	}

	val trackSelector = DefaultTrackSelector(DefaultBandwidthMeter())
	val renderersFactory = DefaultRenderersFactory(context, null)
	private val dataSourceFactory = DefaultDataSourceFactory(context, VideoActivity.USER_AGENT)
	val extractorMediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
	var recipe: Recipe? = null
	private val gson = Gson()

	override fun getItemCount(): Int {
		return recipe?.steps?.size ?: 0
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recipe_step, parent, false))
	}

	fun save() {
		recipe?.let {
			val sharedPref = context.getSharedPreferences(RECIPE_PREFERENCE, Context.MODE_PRIVATE)
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
