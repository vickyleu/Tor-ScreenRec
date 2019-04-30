package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.widget.RelativeLayout

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.SwitchTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

class StopWhenScreenOffTile(context: Context) : QuickTile(context) {

    init {
        this.iconRes = R.drawable.ic_screen_lock_portrait_black_24dp
        this.tileView = object : SwitchTileView(context) {

            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                isChecked = SettingsProvider.get()!!.getBoolean(SettingsProvider.Key.SCREEN_OFF_STOP)
            }

            override fun onCheckChanged(checked: Boolean) {
                super.onCheckChanged(checked)
                SettingsProvider.get()!!.putBoolean(SettingsProvider.Key.SCREEN_OFF_STOP, checked)
            }
        }
        this.titleRes = R.string.title_stop_when_screen_off
    }
}
