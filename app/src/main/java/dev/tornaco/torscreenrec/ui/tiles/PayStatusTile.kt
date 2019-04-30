package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.widget.RelativeLayout
import android.widget.Toast

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.SwitchTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

/**
 * Created by Tornaco on 2017/7/29.
 * Licensed with Apache.
 */

class PayStatusTile(context: Context) : QuickTile(context) {

    init {

        this.titleRes = R.string.title_pay_status_paid
        this.iconRes = R.drawable.ic_check_circle_black_24dp
        this.tileView = object : SwitchTileView(context) {
            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                isChecked = SettingsProvider.get()!!.getBoolean(SettingsProvider.Key.PAID)
            }

            override fun onCheckChanged(checked: Boolean) {
                super.onCheckChanged(checked)
                SettingsProvider.get()!!.putBoolean(SettingsProvider.Key.PAID, checked)

                if (checked) {
                    Toast.makeText(context, "✧ (≖ ‿ ≖)✧", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
