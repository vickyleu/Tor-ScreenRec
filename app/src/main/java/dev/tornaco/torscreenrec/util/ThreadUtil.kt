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

package dev.tornaco.torscreenrec.util

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

object ThreadUtil {
    val mainThreadHandler = Handler(Looper.getMainLooper())
    private var sWorkThreadHandler: Handler? = null

    val workThreadHandler: Handler
        @Synchronized get() {
            if (sWorkThreadHandler == null) {
                val ht = HandlerThread("worker.thread.handler")
                ht.start()
                sWorkThreadHandler = Handler(ht.looper)
            }
            return sWorkThreadHandler!!
        }

    fun newThread(runnable: Runnable): Thread {
        val t = Thread(runnable, "screencast.thread")
        t.isDaemon = false
        t.priority = Thread.NORM_PRIORITY
        return t
    }

    fun sleep(timeMills: Long) {
        try {
            Thread.sleep(timeMills)
        } catch (ignored: InterruptedException) {

        }

    }
}
