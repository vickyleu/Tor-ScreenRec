package dev.nick.tiles.tile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.DatePicker
import android.widget.TimePicker

import java.util.Calendar

class TimeTileView : TileView {

    private var mDatePickerDialog: DatePickerDialog? = null
    private var mTimePickerDialog: TimePickerDialog? = null
    private var listener: OnDateSetListener? = null
    private var time: Long=0

    constructor(context: Context, time: Long) : super(context) {
        this.time = time
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    fun setListener(listener: OnDateSetListener) {
        this.listener = listener
    }

    override fun onCreate(context: Context) {
        super.onCreate(context)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time

        mDatePickerDialog = DatePickerDialog(context, DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth -> mTimePickerDialog!!.show() }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE))

        mTimePickerDialog = TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
            listener!!.onDateSet(mDatePickerDialog!!.datePicker,
                    mDatePickerDialog!!.datePicker.year,
                    mDatePickerDialog!!.datePicker.month,
                    mDatePickerDialog!!.datePicker.dayOfMonth,
                    hourOfDay, minute)
        }, calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE), true)
    }


    override fun onClick(v: View) {
        super.onClick(v)
        mDatePickerDialog!!.show()
    }

    interface OnDateSetListener {
        /**
         * @param view       the picker associated with the dialog
         * @param year       the selected year
         * @param month      the selected month (0-11 for compatibility with
         * [Calendar.MONTH])
         * @param dayOfMonth th selected day of the month (1-31, depending on
         * month)
         */
        fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int, hourOfDay: Int, minute: Int)
    }
}
