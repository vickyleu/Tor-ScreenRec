package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.R

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class EmailTile(context: Context) : QuickTile(context) {
    init {

        this.iconRes = R.drawable.ic_mail_outline_black_24dp
        this.titleRes = R.string.title_email
        this.summary = "tornaco@163.com"
        this.tileView = QuickTileView(context, this)
    }
}
