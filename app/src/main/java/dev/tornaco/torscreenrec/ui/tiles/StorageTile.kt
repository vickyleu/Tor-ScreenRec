package dev.tornaco.torscreenrec.ui.tiles

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.RelativeLayout

import com.nononsenseapps.filepicker.FilePickerActivity

import java.util.Observable
import java.util.Observer

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

class StorageTile(context: Context) : QuickTile(context) {

    private val o = Observer { o, arg ->
        if (arg === SettingsProvider.Key.VIDEO_ROOT_PATH)
            tileView!!.summaryTextView!!.text = context.getString(R.string.summary_storage,
                    SettingsProvider.get()!!.getString(SettingsProvider.Key.VIDEO_ROOT_PATH))
    }

    init {
        this.titleRes = R.string.title_storage
        this.summary = context.getString(R.string.summary_storage,
                SettingsProvider.get()!!.getString(SettingsProvider.Key.VIDEO_ROOT_PATH))
        this.iconRes = R.drawable.ic_folder_open_black_24dp
        this.tileView = object : QuickTileView(context, this@StorageTile) {

            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                SettingsProvider.get()!!.addObserver(o)
            }

            override fun onClick(v: View) {
                super.onClick(v)
                pickSingleDir(context as Activity, SettingsProvider.REQUEST_CODE_FILE_PICKER)
            }

            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
                SettingsProvider.get()!!.deleteObserver(o)
            }
        }
    }

    private fun pickSingleDir(activity: Activity, code: Int) {
        // This always works
        val i = Intent(activity, FilePickerActivity::class.java)
        // This works if you defined the intent filter
        // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

        // Set these depending on your use case. These are the defaults.
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR)

        // Configure initial directory by specifying a String.
        // You could specify a String like "/storage/emulated/0/", but that can
        // dangerous. Always use Android's API calls to get paths to the SD-card or
        // internal memory.
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().path)

        activity.startActivityForResult(i, code)
    }
}
