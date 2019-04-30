package dev.tornaco.torscreenrec.ui.tiles

import android.app.ProgressDialog
import android.content.Context
import com.google.android.material.snackbar.Snackbar
import android.view.View

import com.stericson.rootools.RootTools

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.bridge.Installer

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class UnInstallTile(context: Context) : QuickTile(context) {

    init {

        this.titleRes = R.string.title_uninstall
        this.iconRes = R.drawable.ic_remove_circle_black_24dp

        this.tileView = object : QuickTileView(context, this@UnInstallTile) {
            override fun onClick(v: View) {
                super.onClick(v)
                val p = ProgressDialog(context)
                p.setMessage(context.getString(R.string.uninstalling))
                p.setCancelable(false)
                p.show()

                Installer.unInstallAsync(getContext(), object : Installer.Callback {
                    override fun onSuccess() {
                        p.dismiss()
                        Snackbar.make(v, R.string.uninstall_success, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.restart) { RootTools.restartAndroid() }.show()
                    }

                    override fun onFailure(throwable: Throwable, errTitle: String) {
                        p.dismiss()
                        Snackbar.make(v, context.getString(R.string.uninstall_fail, errTitle),
                                Snackbar.LENGTH_LONG)
                                .setAction(R.string.report) { }.show()
                    }
                })
            }
        }
    }
}
