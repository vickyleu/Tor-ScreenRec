/* 
 * This file is part of the RootTools Project: http://code.google.com/p/RootTools/
 *  
 * Copyright (c) 2012 Stephen Erickson, Chris Ravenscroft, Dominik Schuermann, Adam Shanks
 *  
 * This code is dual-licensed under the terms of the Apache License Version 2.0 and
 * the terms of the General Public License (GPL) Version 2.
 * You may use this code according to either of these licenses as is most appropriate
 * for your project on a case-by-case basis.
 * 
 * The terms of each license can be found in the root directory of this project's repository as well as at:
 * 
 * * http://www.apache.org/licenses/LICENSE-2.0
 * * http://www.gnu.org/licenses/gpl-2.0.txt
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under these Licenses is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See each License for the specific language governing permissions and
 * limitations under that License.
 */

package com.stericson.rootools

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.StrictMode
import android.widget.ScrollView
import android.widget.TextView
import com.stericson.rootshell.exceptions.RootDeniedException
import com.stericson.rootshell.execution.Command
import com.stericson.rootshell.execution.Shell
import java.io.IOException
import java.util.concurrent.TimeoutException

class SanityCheckRootTools : Activity() {
    private var mScrollView: ScrollView? = null
    private var mTextView: TextView? = null
    private var mPDialog: ProgressDialog? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build())

        RootTools.debugMode = true

        mTextView = TextView(this)
        mTextView!!.text = ""
        mScrollView = ScrollView(this)
        mScrollView!!.addView(mTextView)
        setContentView(mScrollView)

        print("SanityCheckRootTools \n\n")

        if (RootTools.isRootAvailable) {
            print("Root found.\n")
        } else {
            print("Root not found")
        }

        try {
            Shell.startRootShell()
        } catch (e2: IOException) {
            e2.printStackTrace()
        } catch (e: TimeoutException) {
            print("[ TIMEOUT EXCEPTION! ]\n")
            e.printStackTrace()
        } catch (e: RootDeniedException) {
            print("[ ROOT DENIED EXCEPTION! ]\n")
            e.printStackTrace()
        }

        try {
            if (!RootTools.isAccessGiven) {
                print("ERROR: No root access to this device.\n")
                return
            }
        } catch (e: Exception) {
            print("ERROR: could not determine root access to this device.\n")
            return
        }

        // Display infinite progress bar
        mPDialog = ProgressDialog(this)
        mPDialog!!.setCancelable(false)
        mPDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)

        SanityCheckThread(this, TestHandler()).start()
    }

    protected fun print(text: CharSequence?) {
        mTextView!!.append(text)
        mScrollView!!.post { mScrollView!!.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    // Run our long-running tests in their separate thread so as to
    // not interfere with proper rendering.
    private inner class SanityCheckThread(context: Context, private val mHandler: Handler) : Thread() {

        override fun run() {
            visualUpdate(ACTION_SHOW, null)

            // First test: Install a binary file for future use
            // if it wasn't already installed.
            /*
            visualUpdate(ACTION_PDISPLAY, "Installing binary if needed");
            if(false == RootTools.installBinary(mContext, R.raw.nes, "nes_binary")) {
                visualUpdate(ACTION_HIDE, "ERROR: Failed to install binary. Please see log file.");
                return;
            }
            */

            var result: Boolean

            visualUpdate(ACTION_PDISPLAY, "Testing getPath")
            visualUpdate(ACTION_DISPLAY, "[ getPath ]\n")

            try {
                val paths = RootTools.path

                for (path in paths) {
                    visualUpdate(ACTION_DISPLAY, path + " k\n\n")
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            visualUpdate(ACTION_PDISPLAY, "Testing A ton of commands")
            visualUpdate(ACTION_DISPLAY, "[ Ton of Commands ]\n")

            for (i in 0..99) {
                RootTools.exists("/system/xbin/busybox")
            }

            visualUpdate(ACTION_PDISPLAY, "Testing Find Binary")
            result = RootTools.isRootAvailable
            visualUpdate(ACTION_DISPLAY, "[ Checking Root ]\n")
            visualUpdate(ACTION_DISPLAY, "$result k\n\n")

            visualUpdate(ACTION_PDISPLAY, "Testing file exists")
            visualUpdate(ACTION_DISPLAY, "[ Checking Exists() ]\n")
            visualUpdate(ACTION_DISPLAY, RootTools.exists("/system/sbin/[").toString() + " k\n\n")

            visualUpdate(ACTION_PDISPLAY, "Testing Is Access Given")
            result = RootTools.isAccessGiven
            visualUpdate(ACTION_DISPLAY, "[ Checking for Access to Root ]\n")
            visualUpdate(ACTION_DISPLAY, "$result k\n\n")

            visualUpdate(ACTION_PDISPLAY, "Testing Remount")
            result = RootTools.remount("/system", "rw")
            visualUpdate(ACTION_DISPLAY, "[ Remounting System as RW ]\n")
            visualUpdate(ACTION_DISPLAY, "$result k\n\n")

            visualUpdate(ACTION_PDISPLAY, "Testing CheckUtil")
            visualUpdate(ACTION_DISPLAY, "[ Checking busybox is setup ]\n")
            visualUpdate(ACTION_DISPLAY, RootTools.checkUtil("busybox").toString() + " k\n\n")

            visualUpdate(ACTION_PDISPLAY, "Testing getBusyBoxVersion")
            visualUpdate(ACTION_DISPLAY, "[ Checking busybox version ]\n")
            visualUpdate(ACTION_DISPLAY, RootTools.getBusyBoxVersion("/system/xbin/") + " k\n\n")

            try {
                visualUpdate(ACTION_PDISPLAY, "Testing fixUtils")
                visualUpdate(ACTION_DISPLAY, "[ Checking Utils ]\n")
                visualUpdate(ACTION_DISPLAY, RootTools.fixUtils(arrayOf("ls", "rm", "ln", "dd", "chmod", "mount")).toString() + " k\n\n")
            } catch (e2: Exception) {
                e2.printStackTrace()
            }

            try {
                visualUpdate(ACTION_PDISPLAY, "Testing getSymlink")
                visualUpdate(ACTION_DISPLAY, "[ Checking [[ for symlink ]\n")
                visualUpdate(ACTION_DISPLAY, RootTools.getSymlink("/system/bin/[[") + " k\n\n")
            } catch (e2: Exception) {
                e2.printStackTrace()
            }

            visualUpdate(ACTION_PDISPLAY, "Testing getInode")
            visualUpdate(ACTION_DISPLAY, "[ Checking Inodes ]\n")
            visualUpdate(ACTION_DISPLAY, RootTools.getInode("/system/bin/busybox") + " k\n\n")

            visualUpdate(ACTION_PDISPLAY, "Testing GetBusyBoxapplets")
            try {

                visualUpdate(ACTION_DISPLAY, "[ Getting all available Busybox applets ]\n")
                for (applet in RootTools.getBusyBoxApplets("/data/data/stericson.busybox/files/bb/busybox")) {
                    visualUpdate(ACTION_DISPLAY, "$applet k\n\n")
                }

            } catch (e1: Exception) {
                e1.printStackTrace()
            }

            visualUpdate(ACTION_PDISPLAY, "Testing GetBusyBox version in a special directory!")
            try {

                visualUpdate(ACTION_DISPLAY, "[ Testing GetBusyBox version in a special directory! ]\n")
                val v = RootTools.getBusyBoxVersion("/data/data/stericson.busybox/files/bb/")

                visualUpdate(ACTION_DISPLAY, "$v k\n\n")

            } catch (e1: Exception) {
                e1.printStackTrace()
            }

            visualUpdate(ACTION_PDISPLAY, "Testing getFilePermissionsSymlinks")
            val permissions = RootTools.getFilePermissionsSymlinks("/system/xbin/busybox")
            visualUpdate(ACTION_DISPLAY, "[ Checking busybox permissions and symlink ]\n")

            if (permissions != null) {
                visualUpdate(ACTION_DISPLAY, "Symlink: " + permissions.symlink + " k\n\n")
                visualUpdate(ACTION_DISPLAY, "Group Permissions: " + permissions.groupPermissions + " k\n\n")
                visualUpdate(ACTION_DISPLAY, "Owner Permissions: " + permissions.otherPermissions + " k\n\n")
                visualUpdate(ACTION_DISPLAY, "Permissions: " + permissions.permissions + " k\n\n")
                visualUpdate(ACTION_DISPLAY, "Type: " + permissions.type + " k\n\n")
                visualUpdate(ACTION_DISPLAY, "User Permissions: " + permissions.userPermissions + " k\n\n")
            } else {
                visualUpdate(ACTION_DISPLAY, "Permissions == null k\n\n")
            }

            var shell: Shell

            visualUpdate(ACTION_PDISPLAY, "Testing output capture")
            visualUpdate(ACTION_DISPLAY, "[ busybox ash --help ]\n")

            try {
                shell = RootTools.getShell(true)
                var cmd: Command = object : Command(
                        0,
                        "busybox ash --help") {

                    override fun commandOutput(id: Int, line: String?) {
                        visualUpdate(ACTION_DISPLAY, line!! + "\n")
                        super.commandOutput(id, line)
                    }
                }
                shell.add(cmd)

                visualUpdate(ACTION_PDISPLAY, "getevent - /dev/input/event0")
                visualUpdate(ACTION_DISPLAY, "[ getevent - /dev/input/event0 ]\n")

                cmd = object : Command(0, 0, "getevent /dev/input/event0") {
                    override fun commandOutput(id: Int, line: String?) {
                        visualUpdate(ACTION_DISPLAY, line!! + "\n")
                        super.commandOutput(id, line)
                    }

                }
                shell.add(cmd)

            } catch (e: Exception) {
                e.printStackTrace()
            }

            visualUpdate(ACTION_PDISPLAY, "Switching RootContext - SYSTEM_APP")
            visualUpdate(ACTION_DISPLAY, "[ Switching Root Context - SYSTEM_APP ]\n")

            try {
                shell = RootTools.getShell(true, Shell.ShellContext.SYSTEM_APP)
                var cmd: Command = object : Command(
                        0,
                        "id") {

                    override fun commandOutput(id: Int, line: String?) {
                        visualUpdate(ACTION_DISPLAY, line!! + "\n")
                        super.commandOutput(id, line)
                    }
                }
                shell.add(cmd)

                visualUpdate(ACTION_PDISPLAY, "Testing PM")
                visualUpdate(ACTION_DISPLAY, "[ Testing pm list packages -d ]\n")

                cmd = object : Command(
                        0,
                        "sh /system/bin/pm list packages -d") {

                    override fun commandOutput(id: Int, line: String?) {
                        visualUpdate(ACTION_DISPLAY, line!! + "\n")
                        super.commandOutput(id, line)
                    }
                }
                shell.add(cmd)

            } catch (e: Exception) {
                e.printStackTrace()
            }

            visualUpdate(ACTION_PDISPLAY, "Switching RootContext - UNTRUSTED")
            visualUpdate(ACTION_DISPLAY, "[ Switching Root Context - UNTRUSTED ]\n")

            try {
                shell = RootTools.getShell(true, Shell.ShellContext.UNTRUSTED_APP)
                val cmd = object : Command(
                        0,
                        "id") {

                    override fun commandOutput(id: Int, line: String?) {
                        visualUpdate(ACTION_DISPLAY, line!! + "\n")
                        super.commandOutput(id, line)
                    }
                }
                shell.add(cmd)

            } catch (e: Exception) {
                e.printStackTrace()
            }

            visualUpdate(ACTION_PDISPLAY, "Testing df")
            val spaceValue = RootTools.getSpace("/data")
            visualUpdate(ACTION_DISPLAY, "[ Checking /data partition size]\n")
            visualUpdate(ACTION_DISPLAY, spaceValue.toString() + "k\n\n")

            try {
                shell = RootTools.getShell(true)

                val cmd = object : Command(42, false, "echo done") {

                    internal var _catch = false

                    override fun commandOutput(id: Int, line: String?) {
                        if (_catch) {
                            RootTools.log("CAUGHT!!!")
                        }

                        super.commandOutput(id, line)

                    }

                    override fun commandTerminated(id: Int, reason: String?) {
                        synchronized(this@SanityCheckRootTools) {

                            _catch = true
                            visualUpdate(ACTION_PDISPLAY, "All tests complete.")
                            visualUpdate(ACTION_HIDE, null)

                            try {
                                RootTools.closeAllShells()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                        }
                    }

                    override fun commandCompleted(id: Int, exitCode: Int) {
                        synchronized(this@SanityCheckRootTools) {
                            _catch = true

                            visualUpdate(ACTION_PDISPLAY, "All tests complete.")
                            visualUpdate(ACTION_HIDE, null)

                            try {
                                RootTools.closeAllShells()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                        }
                    }
                }

                shell.add(cmd)

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        private fun visualUpdate(action: Int, text: String?) {
            val msg = mHandler.obtainMessage()
            val bundle = Bundle()
            bundle.putInt(ACTION, action)
            bundle.putString(TEXT, text)
            msg.data = bundle
            mHandler.sendMessage(msg)
        }
    }

    private inner class TestHandler : Handler() {

        override fun handleMessage(msg: Message) {
            val action = msg.data.getInt(ACTION)
            val text = msg.data.getString(TEXT)

            when (action) {
                ACTION_SHOW -> {
                    mPDialog!!.show()
                    mPDialog!!.setMessage("Running Root Library Tests...")
                }
                ACTION_HIDE -> {
                    if (null != text) {
                        print(text)
                    }
                    mPDialog!!.hide()
                }
                ACTION_DISPLAY -> print(text)
                ACTION_PDISPLAY -> mPDialog!!.setMessage(text)
            }
        }
    }

    companion object {
        val ACTION = "action"
        val ACTION_SHOW = 0x01
        val ACTION_HIDE = 0x02
        val ACTION_DISPLAY = 0x03
        val ACTION_PDISPLAY = 0x04
        val TEXT = "text"
    }
}
