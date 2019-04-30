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

class OpenSourceTile(context: Context) : QuickTile(context) {
    init {

        this.iconRes = R.drawable.ic_code_black_24dp
        this.titleRes = R.string.open_source
        this.summary = "https://github.com/Tornaco/Tor-ScreenRec"
        this.tileView = object : QuickTileView(context, this@OpenSourceTile) {
            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                summaryTextView!!.autoLinkMask = Linkify.ALL
            }
        }
    }
}
