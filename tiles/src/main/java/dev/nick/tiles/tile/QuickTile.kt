package dev.nick.tiles.tile

import android.content.Context

abstract class QuickTile : Tile {

    var context: Context? = null
        private set
    internal var listener: TileListener?=null

    constructor(context: Context, listener: TileListener?) {
        this.context = context
        this.listener = listener
    }

    constructor(context: Context?) {
        this.context = context
    }

    fun setEnabled(enabled: Boolean) {
        if (tileView != null)
            tileView!!.isEnabled = enabled
    }
}
