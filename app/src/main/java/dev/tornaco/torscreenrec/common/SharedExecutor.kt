package dev.tornaco.torscreenrec.common

import android.os.Handler
import android.os.Looper

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by Nick@NewStand.org on 2017/3/7 12:15
 * E-Mail: NewStand@163.com
 * All right reserved.
 */

object SharedExecutor {

    val service = Executors.newCachedThreadPool()

    private val mUIThreadHandler = Handler(Looper.getMainLooper())

    fun execute(runnable: Runnable) {
        service.execute(runnable)
    }

    fun runOnUIThread(runnable: Runnable) {
        mUIThreadHandler.post(runnable)
    }

    fun runOnUIThreadDelayed(runnable: Runnable, delayMills: Long) {
        mUIThreadHandler.postDelayed(runnable, delayMills)
    }
}
