package dev.nick.tiles.tile

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.CheckBox
import android.widget.Checkable
import android.widget.RelativeLayout

class CheckboxTileView : TileView, Checkable {

    protected var mBox: CheckBox? = null

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun onBindActionView(container: RelativeLayout) {
        val s = CheckBox(context)
        s.isSoundEffectsEnabled = false
        container.addView(s, generateCenterParams())
        s.setOnClickListener {
            val checked = s.isChecked
            onCheckChanged(checked)
        }
        mBox = s
    }

    protected fun onCheckChanged(checked: Boolean) {
        // Empty.
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val myState = SavedState(superState)
        myState.checked = isChecked
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as SavedState?
        super.onRestoreInstanceState(myState!!.superState)
        isChecked = myState.checked
    }

    override fun onClick(v: View) {
        super.onClick(v)
        mBox!!.performClick()
    }

    override fun isChecked(): Boolean {
        ensureSwitch()
        return mBox!!.isChecked
    }

    override fun setChecked(checked: Boolean) {
        ensureSwitch()
        mBox!!.isChecked = checked
    }

    override fun toggle() {
        ensureSwitch()
        mBox!!.isChecked = !mBox!!.isChecked
    }

    private fun ensureSwitch() {
        if (mBox == null) {
            throw IllegalStateException("View not finished inflate yet.")
        }
    }

    internal class SavedState : View.BaseSavedState {
        var checked: Boolean = false

        constructor(source: Parcel) : super(source) {
            checked = source.readInt() == 1
        }

        constructor(superState: Parcelable) : super(superState) {}

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(if (checked) 1 else 0)
        }

//        companion object {
//            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
//                override fun createFromParcel(`in`: Parcel): SavedState {
//                    return SavedState(`in`)
//                }
//
//                override fun newArray(size: Int): Array<SavedState> {
//                    return arrayOfNulls(size)
//                }
//            }
//        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}
