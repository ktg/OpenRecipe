package uk.ac.nott.mrl.openrecipe.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recipe_section_edit.view.*
import uk.ac.nott.mrl.openrecipe.R
import uk.ac.nott.mrl.openrecipe.model.Recipe
import uk.ac.nott.mrl.openrecipe.model.RecipeStep

open class RecipeSectionEditListAdapter : RecyclerView.Adapter<RecipeSectionEditListAdapter.ViewHolder>() {
	inner class ViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {
		var recipeStep: RecipeStep? = null

		init {
			root.sectionTitle.addTextChangedListener(object : TextWatcher {
				override fun afterTextChanged(s: Editable?) {
					recipeStep?.name = s.toString()
					val index = recipe?.steps?.indexOf(recipeStep)
					if (index != null && index > -1) {
						itemEdited?.invoke(index)
					}
				}

				override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
				override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			})
		}

		fun setMedia(item: RecipeStep) {
			recipeStep = item
			root.sectionAdd.visibility = View.GONE
			root.section.visibility = View.VISIBLE
			root.sectionTitle.setText(item.name)

			root.setOnClickListener {
				val index = recipe?.steps?.indexOf(item)
				if (index != null && index > -1) {
					itemSelected?.invoke(index)
				}
			}
			root.deleteStep.setOnClickListener {
				val index = recipe?.steps?.indexOf(item)
				if (index != null && index > -1) {
					itemDeleted?.invoke(index)
				}
			}
			root.sectionTitle.setOnFocusChangeListener { v, hasFocus ->
				if (hasFocus) {
					val index = recipe?.steps?.indexOf(item)
					if (index != null && index > -1) {
						itemSelected?.invoke(index)
					}
				}
			}

			val index = recipe?.steps?.indexOf(item)
			if(index != null && index > -1) {
				root.sectionTitle.hint = "Step " + (index + 1) + " Name"
			} else {
				root.sectionTitle.hint = "Step Name"
			}
			if (index == (itemCount - 1)) {
				root.bottomLine.visibility = View.INVISIBLE
			} else {
				root.bottomLine.visibility = View.VISIBLE
			}
			if (index == selectedIndex) {
				root.sectionTitle.isSelected = true
				root.sectionTitle.setBackgroundResource(R.color.colorPrimaryDark)
			} else {
				root.sectionTitle.isSelected = false
				root.sectionTitle.setBackgroundResource(R.color.colorPrimary)
			}
		}

		fun showAdd() {
			root.sectionAdd.visibility = View.VISIBLE
			root.section.visibility = View.GONE
			root.bottomLine.visibility = View.GONE
			root.setOnClickListener {
				recipe?.let {
					it.steps.add(RecipeStep())
					val index = it.steps.size - 1
					itemAdded?.invoke(index)

				}
			}
		}
	}

	var recipe: Recipe? = null
		set(value) {
			field = value
			notifyDataSetChanged()
		}
	var itemSelected: ((Int) -> Unit)? = null
	var itemAdded: ((Int) -> Unit)? = null
	var itemEdited: ((Int) -> Unit)? = null
	var itemDeleted: ((Int) -> Unit)? = null
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
		return (recipe?.steps?.size ?: 0) + 1
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recipe_section_edit, parent, false))
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		if (position >= (recipe?.steps?.size ?: 0)) {
			holder.showAdd()
		} else {
			holder.setMedia(recipe!!.steps[position])
		}
	}
}
