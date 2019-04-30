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

class XposedInstallTile(context: Context) : QuickTile(context) {

    init {

        this.titleRes = R.string.title_xposed_install
        this.summaryRes = R.string.summary_xposed_install
        this.iconRes = R.mipmap.avatar_xposed

        this.tileView = object : QuickTileView(context, this@XposedInstallTile) {
            override fun useStaticTintColor(): Boolean {
                return false
            }

            override fun onClick(v: View) {
                super.onClick(v)
            }
        }
    }
}
