/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.tornaco.torscreenrec.camera

import android.content.Intent
import android.os.AsyncTask
import android.os.Debug
import android.os.SystemClock
import android.util.Log

import dev.tornaco.torscreenrec.TorScreenRecApp
import dev.tornaco.torscreenrec.util.ThreadUtil


/**
 * Wrapper class which provides explicit API for:
 *
 *  1. Threading policy choice - Users of this class should use the explicit API instead of
 * [.execute] which uses different threading policy on different OS versions.
 *  1. Enforce creation on main thread as required by AsyncTask
 *  1. Enforce that the background task does not take longer than expected.
 *
 */
abstract class SafeAsyncTask<Params, Progress, Result>
/**
 * @param maxTimeMillis            maximum expected time for the background operation. This is just
 * a diagnostic tool to catch unexpectedly long operations. If an operation does take
 * longer than expected, it is fine to increase this argument. If the value is larger
 * than a minute, you should consider using a dedicated thread so as not to interfere
 * with other AsyncTasks.
 *
 *
 *
 * Use [.UNBOUNDED_TIME] if you do not know the maximum expected time. This
 * is strongly discouraged as it can block other AsyncTasks indefinitely.
 * @param cancelExecutionOnTimeout whether to attempt to cancel the task execution on timeout.
 * If this is set, at execution timeout we will call cancel(), so doInBackgroundTimed()
 * should periodically check if the task is to be cancelled and finish promptly if
 * possible, and handle the cancel event in onCancelled(). Also, at the end of execution
 * we will not crash the execution if it went over limit since we explicitly canceled it.
 */
@JvmOverloads constructor(private val mMaxExecutionTimeMillis: Long = DEFAULT_MAX_EXECUTION_TIME_MILLIS, private val mCancelExecutionOnTimeout: Boolean = false) : AsyncTask<Params, Progress, Result>() {
    private var mThreadPoolRequested: Boolean = false

    fun executeOnThreadPool(
            vararg params: Params): SafeAsyncTask<Params, Progress, Result> {
        mThreadPoolRequested = true
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, *params)
        return this
    }

    protected abstract fun doInBackgroundTimed(vararg params: Params): Result

    override fun doInBackground(vararg params: Params): Result {
        // This enforces that executeOnThreadPool was called, not execute. Ideally, we would
        // make execute throw an exception, but since it is final, we cannot override it.

        if (mCancelExecutionOnTimeout) {
            ThreadUtil.mainThreadHandler.postDelayed(object : Runnable {
                override fun run() {
                    if (status == AsyncTask.Status.RUNNING) {
                        // Cancel the task if it's still running.
                        Log.w(TAG, String.format("%s timed out and is canceled",
                                this))
                        cancel(true /* mayInterruptIfRunning */)
                    }
                }
            }, mMaxExecutionTimeMillis)
        }

        val startTime = SystemClock.elapsedRealtime()
        try {
            return doInBackgroundTimed(*params)
        } finally {
            val executionTime = SystemClock.elapsedRealtime() - startTime
            if (executionTime > mMaxExecutionTimeMillis) {
                Log.w(TAG, String.format("%s took %dms", this, executionTime))
                // Don'data crash if debugger is attached or if we are asked to cancel on timeout.
                if (!Debug.isDebuggerConnected() && !mCancelExecutionOnTimeout) {
                }
            }
        }

    }

    override fun onPostExecute(result: Result) {
        // No need to use AsyncTask at all if there is no onPostExecute
    }

    companion object {

        /**
         * This is strongly discouraged as it can block other AsyncTasks indefinitely.
         */
        val UNBOUNDED_TIME = java.lang.Long.MAX_VALUE
        protected val WAKELOCK_OP = 1000
        internal val TAG = "SafeAsyncTask"
        private val DEFAULT_MAX_EXECUTION_TIME_MILLIS = (10 * 1000).toLong() // 10 seconds
        private val WAKELOCK_ID = "bugle_safe_async_task_wakelock"
        private val sWakeLock = WakeLockHelper(WAKELOCK_ID)

        /**
         * This provides a way for people to run async tasks but without onPostExecute.
         * This can be called on any thread.
         *
         *
         * Run code in a thread using AsyncTask's thread pool.
         *
         * @param runnable     The Runnable to execute asynchronously
         * @param withWakeLock when set, a wake lock will be held for the duration of the runnable
         * execution
         */
        @JvmOverloads
        fun executeOnThreadPool(runnable: Runnable, withWakeLock: Boolean = false) {
            if (withWakeLock) {
                val intent = Intent()
                sWakeLock.acquire(TorScreenRecApp.app!!.getApplicationContext(), intent, WAKELOCK_OP)
                AsyncTask.THREAD_POOL_EXECUTOR.execute {
                    try {
                        runnable.run()
                    } finally {
                        sWakeLock.release(intent, WAKELOCK_OP)
                    }
                }
            } else {
                AsyncTask.THREAD_POOL_EXECUTOR.execute(runnable)
            }
        }
    }
}
/**
 * This provides a way for people to run async tasks but without onPostExecute.
 * This can be called on any thread.
 *
 *
 * Run code in a thread using AsyncTask's thread pool.
 *
 *
 * To enable wakelock during the execution, see [.executeOnThreadPool]
 *
 * @param runnable The Runnable to execute asynchronously
 */
