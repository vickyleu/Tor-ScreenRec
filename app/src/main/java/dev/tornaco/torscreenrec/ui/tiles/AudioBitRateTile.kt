package dev.tornaco.torscreenrec.ui.tiles

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputType
import android.util.Log
import android.widget.Toast

import dev.nick.tiles.tile.EditTextTileView
import dev.nick.tiles.tile.QuickTile
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

/**
 * Created by Tornaco on 2017/7/21.
 * Licensed with Apache.
 */

class AudioBitRateTile(context: Context) : QuickTile(context, null) {
    init {

        this.titleRes = R.string.title_bitrate_rate
        this.iconRes = R.drawable.ic_audiotrack_black_24dp
        this.summary = SettingsProvider.get()!!.getInt(SettingsProvider.Key.AUDIO_BITRATE_RATE_K).toString() + "K"

        this.tileView = object : EditTextTileView(context) {
            override val inputType: Int
                get() = InputType.TYPE_CLASS_NUMBER

            override val hint: CharSequence?
                get() = SettingsProvider.get()!!.getInt(SettingsProvider.Key.AUDIO_BITRATE_RATE_K).toString()

            override val dialogTitle: CharSequence
                get() = context.getString(R.string.title_bitrate_rate)

            @SuppressLint("SetTextI18n")
            override fun onPositiveButtonClick() {
                super.onPositiveButtonClick()
                val text = editText!!.text.toString().trim { it <= ' ' }
                try {
                    val rate = Integer.parseInt(text)

                    if (rate > 1024) {
                        Toast.makeText(context, "<=1024 ~.~", Toast.LENGTH_LONG).show()
                        return
                    }

                    SettingsProvider.get()!!.putInt(SettingsProvider.Key.AUDIO_BITRATE_RATE_K, rate)
                } catch (e: Throwable) {
                    Toast.makeText(context, Log.getStackTraceString(e), Toast.LENGTH_LONG).show()
                }

                summaryTextView!!.text = SettingsProvider.get()!!.getInt(SettingsProvider.Key.AUDIO_BITRATE_RATE_K).toString() + "K"
            }
        }
    }
}
