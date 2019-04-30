package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.widget.RelativeLayout

import java.util.Arrays

import dev.nick.tiles.tile.DropDownTileView
import dev.nick.tiles.tile.QuickTile
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.control.FloatControlTheme
import dev.tornaco.torscreenrec.pref.SettingsProvider

class FloatControlThemeTile(context: Context) : QuickTile(context) {

    private var mSources: Array<String?>? = null

    init {

        this.iconRes = R.drawable.ic_color_lens_black_24dp

        this.mSources = arrayOfNulls(FloatControlTheme.values().size)
        for (i in mSources!!.indices) {
            mSources!![i] = context.getString(FloatControlTheme.values()[i].stringRes)
        }

        this.tileView = object : DropDownTileView(context) {

            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                val index = FloatControlTheme.valueOf(SettingsProvider.get()!!.getString(SettingsProvider.Key.FLOAT_WINDOW_THEME)?:"").ordinal
                setSelectedItem(index, false)
            }

            override fun onCreateDropDownList(): List<String> {
                return Arrays.asList<String>(*mSources!!)
            }

            override fun onItemSelected(position: Int) {
                super.onItemSelected(position)
                SettingsProvider.get()!!.putString(
                        SettingsProvider.Key.FLOAT_WINDOW_THEME, FloatControlTheme.from(position).name)
                summaryTextView!!.text = mSources!![position]
            }
        }
        val index = FloatControlTheme.valueOf(SettingsProvider.get()!!.getString(SettingsProvider.Key.FLOAT_WINDOW_THEME)?:"").ordinal
        this.titleRes = R.string.float_theme
        this.summary = mSources!![index]?:""
    }
}
