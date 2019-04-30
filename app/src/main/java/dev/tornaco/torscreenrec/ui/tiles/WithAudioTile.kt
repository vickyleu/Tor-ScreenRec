package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.view.View
import android.widget.RelativeLayout

import com.google.common.collect.Lists
import java.util.Observable
import java.util.Observer

import dev.nick.library.AudioSource
import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.SwitchTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider
import dev.tornaco.torscreenrec.ui.AudioSourceFragment
import dev.tornaco.torscreenrec.ui.ContainerHostActivity

/**
 * Created by Tornaco on 2017/7/27.
 * Licensed with Apache.
 */

class WithAudioTile(context: Context) : QuickTile(context) {

    private val o = Observer { observable, o ->
        if (o === SettingsProvider.Key.AUDIO_SOURCE) {
            tileView!!.summaryTextView!!.text = context.getString(R.string.summary_with_audio,
                    getDesc(context)[toPosition(SettingsProvider.get()!!
                            .getInt(SettingsProvider.Key.AUDIO_SOURCE))])
        }
    }

    init {

        val settingsProvider = SettingsProvider.get()

        settingsProvider!!.addObserver(o)

        this.titleRes = R.string.title_with_audio
        this.iconRes = R.drawable.ic_record_voice_over_black_24dp

        val withAudio = settingsProvider.getBoolean(SettingsProvider.Key.WITH_AUDIO)

        this.summary = context.getString(R.string.summary_with_audio,
                getDesc(context)[toPosition(settingsProvider.getInt(SettingsProvider.Key.AUDIO_SOURCE))])

        this.tileView = object : SwitchTileView(context) {
            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                isChecked = withAudio
            }

            override fun onCheckChanged(checked: Boolean) {
                super.onCheckChanged(checked)
                settingsProvider.putBoolean(SettingsProvider.Key.WITH_AUDIO, checked)
            }

            override fun onClick(v: View) {
                context.startActivity(ContainerHostActivity.getIntent(context, AudioSourceFragment::class.java))
            }

            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
                settingsProvider.deleteObserver(o)
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
}
