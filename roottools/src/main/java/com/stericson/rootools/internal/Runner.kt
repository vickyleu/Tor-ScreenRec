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

package com.stericson.rootools.internal

import java.io.IOException

import com.stericson.rootshell.execution.Command
import com.stericson.rootshell.execution.Shell
import com.stericson.rootools.RootTools

import android.content.Context
import android.util.Log

class Runner(internal var context: Context, internal var binaryName: String, internal var parameter: String) : Thread() {

    override fun run() {
        var privateFilesPath: String? = null
        try {
            privateFilesPath = context.filesDir.canonicalPath
        } catch (e: IOException) {
            if (RootTools.debugMode) {
                Log.e(LOG_TAG, "Problem occured while trying to locate private files directory!")
            }
            e.printStackTrace()
        }

        if (privateFilesPath != null) {
            try {
                val command = Command(0, false, "$privateFilesPath/$binaryName $parameter")
                Shell.startRootShell().add(command)
                commandWait(command)

            } catch (e: Exception) {
            }

        }
    }

    private fun commandWait(cmd: Command) {
        synchronized(cmd) {
            try {
                if (!cmd.isFinished) {
                    cmd.p_wait(2000)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
    }

    companion object {

        private val LOG_TAG = "RootTools::Runner"
    }

}
