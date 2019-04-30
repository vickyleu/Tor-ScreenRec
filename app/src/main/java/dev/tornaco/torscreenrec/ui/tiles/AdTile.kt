package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.view.View
import android.widget.Toast

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class AdTile(context: Context) : QuickTile(context) {

    private var clicked = 0

    private var startTime: Long = 0

    private var toast: Toast? = null

    init {
        this.titleRes = R.string.title_ad
        this.iconRes = R.drawable.ic_rss_feed_black_24dp
        this.tileView = object : QuickTileView(context, this@AdTile) {
            override fun onClick(v: View) {
                super.onClick(v)
                if (toast != null) toast!!.cancel()
                toast = Toast.makeText(context, clicked.toString(), Toast.LENGTH_SHORT)
                toast!!.show()
                if (startTime == 0L) {
                    startTime = System.currentTimeMillis()
                }
                clicked++
                if (clicked > 128) {
                    SettingsProvider.get()!!.putBoolean(SettingsProvider.Key.PAID, true)
                    val takeTime = System.currentTimeMillis() - startTime
                    Toast.makeText(context, ":+" + takeTime + "ms", Toast.LENGTH_LONG).show()
                    clicked = 0
                }
            }
        }
    }
}
