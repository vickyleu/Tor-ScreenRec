package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.widget.RelativeLayout

import com.google.common.collect.Lists

import org.newstand.logger.Logger

import dev.nick.library.AudioSource
import dev.nick.tiles.tile.DropDownTileView
import dev.nick.tiles.tile.QuickTile
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

/**
 * Created by Tornaco on 2017/7/26.
 * Licensed with Apache.
 */

class AudioSourceTile(context: Context) : QuickTile(context) {


    init {

        val settingsProvider = SettingsProvider.get()

        this.titleRes = R.string.title_audio_source
        this.iconRes = R.drawable.ic_speaker_black_24dp

        val descList = getDesc(context)

        this.summary = descList[toPosition(settingsProvider!!.getInt(SettingsProvider.Key.AUDIO_SOURCE))]

        this.tileView = object : DropDownTileView(context) {

            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                setSelectedItem(toPosition(settingsProvider.getInt(SettingsProvider.Key.AUDIO_SOURCE)), true)
            }

            override fun onCreateDropDownList(): List<String> {
                return descList
            }

            override fun onItemSelected(position: Int) {
                super.onItemSelected(position)
                val source = toSource(position)
                Logger.i("Selected source:%s", source)
                settingsProvider.putInt(SettingsProvider.Key.AUDIO_SOURCE, source)
                summaryTextView!!.text = descList[position]
            }
        }
    }

    private fun getDesc(context: Context): List<String> {
        return Lists.newArrayList(
                context.getString(R.string.audio_source_noop),
                context.getString(R.string.audio_source_mic),
                context.getString(R.string.audio_source_submix)

        )
    }

    private fun toPosition(source: Int): Int {
        when (source) {
            AudioSource.NOOP -> return 0
            AudioSource.MIC -> return 1
            AudioSource.R_SUBMIX -> return 2
        }
        throw IllegalArgumentException("Bad source")
    }

    private fun toSource(position: Int): Int {
        when (position) {
            0 -> return AudioSource.NOOP
            1 -> return AudioSource.MIC
            2 -> return AudioSource.R_SUBMIX
        }
        throw IllegalArgumentException("Bad source")
    }
}
