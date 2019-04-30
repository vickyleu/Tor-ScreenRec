package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.widget.RelativeLayout

import java.util.Arrays

import dev.nick.tiles.tile.DropDownTileView
import dev.nick.tiles.tile.QuickTile
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

class OrientationTile(context: Context) : QuickTile(context) {

    private val mOriDesc: Array<String>

    init {
        this.iconRes = R.drawable.ic_phone_android_black_24dp
        this.titleRes = R.string.title_orientation

        mOriDesc = context.resources.getStringArray(R.array.orientations)
        this.summary = mOriDesc[SettingsProvider.get()!!.getInt(SettingsProvider.Key.ORIENTATION)]

        this.tileView = object : DropDownTileView(context) {

            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                setSelectedItem(SettingsProvider.get()!!.getInt(SettingsProvider.Key.ORIENTATION), false)
            }

            override fun onCreateDropDownList(): List<String> {
                return Arrays.asList(*mOriDesc)
            }

            override fun onItemSelected(position: Int) {
                super.onItemSelected(position)
                SettingsProvider.get()!!.putInt(SettingsProvider.Key.ORIENTATION, position)
                summaryTextView!!.text = mOriDesc[position]
            }
        }
    }
}