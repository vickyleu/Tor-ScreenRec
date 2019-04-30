package dev.tornaco.torscreenrec.camera

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

class AutoFadeLayout : LinearLayout {

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {}

    fun startFading(delay: Long) {
        postDelayed({ alpha = 0.1f }, delay)
    }

    fun stopFading() {
        alpha = 1.0f
    }
}
