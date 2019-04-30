package dev.nick.tiles.tile

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import android.util.AttributeSet
import android.widget.RelativeLayout

class EmptyActionTileView : TileView {

    protected val actionTextColor: Int
        @ColorInt get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            resources.getColor(android.R.color.holo_blue_light, context.theme)
        } else resources.getColor(android.R.color.holo_blue_light)

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun onBindActionView(container: RelativeLayout) {
        super.onBindActionView(container)
        titleTextView!!.setTextColor(actionTextColor)
    }
}
