package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.BuildConfig
import dev.tornaco.torscreenrec.R

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class VersionTile(context: Context) : QuickTile(context) {
    init {

        this.iconRes = R.drawable.ic_phone_android_black_24dp
        this.titleRes = R.string.title_version
        this.summary = BuildConfig.VERSION_NAME + "-" + BuildConfig.BUILD_TYPE
        this.tileView = QuickTileView(context, this)
    }
}
