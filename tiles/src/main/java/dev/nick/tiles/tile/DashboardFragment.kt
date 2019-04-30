package dev.nick.tiles.tile

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import java.util.ArrayList

import dev.nick.tiles.R

open class DashboardFragment : Fragment() {

    private var mLayoutInflater: LayoutInflater? = null
    private var mDashboard: ViewGroup? = null


    protected open val layoutId: Int
        get() = R.layout.dashboard

    private val dashboardCategories: MutableList<Category>
        get() {
            val categories = ArrayList<Category>()
            onCreateDashCategories(categories)
            return categories
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity!!.runOnUiThread { buildUI(activity) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mLayoutInflater = inflater

        val rootView = inflater.inflate(layoutId, container, false)
        mDashboard = rootView.findViewById<View>(R.id.dashboard_container) as ViewGroup

        return rootView
    }

    protected open fun onCreateDashCategories(categories: MutableList<Category>) {
        // Need an impl.
    }

    protected fun buildUI(context: Context?) {
        if (!isAdded) {
            throw IllegalStateException("Fragment not added yet.")
        }

        val start = System.currentTimeMillis()
        val res = resources

        mDashboard!!.removeAllViews()

        val categories = dashboardCategories

        val count = categories.size

        for (n in 0 until count) {
            val category = categories[n]

            val categoryView = mLayoutInflater!!.inflate(R.layout.dashboard_category, mDashboard,
                    false)

            val categoryLabel = categoryView.findViewById<View>(R.id.category_title) as TextView
            if (category.getTitle(res) != null)
                categoryLabel.text = category.getTitle(res)
            else {
                categoryLabel.visibility = View.GONE
            }

            val categorySummary = categoryView.findViewById<View>(R.id.category_summary) as TextView

            if (category.getSummary(res) != null) {
                categorySummary.text = category.getSummary(res)
                category.onSummarySet(categorySummary)

            } else {
                categorySummary.visibility = View.GONE
            }

            val categoryContent = categoryView.findViewById<View>(R.id.category_content) as ViewGroup

            val tilesCount = category.tilesCount
            for (i in 0 until tilesCount) {
                val tile = category.getTile(i)
                val tileView = tile.tileView
                updateTileView(context, res, tile, tileView!!.imageView,
                        tileView.titleTextView, tileView.summaryTextView)

                categoryContent.addView(tileView)
            }

            // Add the category
            mDashboard!!.addView(categoryView)
        }
        val delta = System.currentTimeMillis() - start
        onUIBuilt()
    }

    protected fun onUIBuilt() {
        // None
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun updateTileView(context: Context?,
                               res: Resources, tile: Tile,
                               tileIcon: ImageView?, tileTextView: TextView?, statusTextView: TextView?) {

        if (tileIcon != null) {
            if (tile.iconRes > 0) {
                tileIcon.setImageResource(tile.iconRes)
            } else if (tile.iconDrawable != null) {
                tileIcon.setImageDrawable(tile.iconDrawable)
            } else {
                tileIcon.setImageDrawable(null)
                tileIcon.background = null
            }
        }

        if (tileTextView != null) {
            tileTextView.text = tile.getTitle(res)
        }

        if (statusTextView != null) {
            val summary = tile.getSummary(res)
            if (!TextUtils.isEmpty(summary)) {
                statusTextView.visibility = View.VISIBLE
                statusTextView.text = summary
            } else {
                statusTextView.visibility = View.GONE
            }
        }
    }

}
