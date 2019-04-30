package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.view.View

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.ui.ContainerHostActivity
import dev.tornaco.torscreenrec.ui.RecordingBrowserFragment

/**
 * Created by Tornaco on 2017/7/27.
 * Licensed with Apache.
 */

class RecordingBrowserTile(context: Context) : QuickTile(context) {

    init {

        this.titleRes = R.string.title_recording_browser
        this.iconRes = R.drawable.ic_movie_black_24dp

        this.tileView = object : QuickTileView(context, this@RecordingBrowserTile) {
            override fun onClick(v: View) {
                super.onClick(v)
                context.startActivity(ContainerHostActivity.getIntent(
                        context, RecordingBrowserFragment::class.java
                ))
            }
        }
    }
}
