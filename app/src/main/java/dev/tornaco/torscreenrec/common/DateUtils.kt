package dev.tornaco.torscreenrec.common

import java.sql.Date
import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * Created by Nick@NewStand.org on 2017/3/26 17:00
 * E-Mail: NewStand@163.com
 * All right reserved.
 */

object DateUtils {

    fun formatLong(l: Long): String {
        val time: String
        val format = DateFormat.getDateInstance(DateFormat.FULL)
        val d1 = Date(l)
        time = format.format(d1)
        val timeInstance = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)
        return time + "\t" + timeInstance.format(d1)
    }

    fun formatForFileName(l: Long): String {
        val time: String
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val format = SimpleDateFormat("YYYY-MM-dd-HH-mm-ss")
            val d1 = Date(l)
            time = format.format(d1)
        } else {
            // I have no time to fix YYYY issues.
            // So, removed.
            val format = SimpleDateFormat("MM-dd-HH-mm-ss")
            val d1 = Date(l)
            time = format.format(d1)
        }
        return time
    }
}
