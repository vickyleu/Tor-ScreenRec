package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.view.View

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.ui.ContainerHostActivity
import dev.tornaco.torscreenrec.ui.PayListBrowserFragment

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class PayListTile(context: Context) : QuickTile(context) {

    init {

        this.iconRes = R.drawable.ic_playlist_add_check_black_24dp
        this.titleRes = R.string.title_pay_list

        this.tileView = object : QuickTileView(context, this@PayListTile) {
            override fun onClick(v: View) {
                super.onClick(v)
                context.startActivity(ContainerHostActivity.getIntent(context, PayListBrowserFragment::class.java))
            }
        }


    }
}
