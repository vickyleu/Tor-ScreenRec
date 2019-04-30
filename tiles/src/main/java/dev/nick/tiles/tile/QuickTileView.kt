package dev.nick.tiles.tile

import android.content.Context
import android.util.AttributeSet
import android.view.View

open class QuickTileView : TileView {

    private var mTile: QuickTile? = null

    constructor(context: Context, tile: QuickTile) : super(context) {
        mTile = tile
    }

    constructor(context: Context, attrs: AttributeSet, tile: QuickTile) : super(context, attrs) {
        mTile = tile
    }

    override fun onClick(v: View) {
        super.onClick(v)
        if (mTile!!.listener != null) {
            mTile!!.listener?.onTileClick(mTile!!)
        }
    }
}
