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

class ShopAliPayCodeTile(context: Context) : QuickTile(context) {

    init {

        this.iconRes = R.drawable.ic_alipay
        this.titleRes = R.string.title_alipay
        this.tileView = object : QuickTileView(context, this@ShopAliPayCodeTile) {
            override fun onClick(v: View) {
                super.onClick(v)
                PaymentDialog.show(context, R.string.title_alipay, R.drawable.qr_alipay)
            }

            override fun useStaticTintColor(): Boolean {
                return false
            }
        }


    }
}
