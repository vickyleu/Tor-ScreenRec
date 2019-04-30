package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.view.View

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.R

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class MagiskInstallTile(context: Context) : QuickTile(context) {

    init {

        this.titleRes = R.string.title_magisk_install
        this.summaryRes = R.string.summary_magisk_install
        this.iconRes = R.mipmap.avatar_magisk

        this.tileView = object : QuickTileView(context, this@MagiskInstallTile) {
            override fun onClick(v: View) {
                super.onClick(v)
            }

            override fun useStaticTintColor(): Boolean {
                return false
            }
        }
    }
}
