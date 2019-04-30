package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.widget.RelativeLayout

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.SwitchTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class FlowViewTile(context: Context) : QuickTile(context) {

    init {

        this.titleRes = R.string.title_float_view
        this.iconRes = R.drawable.ic_bubble_chart

        this.tileView = object : SwitchTileView(context) {
            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                isChecked = SettingsProvider.get()!!.getBoolean(SettingsProvider.Key.FLOAT_WINDOW)
            }

            override fun onCheckChanged(checked: Boolean) {
                super.onCheckChanged(checked)
                SettingsProvider.get()!!.putBoolean(SettingsProvider.Key.FLOAT_WINDOW, checked)
            }
        }
    }
}
