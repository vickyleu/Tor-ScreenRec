package dev.nick.tiles.tile

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import dev.nick.tiles.R

open class EditTextTileView : TileView {

    protected var editText: EditText? = null
        protected set
    internal var mAlertDialog: AlertDialog? = null

    protected open val inputType: Int
        get() = InputType.TYPE_CLASS_TEXT

    protected open val hint: CharSequence?
        get() = null

    protected open val dialogTitle: CharSequence
        get() = "Edit tile"

    protected val positiveButton: CharSequence
        get() = "SAVE"

    protected val negativeButton: CharSequence
        get() = "DISCARD"

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun onCreate(context: Context) {
        super.onCreate(context)
        val editTextContainer = LayoutInflater.from(context).inflate(R.layout.dialog_edit_text, null, false)
        editText = editTextContainer.findViewById<View>(R.id.edit_text) as EditText
        editText?.hint = hint
        editText?.inputType = inputType
        mAlertDialog = AlertDialog.Builder(context)
                .setView(editTextContainer)
                .setTitle(dialogTitle)
                .setPositiveButton(positiveButton) { dialog, which -> onPositiveButtonClick() }
                .setNegativeButton(negativeButton) { dialog, which -> onNegativeButtonClick() }
                .create()
    }

    protected open fun onPositiveButtonClick() {
        // Nothing.
    }

    protected fun onNegativeButtonClick() {
        // Nothing.
    }

    override fun onClick(v: View) {
        super.onClick(v)
        mAlertDialog?.show()
    }
}
