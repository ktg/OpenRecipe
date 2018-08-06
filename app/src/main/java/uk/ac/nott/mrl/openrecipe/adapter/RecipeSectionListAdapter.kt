package uk.ac.nott.mrl.openrecipe.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recipe_section.view.*
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.model.Recipe
import uk.ac.nott.mrl.openrecipe.model.RecipeStep

open class RecipeSectionListAdapter : RecyclerView.Adapter<RecipeSectionListAdapter.ViewHolder>() {
	inner class ViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {
		fun setMedia(item: RecipeStep) {
			root.section.visibility = View.VISIBLE
			val index = recipe?.steps?.indexOf(item)
			root.sectionTitle.text = if(item.name.isBlank()) {
				if(index != null) {
					"Step ${index + 1}"
				}  else {
					"Step"
				}
			} else {
				item.name
			}
			if (index == (itemCount - 1)) {
				root.bottomLine.visibility = View.GONE
			} else {
				root.bottomLine.visibility = View.VISIBLE
			}
			root.setOnClickListener {
				if (index != null && index > -1) {
					itemSelected?.invoke(index)
				}
			}
			if (index == selectedIndex) {
				root.sectionTitle.isSelected = true
				root.sectionTitle.setBackgroundResource(R.color.colorPrimaryDark)
			} else {
				root.sectionTitle.isSelected = false
				root.sectionTitle.setBackgroundResource(R.color.colorPrimary)
			}
		}
	}

	var recipe: Recipe? = null
		set(value) {
			field = value
			notifyDataSetChanged()
		}
	var itemSelected: ((Int) -> Unit)? = null
	var selectedIndex: Int? = null
		set(value) {
			val oldValue = field
			field = value
			if (value != null) {
				notifyItemChanged(value)
			}
			if (oldValue != null) {
				notifyItemChanged(oldValue)
			}
		}

	override fun getItemCount(): Int {
		return recipe?.steps?.size ?: 0
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recipe_section, parent, false))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.setMedia(recipe!!.steps[position])
	}
}
