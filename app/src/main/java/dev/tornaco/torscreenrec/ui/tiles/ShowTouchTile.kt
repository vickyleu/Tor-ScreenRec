package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.widget.RelativeLayout

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.SwitchTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

class ShowTouchTile(context: Context) : QuickTile(context) {

    init {
        this.iconRes = R.drawable.ic_touch_app_black_24dp
        this.tileView = object : SwitchTileView(context) {

            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                isChecked = SettingsProvider.get()!!.getBoolean(SettingsProvider.Key.SHOW_TOUCH)
            }

            override fun onCheckChanged(checked: Boolean) {
                super.onCheckChanged(checked)
                SettingsProvider.get()!!.putBoolean(SettingsProvider.Key.SHOW_TOUCH, checked)
            }
        }
        this.titleRes = R.string.title_show_touch
    }
}
