package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.view.View

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.camera.CameraManager
import dev.tornaco.torscreenrec.util.ThreadUtil

class SwitchCameraTile(context: Context) : QuickTile(context) {

    init {
        this.titleRes = R.string.title_switch_camera
        this.iconRes = R.drawable.ic_camera_front_black_24dp
        this.tileView = object : QuickTileView(context, this@SwitchCameraTile) {
            override fun onClick(v: View) {
                ThreadUtil.mainThreadHandler.post(Runnable { CameraManager.get().swapCamera() })
            }
        }
    }
}
