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

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import androidx.appcompat.app.AppCompatActivity
import dev.tornaco.torscreenrec.TorScreenRecApp
import java.util.*

/**
 * Android OS version utilities
 */
object OsUtil {
    /**
     * @return True if the version of Android that we're running on is at least Ice Cream Sandwich
     * MR1 (API level 15).
     */
    var isAtLeastICS_MR1: Boolean = false
        private set
    /**
     * @return True if the version of Android that we're running on is at least Jelly Bean
     * (API level 16).
     */
    var isAtLeastJB: Boolean = false
        private set
    /**
     * @return True if the version of Android that we're running on is at least Jelly Bean MR1
     * (API level 17).
     */
    var isAtLeastJB_MR1: Boolean = false
        private set
    /**
     * @return True if the version of Android that we're running on is at least Jelly Bean MR2
     * (API level 18).
     */
    var isAtLeastJB_MR2: Boolean = false
        private set
    /**
     * @return True if the version of Android that we're running on is at least KLP
     * (API level 19).
     */
    var isAtLeastKLP: Boolean = false
        private set
    /**
     * @return True if the version of Android that we're running on is at least L
     * (API level 21).
     */
    var isAtLeastL: Boolean = false
        private set
    /**
     * @return True if the version of Android that we're running on is at least L MR1
     * (API level 22).
     */
    var isAtLeastL_MR1: Boolean = false
        private set
    /**
     * @return True if the version of Android that we're running on is at least M
     * (API level 23).
     */
    var isAtLeastM: Boolean = false
        private set

    private var sIsSecondaryUser: Boolean? = null
    private val sPermissions = Hashtable<String, Int>()
    private val sRequiredPermissions = arrayOf(
            // Required to read existing SMS threads
            Manifest.permission.READ_SMS,
            // Required for knowing the phone number, number of SIMs, etc.
            Manifest.permission.READ_PHONE_STATE,
            // This is not strictly required, but simplifies the contact picker scenarios
            Manifest.permission.READ_CONTACTS)

    /**
     * @return The Android API version of the OS that we're currently running on.
     */
    val apiVersion: Int
        get() = Build.VERSION.SDK_INT

    // Only check for newer devices (but not the nexus 10)
    val isSecondaryUser: Boolean
        get() {
            if (sIsSecondaryUser == null) {
                val context = TorScreenRecApp.app!!.getApplicationContext()
                var isSecondaryUser = false
                if (OsUtil.isAtLeastJB_MR1 && "Nexus 10" != Build.MODEL) {
                    val uh = android.os.Process.myUserHandle()
                    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
                    if (userManager != null) {
                        val userSerialNumber = userManager.getSerialNumberForUser(uh)
                        isSecondaryUser = 0L != userSerialNumber
                    }
                }
                sIsSecondaryUser = isSecondaryUser
            }
            return sIsSecondaryUser!!
        }

    val missingRequiredPermissions: Array<String>
        get() = getMissingPermissions(sRequiredPermissions)

    init {
        val v = apiVersion
        isAtLeastICS_MR1 = v >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
        isAtLeastJB = v >= Build.VERSION_CODES.JELLY_BEAN
        isAtLeastJB_MR1 = v >= Build.VERSION_CODES.JELLY_BEAN_MR1
        isAtLeastJB_MR2 = v >= Build.VERSION_CODES.JELLY_BEAN_MR2
        isAtLeastKLP = v >= Build.VERSION_CODES.KITKAT
        isAtLeastL = v >= Build.VERSION_CODES.LOLLIPOP
        isAtLeastL_MR1 = v >= Build.VERSION_CODES.LOLLIPOP_MR1
        isAtLeastM = v >= Build.VERSION_CODES.M
    }

    /**
     * Creates a joined string from a Set<String> using the given delimiter.
     *
     * @param values
     * @param delimiter
     * @return
    </String> */
    fun joinFromSetWithDelimiter(
            values: Set<String>?, delimiter: String): String? {
        if (values != null) {
            val joinedStringBuilder = StringBuilder()
            var firstValue = true
            for (value in values) {
                if (firstValue) {
                    firstValue = false
                } else {
                    joinedStringBuilder.append(delimiter)
                }
                joinedStringBuilder.append(value)
            }
            return joinedStringBuilder.toString()
        }
        return null
    }

    /**
     * Check if the app has the specified permission. If it does not, the app needs to use
     * [AppCompatActivity.requestPermissions]. Note that if it
     * returns true, it cannot return false in the same process as the OS kills the process when
     * any permission is revoked.
     *
     * @param permission A permission from [Manifest.permission]
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun hasPermission(permission: String): Boolean {
        if (OsUtil.isAtLeastM) {
            // It is safe to cache the PERMISSION_GRANTED result as the process gets killed if the
            // user revokes the permission setting. However, PERMISSION_DENIED should not be
            // cached as the process does not get killed if the user enables the permission setting.
            if (!sPermissions.containsKey(permission) || sPermissions[permission] == PackageManager.PERMISSION_DENIED) {
                val context = TorScreenRecApp.app!!.getApplicationContext()
                val permissionState = context.checkSelfPermission(permission)
                sPermissions[permission] = permissionState
            }
            return sPermissions[permission] == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }

    /**
     * Does the app have all the specified permissions
     */
    fun hasPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (!hasPermission(permission)) {
                return false
            }
        }
        return true
    }

    fun hasPhonePermission(): Boolean {
        return hasPermission(Manifest.permission.READ_PHONE_STATE)
    }

    fun hasSmsPermission(): Boolean {
        return hasPermission(Manifest.permission.READ_SMS)
    }

    fun hasLocationPermission(): Boolean {
        return OsUtil.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun hasStoragePermission(): Boolean {
        // Note that READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE are granted or denied
        // together.
        return OsUtil.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasRecordAudioPermission(): Boolean {
        return OsUtil.hasPermission(Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Returns array with the set of permissions that have not been granted from the given set.
     * The array will be empty if the app has all of the specified permissions. Note that calling
     * [AppCompatActivity.requestPermissions] for an already granted permission can prompt the user
     * again, and its up to the app to only request permissions that are missing.
     */
    fun getMissingPermissions(permissions: Array<String>): Array<String> {
        val missingList = ArrayList<String>()
        for (permission in permissions) {
            if (!hasPermission(permission)) {
                missingList.add(permission)
            }
        }
        return missingList.toTypedArray()
    }

    /**
     * Does the app have the minimum set of permissions required to operate.
     */
    fun hasRequiredPermissions(): Boolean {
        return hasPermissions(sRequiredPermissions)
    }
}
