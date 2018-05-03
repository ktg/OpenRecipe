package uk.ac.nott.mrl.openrecipe

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class AppGlideModule : AppGlideModule() {
	override fun applyOptions(context: Context, builder: GlideBuilder) {
		// Apply options to the builder here.
	}
}