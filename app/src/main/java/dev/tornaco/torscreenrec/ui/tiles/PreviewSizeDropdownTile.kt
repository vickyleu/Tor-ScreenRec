package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.widget.RelativeLayout

import java.util.Arrays

import dev.nick.tiles.tile.DropDownTileView
import dev.nick.tiles.tile.QuickTile
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

class PreviewSizeDropdownTile(context: Context) : QuickTile(context) {

    private var mSizes: Array<String>? = null

    init {
        this.mSizes = context.resources.getStringArray(R.array.preview_sizes)

        this.iconRes = R.drawable.ic_photo_size_select_small_black_24dp
        this.titleRes = R.string.title_preview_size
        this.summary = mSizes!![SettingsProvider.get()!!.getInt(SettingsProvider.Key.CAMERA_SIZE)]

        this.tileView = object : DropDownTileView(context) {

            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                setSelectedItem(SettingsProvider.get()!!.getInt(SettingsProvider.Key.CAMERA_SIZE), false)
            }

            override fun onCreateDropDownList(): List<String> {
                return Arrays.asList(*mSizes!!)
            }

            override fun onItemSelected(position: Int) {
                super.onItemSelected(position)
                SettingsProvider.get()!!.putInt(SettingsProvider.Key.CAMERA_SIZE, position)
                summaryTextView!!.text = mSizes!![position]
            }
        }
    }
}