package dev.tornaco.torscreenrec.ui.widget

import android.content.Context
import android.content.res.Configuration
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.AnimationUtils

import dev.tornaco.torscreenrec.R

class RecordingButton : FloatingActionButton {

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    fun onRecording() {
        val animation = AnimationUtils.loadAnimation(context, R.anim.rotate)
        animation.setInterpolator(context, android.R.anim.linear_interpolator)
        startAnimation(animation)
    }

    fun onStopRecording() {
        clearAnimation()
    }

    public override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}
