package fs.resume.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
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
fun TextView.setCompoundDrawableTint(@ColorInt color : Int) {
    compoundDrawablesRelative.filterNotNull().forEach { it.setCompatTint(color) }
}

/** tint a drawable using [DrawableCompat] **/
fun Drawable.setCompatTint(@ColorInt color : Int) : Drawable {
    DrawableCompat.wrap(this)
    DrawableCompat.setTint(this, color)
    return this
}

/** get a tinted drawable **/
fun Context.getDrawableWithTint(@DrawableRes drawableResId : Int, @ColorRes colorRes : Int) : Drawable {
    return getDrawable(drawableResId).setCompatTint(resources.getColor(colorRes))
}

/** launch intent with activity **/
fun Intent.startActivity(activity : Activity) = activity.startActivity(this)

fun Any.runOnUi(delay : Long = 0, runnable : () -> Unit ) {
    Handler(Looper.getMainLooper()).apply {
        if (delay != 0L) postDelayed(toRunnable(runnable), delay)
        else post(toRunnable(runnable))
    }
}

private fun toRunnable(runnable : () -> Unit) : Runnable {
    return object : Runnable {
        override fun run() : Unit = runnable()
    }
}