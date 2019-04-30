package dev.tornaco.torscreenrec.ui.tiles

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

class FrameRateTile(context: Context) : QuickTile(context, null) {
    init {

        this.titleRes = R.string.title_frame_rate
        this.iconRes = R.drawable.ic_filter_frames_black_24dp
        this.summary = SettingsProvider.get()!!.getInt(SettingsProvider.Key.FAME_RATE).toString()

        this.tileView = object : EditTextTileView(context) {
            override val inputType: Int
                get() = InputType.TYPE_CLASS_NUMBER

            override val hint: CharSequence?
                get() = SettingsProvider.get()!!.getInt(SettingsProvider.Key.FAME_RATE).toString()

            override val dialogTitle: CharSequence
                get() = context.getString(R.string.title_frame_rate)

            override fun onPositiveButtonClick() {
                super.onPositiveButtonClick()
                val text = editText!!.text.toString().trim { it <= ' ' }
                try {
                    val rate = Integer.parseInt(text)

                    if (rate > 99) {
                        Toast.makeText(context, "<=99 ~.~", Toast.LENGTH_LONG).show()
                        return
                    }

                    SettingsProvider.get()!!.putInt(SettingsProvider.Key.FAME_RATE, rate)
                } catch (e: Throwable) {
                    Toast.makeText(context, Log.getStackTraceString(e), Toast.LENGTH_LONG).show()
                }

                summaryTextView!!.text = SettingsProvider.get()!!.getInt(SettingsProvider.Key.FAME_RATE).toString()
            }
        }
    }
}
