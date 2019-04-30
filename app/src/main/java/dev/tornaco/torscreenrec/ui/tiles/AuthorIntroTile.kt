package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.R

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class AuthorIntroTile(context: Context) : QuickTile(context) {
    init {

        this.iconRes = R.drawable.ic_header_avatar
        this.titleRes = R.string.tornaco
        this.summaryRes = R.string.tornaco_intro
        this.tileView = object : QuickTileView(context, this@AuthorIntroTile) {
            override fun useStaticTintColor(): Boolean {
                return false
            }
        }
    }
}
