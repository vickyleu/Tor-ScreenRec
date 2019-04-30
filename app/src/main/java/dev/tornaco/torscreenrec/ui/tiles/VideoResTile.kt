package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.widget.RelativeLayout

import java.util.Arrays

import dev.nick.library.ValidResolutions
import dev.nick.tiles.tile.DropDownTileView
import dev.nick.tiles.tile.QuickTile
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class VideoResTile(context: Context) : QuickTile(context) {

    private val mResDescs: Array<String>

    init {
        this.iconRes = R.drawable.ic_play_circle_filled_black_24dp
        this.titleRes = R.string.title_high_res

        mResDescs = ValidResolutions.DESC

        val current = SettingsProvider.get()!!.getString(SettingsProvider.Key.RESOLUTION)

        mResDescs[ValidResolutions.INDEX_MASK_AUTO] = context.getString(R.string.summary_res_auto)

        this.summary = current?:""

        this.tileView = object : DropDownTileView(context) {

            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                val index = if (current!!.trim { it <= ' ' } == "AUTO") 0 else ValidResolutions.indexOf(current)
                setSelectedItem(index, false)
            }

            override fun onCreateDropDownList(): List<String> {
                return Arrays.asList(*mResDescs)
            }

            override fun onItemSelected(position: Int) {
                super.onItemSelected(position)
                SettingsProvider.get()!!.putString(SettingsProvider.Key.RESOLUTION,
                        ValidResolutions.DESC[position])
                summaryTextView!!.text = mResDescs[position]
            }
        }
    }
}
