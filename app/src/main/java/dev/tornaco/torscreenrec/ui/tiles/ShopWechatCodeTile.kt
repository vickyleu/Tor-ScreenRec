package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.view.View

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.R

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class ShopWechatCodeTile(context: Context) : QuickTile(context) {

    init {

        this.iconRes = R.drawable.ic_wechat
        this.titleRes = R.string.title_wechat
        this.tileView = object : QuickTileView(context, this@ShopWechatCodeTile) {
            override fun onClick(v: View) {
                super.onClick(v)
                PaymentDialog.show(context, R.string.title_wechat, R.drawable.qr_wechat)
            }

            override fun useStaticTintColor(): Boolean {
                return false
            }
        }
    }
}
