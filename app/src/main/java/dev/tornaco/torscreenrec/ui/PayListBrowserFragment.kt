package dev.tornaco.torscreenrec.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog
import com.tbruyelle.rxpermissions2.RxPermissions

import java.util.ArrayList

import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.loader.PayExtraLoader
import dev.tornaco.torscreenrec.modle.PayExtra
import io.reactivex.functions.Consumer

/**
 * Created by Tornaco on 2017/7/29.
 * Licensed with Apache.
 */

class PayListBrowserFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var adapter: Adapter? = null

    protected val layoutManager: RecyclerView.LayoutManager
        get() = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.recycler_view_template, container, false)
        setupView(root)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity!!.setTitle(R.string.title_pay_list)
        showRetention()
    }

    fun setupView(root: View) {

        swipeRefreshLayout = root.findViewById(R.id.swipe)
        swipeRefreshLayout!!.setColorSchemeColors(*resources.getIntArray(R.array.polluted_waves))

        recyclerView = root.findViewById(R.id.recycler_view)

        swipeRefreshLayout!!.setOnRefreshListener { startLoading() }

        setupAdapter()
    }

    private fun startLoading() {
        swipeRefreshLayout!!.isRefreshing = true
        PayExtraLoader().loadAsync(getString(R.string.pay_list_url),
                object : PayExtraLoader.Callback {
                    override fun onError(e: Throwable?) {
                        if (activity == null) return
                        activity!!.runOnUiThread { swipeRefreshLayout!!.isRefreshing = false }
                    }

                    override fun onSuccess(extras: List<PayExtra>) {
                        if (activity == null) return
                        activity!!.runOnUiThread {
                            adapter!!.update(extras)
                            swipeRefreshLayout!!.isRefreshing = false
                        }
                    }
                })
    }

    private fun requestPerms() {
        val rxPermissions = RxPermissions(activity!!)
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe { granted ->
                    if (granted!!) {
                        onPermissionGrant()
                    } else {
                        onPermissionNotGrant()
                    }
                }.toString()
    }

    private fun onPermissionNotGrant() {
        activity!!.finish()
    }

    private fun onPermissionGrant() {
        startLoading()
    }

    private fun showRetention() {
        val hasBasicPermission = ContextCompat.checkSelfPermission(context!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (!hasBasicPermission) {
            MaterialStyledDialog.Builder(activity!!)
                    .setTitle(R.string.title_perm_require)
                    .setDescription(R.string.summary_perm_require)
                    .setIcon(R.drawable.ic_folder_white_24dp)
                    .withDarkerOverlay(false)
                    .setCancelable(false)
                    .setPositiveText(android.R.string.ok)
                    .setNegativeText(android.R.string.cancel)
                    .onPositive { dialog, which -> requestPerms() }
                    .onNegative { dialog, which -> activity!!.finish() }
                    .show()
        } else {
            onPermissionGrant()
        }
    }

    protected fun setupAdapter() {
        recyclerView!!.setHasFixedSize(true)
        setupLayoutManager()
        adapter = Adapter()
        recyclerView!!.adapter = adapter

    }

    protected fun setupLayoutManager() {
        recyclerView!!.layoutManager = layoutManager
    }


    internal inner class TwoLinesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var title: TextView
        var description: TextView
        var thumbnail: ImageView

        init {
            title = itemView.findViewById(android.R.id.title)
            description = itemView.findViewById(android.R.id.text1)
            thumbnail = itemView.findViewById(R.id.avatar)
            thumbnail.setImageResource(R.drawable.ic_header_avatar)
        }
    }


    private inner class Adapter @JvmOverloads constructor(private val data: MutableList<PayExtra> = ArrayList()) : RecyclerView.Adapter<TwoLinesViewHolder>() {

        fun update(data: List<PayExtra>) {
            this.data.clear()
            this.data.addAll(data)
            notifyDataSetChanged()
        }

        fun remove(position: Int) {
            this.data.removeAt(position)
            notifyItemRemoved(position)
        }

        fun add(PayExtra: PayExtra, position: Int) {
            this.data.add(position, PayExtra)
            notifyItemInserted(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TwoLinesViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.simple_card_item, parent, false)
            return TwoLinesViewHolder(view)
        }

        override fun onBindViewHolder(holder: TwoLinesViewHolder, position: Int) {
            val item = data[position]
            holder.title.text = item.nick
            val descriptionText = item.ad
            holder.description.text = descriptionText
        }

        override fun getItemCount(): Int {
            return data.size
        }

    }
}
