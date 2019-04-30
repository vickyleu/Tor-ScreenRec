package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.widget.SeekBar

import java.util.Observable
import java.util.Observer

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

class FloatControlAlphaTile(context: Context) : QuickTile(context) {

    init {
        this.titleRes = R.string.float_alpha
        this.iconRes = R.drawable.ic_gradient_black_24dp
        this.tileView = object : QuickTileView(context, this@FloatControlAlphaTile) {
            override fun onClick(v: View) {
                super.onClick(v)
                showAlphaSeeker()
            }
        }
        this.summary = SettingsProvider.get()!!.getInt(SettingsProvider.Key.FLOAT_WINDOW_ALPHA).toString()
        SettingsProvider.get()!!.addObserver { o, arg ->
            if (arg === SettingsProvider.Key.FLOAT_WINDOW_ALPHA) {
                tileView!!.summaryTextView!!.text = SettingsProvider.get()!!.getInt(SettingsProvider.Key.FLOAT_WINDOW_ALPHA).toString()
            }
        }
    }

    private fun showAlphaSeeker() {
        val seekBar = SeekBar(context)
        val alpha = SettingsProvider.get()!!.getInt(SettingsProvider.Key.FLOAT_WINDOW_ALPHA)
        seekBar.max = 100
        seekBar.progress = alpha
        AlertDialog.Builder(context!!)
                .setView(seekBar)
                .setTitle(R.string.float_alpha)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    val current = seekBar.progress
                    SettingsProvider.get()!!.putInt(SettingsProvider.Key.FLOAT_WINDOW_ALPHA, current)
                }
                .create()
                .show()
    }
}
