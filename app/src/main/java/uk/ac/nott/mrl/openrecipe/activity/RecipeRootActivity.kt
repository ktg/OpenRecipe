package uk.ac.nott.mrl.openrecipe.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.list.*
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.adapter.RecipeListAdapter


class RecipeRootActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.list)

		setSupportActionBar(toolbar)
		supportActionBar?.title = "Recipes"

		load()
	}

	private var recipeAdapter: RecipeListAdapter? = null

	private fun load() {
		recipeAdapter = RecipeListAdapter(this, null)
		recyclerView.layoutManager = LinearLayoutManager(this)
		recyclerView.adapter = recipeAdapter
		LoaderManager.getInstance(this).initLoader(0, null, recipeAdapter!!)
	}
}
