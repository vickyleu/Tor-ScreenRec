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

import android.content.Context
import android.content.Intent
import android.os.Debug
import android.os.PowerManager
import android.os.Process
import android.util.Log

/**
 * Helper class used to manage wakelock state
 */
class WakeLockHelper(private val mWakeLockId: String) {

    private val mLock = Any()
    private val mMyPid: Int

    private var mWakeLock: PowerManager.WakeLock? = null

    init {
        mMyPid = Process.myPid()
    }

    /**
     * Acquire the wakelock
     */
    fun acquire(context: Context, intent: Intent, opcode: Int) {
        synchronized(mLock) {
            if (mWakeLock == null) {
                if (VERBOSE) {
                    Log.v(TAG, "initializing wakelock")
                }
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, mWakeLockId)
            }
        }
        if (VERBOSE) {
            Log.v(TAG, "acquiring $mWakeLockId for opcode $opcode")
        }
        mWakeLock!!.acquire()
        intent.putExtra(EXTRA_CALLING_PID, mMyPid)
    }

    /**
     * Check if wakelock held by this process
     */
    fun isHeld(intent: Intent): Boolean {
        val respectWakeLock = mMyPid == intent.getIntExtra(EXTRA_CALLING_PID, -1)
        return respectWakeLock && mWakeLock!!.isHeld
    }

    /**
     * Ensure that wakelock is held by this process
     */
    fun ensure(intent: Intent, opcode: Int): Boolean {
        val respectWakeLock = mMyPid == intent.getIntExtra(EXTRA_CALLING_PID, -1)
        if (VERBOSE) {
            Log.v(TAG, "WakeLockHelper.ensure Intent " + intent + " "
                    + intent.action + " opcode: " + opcode
                    + " respectWakeLock " + respectWakeLock)
        }

        if (respectWakeLock) {
            val isHeld = respectWakeLock && isHeld(intent)
            if (!isHeld) {
                Log.e(TAG, "WakeLockHelper.ensure called " + intent + " " + intent.action
                        + " opcode: " + opcode + " sWakeLock: " + mWakeLock + " isHeld: "
                        + if (mWakeLock == null) "(null)" else mWakeLock!!.isHeld)
                if (!Debug.isDebuggerConnected()) {
                }
            }
            return true
        }
        return false
    }

    /**
     * Release wakelock (if it is held by this process)
     */
    fun release(intent: Intent, opcode: Int) {
        val respectWakeLock = mMyPid == intent.getIntExtra(EXTRA_CALLING_PID, -1)
        if (respectWakeLock) {
            try {
                mWakeLock!!.release()
            } catch (ex: RuntimeException) {
                Log.e(TAG, "KeepAliveService.onHandleIntent exit crash " + intent + " "
                        + intent.action + " opcode: " + opcode + " sWakeLock: " + mWakeLock
                        + " isHeld: " + if (mWakeLock == null) "(null)" else mWakeLock!!.isHeld)
                if (!Debug.isDebuggerConnected()) {
                }
            }

        }
    }

    companion object {
        private val TAG = "WakeLockHelper"
        private val VERBOSE = false

        val EXTRA_CALLING_PID = "pid"
    }
}
