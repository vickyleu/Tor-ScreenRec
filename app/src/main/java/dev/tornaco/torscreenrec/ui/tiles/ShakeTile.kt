package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.widget.RelativeLayout

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.SwitchTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

class ShakeTile(context: Context) : QuickTile(context) {

    init {

        this.titleRes = R.string.title_shake_action
        this.iconRes = R.drawable.ic_close_white_24dp

        this.tileView = object : SwitchTileView(context) {
            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                isChecked = SettingsProvider.get()!!.getBoolean(SettingsProvider.Key.SHAKE_STOP)
            }

            override fun onCheckChanged(checked: Boolean) {
                super.onCheckChanged(checked)
                SettingsProvider.get()!!.putBoolean(SettingsProvider.Key.SHAKE_STOP, checked)
            }
        }
    }
}
