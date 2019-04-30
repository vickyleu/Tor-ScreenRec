package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.widget.RelativeLayout

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.SwitchTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.camera.CameraPreviewServiceProxy
import dev.tornaco.torscreenrec.pref.SettingsProvider

class WithCameraTile(context: Context) : QuickTile(context) {

    init {
        this.iconRes = R.drawable.ic_camera_alt_black_24dp
        this.tileView = object : SwitchTileView(context) {

            override fun onBindActionView(container: RelativeLayout) {
                super.onBindActionView(container)
                isChecked = SettingsProvider.get()!!.getBoolean(SettingsProvider.Key.CAMERA)
            }

            override fun onCheckChanged(checked: Boolean) {
                super.onCheckChanged(checked)
                SettingsProvider.get()!!.putBoolean(SettingsProvider.Key.CAMERA, checked)
                if (checked)
                    CameraPreviewServiceProxy.show(getContext(), SettingsProvider.get()!!.getInt(SettingsProvider.Key.CAMERA_SIZE))
                else
                    CameraPreviewServiceProxy.hide(getContext())
            }
        }
        this.titleRes = R.string.title_with_camera
    }
}
