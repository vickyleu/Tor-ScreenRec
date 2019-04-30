package dev.nick.tiles.tile

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import android.widget.Spinner

import java.util.ArrayList

import dev.nick.tiles.R

open class DropDownTileView : TileView {

    internal var mSpinner: Spinner?=null
    internal var mSelectedPosition = -1

    protected val initialSelection: Int
        get() = 0

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    private fun initDropdown() {
        mSpinner = Spinner(context)
        mSpinner?.visibility = View.INVISIBLE
        mSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) {
                setSelectedItem(position, true)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // noop
            }
        }

    }

    fun setSelectedItem(position: Int, fromSpinner: Boolean) {
        if (fromSpinner && position == mSelectedPosition) {
            return
        }
        mSpinner?.setSelection(position)
        mSelectedPosition = mSpinner?.selectedItemPosition?:0
        onItemSelected(position)
    }

    protected open fun onItemSelected(position: Int) {
        // Noop
    }

    override fun onBindActionView(container: RelativeLayout) {
        initDropdown()

        val adapter = ArrayAdapter(context,
                android.R.layout.simple_spinner_item, onCreateDropDownList())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mSpinner?.adapter = adapter
        mSpinner?.setSelection(initialSelection)
        val dropDownWidth = resources.getDimensionPixelSize(R.dimen.drop_down_width)
        container.addView(mSpinner, generateCenterParams(dropDownWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    protected open fun onCreateDropDownList(): List<String> {
        val list = ArrayList<String>()
        list.add("Android")
        list.add("Blackberry")
        list.add("Cherry")
        list.add("Duck")
        list.add("Female")
        return list
    }

    override fun onClick(v: View) {
        super.onClick(v)
        mSpinner?.performClick()
    }
}
