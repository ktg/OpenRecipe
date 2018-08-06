package uk.ac.nott.mrl.openrecipe.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import kotlinx.android.synthetic.main.welcome.*
import okhttp3.*
import uk.ac.nott.mrl.openrecipe.BuildConfig
import uk.ac.nott.mrl.openrecipe.OpenRecipe
import uk.ac.nott.mrl.openrecipe.R
import java.io.IOException

class WelcomeActivity : AppCompatActivity() {
	private val verifyWatcher = object : TextWatcher {
		override fun afterTextChanged(p0: Editable?) {
			startButton.isEnabled = editName.text.isNotBlank() && editEmail.text.isNotBlank() && editServer.text.isNotBlank()
		}

		override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
		override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
	}
	private val editorAction = TextView.OnEditorActionListener { _, actionID, _ ->
		println(actionID)
		if (actionID == EditorInfo.IME_ACTION_GO) {
			submit()
			true
		} else {
			false
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.welcome)

		editName.addTextChangedListener(verifyWatcher)
		editEmail.addTextChangedListener(verifyWatcher)
		editServer.addTextChangedListener(verifyWatcher)
		editServer.setText(getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("server", "http://192.186.0.2:8080/"))

		editEmail.setOnEditorActionListener(editorAction)
		editServer.setOnEditorActionListener(editorAction)
		startButton.setOnClickListener {
			submit()
		}
	}

	private fun showServerEdit(message: String?) {
		runOnUiThread {
			labelServer.visibility = View.VISIBLE
			labelServer.error = message
			editEmail.imeOptions = EditorInfo.IME_ACTION_NEXT
			editServer.requestFocus()
		}
	}

	private fun submit() {
		val sharedPref = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
		sharedPref.edit {
			putString("username", editName.text.toString().trim())
			putString("useremail", editEmail.text.toString().trim())
			putString("server", editServer.text.toString().trim())
		}

		OpenRecipe.server.urlRoot = HttpUrl.parse(editServer.text.toString())!!

		val request = Request.Builder()
				.url(OpenRecipe.server.buildURL()
						.addPathSegments("api/recipes")
						.build())
				.build()

		startButton.isEnabled = false
		OpenRecipe.server.call(request).enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				Log.e("RecipeList", e.message, e)
				showServerEdit(e.message)
			}

			@Throws(IOException::class)
			override fun onResponse(call: Call, response: Response) {
				if (!response.isSuccessful) {
					showServerEdit(response.message())
				} else {
					startActivity(Intent(applicationContext, RecipeRootActivity::class.java))
				}
			}
		})
	}
}
