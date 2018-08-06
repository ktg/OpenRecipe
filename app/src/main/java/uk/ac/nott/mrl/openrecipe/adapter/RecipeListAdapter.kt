package uk.ac.nott.mrl.openrecipe.adapter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.recipe_item.view.*
import okhttp3.*
import uk.ac.nott.mrl.openrecipe.BuildConfig
import uk.ac.nott.mrl.openrecipe.OpenRecipe
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.activity.RecipeActivity
import uk.ac.nott.mrl.openrecipe.activity.RecipeEditActivity
import uk.ac.nott.mrl.openrecipe.loader.RecipeChildLoader
import uk.ac.nott.mrl.openrecipe.model.Recipe
import java.io.IOException


class RecipeListAdapter(private val context: Context, private val parent: String?) : RecyclerView.Adapter<RecipeListAdapter.ViewHolder>(), LoaderManager.LoaderCallbacks<List<Recipe>> {
	inner class ViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {

		fun setMedia(item: Recipe) {
			println("Listing ${item.name}")
			root.recipeName.text = item.name
			root.recipeAuthor.text = item.author
			var icon : String? = null
			for(step in item.steps.reversed()) {
				icon = step.getURL()
				if(icon != null) {
					break
				}
			}
			if (icon != null) {
				root.recipeIcon.visibility = View.VISIBLE
				root.recipeIconDefault.visibility = View.INVISIBLE
				Picasso.get()
						.load(icon)
						.error(R.drawable.ic_receipt_24dp)
						.placeholder(R.drawable.ic_receipt_24dp)
						.into(root.recipeIcon)
			} else {
				root.recipeIcon.visibility = View.INVISIBLE
				root.recipeIconDefault.visibility = View.VISIBLE
			}
			root.bottomLine.visibility = View.VISIBLE
			root.bottomLine.requestLayout()
			root.itemContent.isClickable = true
			root.itemContent.setOnClickListener {
				val intent = Intent(context, RecipeActivity::class.java).apply {
					putExtra("recipe", item._id)
				}
				context.startActivity(intent)
			}
		}

		fun setAddRecipe() {
			root.isClickable = true
			root.setOnClickListener {
				// TODO Show progress?
				Log.i("RecipeList", "Add Recipe")

				val sharedPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
				val recipe = JsonObject()
				recipe.addProperty("cs", OpenRecipe.server.cookingStation)
				recipe.addProperty("author", sharedPreferences.getString("username", "Kevin Glover"))
				recipe.addProperty("email", sharedPreferences.getString("useremail", ""))

				val parentID = parent ?: "root"
				val body = RequestBody.create(JSON, gson.toJson(recipe))
				val request = Request.Builder()
						.url(OpenRecipe.server.buildURL()
								.addPathSegments("api/recipes")
								.addPathSegment(parentID)
								.build())
						.post(body)
						.build()

				OpenRecipe.server.call(request).enqueue(object : Callback {
					override fun onFailure(call: Call, e: IOException) {
						Log.e("RecipeList", e.message, e)
					}

					@Throws(IOException::class)
					override fun onResponse(call: Call, response: Response) {
						if (!response.isSuccessful) throw IOException("Unexpected code $response")
						response.body().use { responseBody ->
							val recipe = Gson().fromJson(responseBody!!.string(), Recipe::class.java)

							val intent = Intent(context, RecipeEditActivity::class.java).apply {
								putExtra("recipe", recipe._id)
							}
							context.startActivity(intent)
						}
					}
				})
			}
		}
	}

	private var recipes: List<Recipe> = emptyList()
		set(value) {
			field = value
			notifyDataSetChanged()
		}

	override fun getItemCount(): Int {
		return recipes.size + 1
	}

	override fun getItemViewType(position: Int): Int {
		if (position >= recipes.size) {
			return addItem
		}
		return recipeItem
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return if (viewType == recipeItem) {
			ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recipe_item, parent, false))
		} else {
			ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recipe_add_item, parent, false))
		}
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		if (getItemViewType(position) == recipeItem) {
			val recipe = recipes[position]
			holder.setMedia(recipe)
		} else {
			holder.setAddRecipe()
		}
	}

	override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Recipe>> {
		return RecipeChildLoader(context, parent)
	}

	override fun onLoadFinished(loader: Loader<List<Recipe>>, data: List<Recipe>) {
		recipes = data
	}

	override fun onLoaderReset(loader: Loader<List<Recipe>>) {
	}

	private class NewRecipe(private val cs: String, private val author: String, private val email: String) {}

	companion object {
		private val gson = Gson()
		private val JSON = MediaType.parse("application/json; charset=utf-8")
		private const val recipeItem = 123
		private const val addItem = 321
	}
}
