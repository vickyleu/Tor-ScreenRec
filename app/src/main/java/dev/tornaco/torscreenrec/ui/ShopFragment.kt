package dev.tornaco.torscreenrec.ui

import android.os.Bundle

import dev.nick.tiles.tile.Category
import dev.nick.tiles.tile.DashboardFragment
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.ui.tiles.PayListTile
import dev.tornaco.torscreenrec.ui.tiles.PayStatusTile
import dev.tornaco.torscreenrec.ui.tiles.ShopAliPayCodeTile
import dev.tornaco.torscreenrec.ui.tiles.ShopIntroTile
import dev.tornaco.torscreenrec.ui.tiles.ShopWechatCodeTile

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class ShopFragment : DashboardFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity!!.setTitle(R.string.title_buy)
    }

    override fun onCreateDashCategories(categories: MutableList<Category>) {
        super.onCreateDashCategories(categories)

        val about = Category()
        about.addTile(ShopIntroTile(requireContext()))

        val payment = Category()
        payment.titleRes = R.string.title_pay_ment_type
        payment.addTile(ShopAliPayCodeTile(requireActivity()))
        payment.addTile(ShopWechatCodeTile(requireActivity()))

        val thanks = Category()
        thanks.titleRes = R.string.title_thanks
        thanks.addTile(PayListTile(requireActivity()))
        thanks.addTile(PayStatusTile(requireActivity()))

        categories.add(about)
        categories.add(payment)
        categories.add(thanks)
    }
}
