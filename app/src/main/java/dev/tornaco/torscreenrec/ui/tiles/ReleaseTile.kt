package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.text.util.Linkify
import android.widget.RelativeLayout

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.R

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class ReleaseTile(context: Context) : QuickTile(context) {
    init {

        this.iconRes = R.drawable.ic_new_releases_black_24dp
        this.titleRes = R.string.release
        this.summary = "https://github.com/Tornaco/Tor-ScreenRec/releases"
        this.tileView = object : QuickTileView(context, this@ReleaseTile) {
            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                summaryTextView!!.autoLinkMask = Linkify.ALL
            }
        }
    }
}
