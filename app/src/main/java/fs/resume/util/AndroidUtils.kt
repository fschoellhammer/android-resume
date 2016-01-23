package fs.resume.util

import android.graphics.drawable.Drawable
import android.support.annotation.LayoutRes
import android.support.v4.graphics.drawable.DrawableCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import fs.resume.R


/** directly inflate layouts from a [ViewGroup] **/
fun ViewGroup.inflate(@LayoutRes layoutRes : Int, attach : Boolean = false) : View {
    return LayoutInflater.from(this.context).inflate(layoutRes, this, attach);
}

/** property to change visibility between [View.GONE] and [View.VISIBLE] **/
var View.visible: Boolean
    get() = visibility != View.GONE
    set(show) { visibility = if (show) View.VISIBLE else View.GONE }

/** tint all compounds of a [TextView] with the given color **/
fun TextView.setCompoundDrawableTint(color : Int) {
    compoundDrawablesRelative.filterNotNull().forEach { it.setCompatTint(color) }
}

/** tint a drawable using [DrawableCompat] **/
fun Drawable.setCompatTint(color : Int) : Drawable {
    DrawableCompat.wrap(this)
    DrawableCompat.setTint(this, color)
    return this
}