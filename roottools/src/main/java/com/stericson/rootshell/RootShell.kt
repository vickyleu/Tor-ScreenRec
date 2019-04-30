/* 
 * This file is part of the RootShell Project: http://code.google.com/p/RootShell/
 *  
 * Copyright (c) 2014 Stephen Erickson, Chris Ravenscroft
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
package com.stericson.rootshell


import android.util.Log
import com.stericson.rootshell.exceptions.RootDeniedException
import com.stericson.rootshell.execution.Command
import com.stericson.rootshell.execution.Shell
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeoutException

object RootShell {

    // --------------------
    // # Public Variables #
    // --------------------

    var debugMode = true

    val version = "DataMig-RS-1.4"

    /**
     * Setting this to false will disable the handler that is used
     * by default for the 3 callback methods for Command.
     *
     *
     * By disabling this all callbacks will be called from a thread other than
     * the main UI thread.
     */
    var handlerEnabled = true


    /**
     * Setting this will change the default command timeout.
     *
     *
     * The default is +8
     */
    var defaultCommandTimeout = Integer.MAX_VALUE

    /**
     * This will return the environment variable PATH
     *
     * @return `List<String></String>` A List of Strings representing the environment variable $PATH
     */
    val path: List<String>
        get() = Arrays.asList(*System.getenv("PATH")!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())

    /**
     * @return `true` if your app has been given root access.
     * @throws TimeoutException if this operation times out. (cannot determine if access is given)
     */
    //parse the userid
    val isAccessGiven: Boolean
        get() {
            val ID = HashSet<String>()
            val IAG = 158

            try {
                log("Checking for Root access")

                val command = object : Command(IAG, false, "id") {
                    override fun commandOutput(id: Int, line: String?) {
                        if (id == IAG) {
                            ID.addAll(Arrays.asList(*line!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
                        }

                        super.commandOutput(id, line)
                    }
                }

                Shell.startRootShell().add(command)
                commandWait(Shell.startRootShell(), command)
                for (userid in ID) {
                    log(userid)

                    if (userid.toLowerCase().contains("uid=0")) {
                        log("Access Given")
                        return true
                    }
                }

                return false
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }

        }

    /**
     * @return `true` if BusyBox or Toybox was found.
     */
    val isBusyboxAvailable: Boolean
        get() = findBinary("busybox", true).size > 0 || findBinary("toybox", true).size > 0

    /**
     * @return `true` if su was found.
     */
    val isRootAvailable: Boolean
        get() = findBinary("su", true).size > 0

    enum class LogLevel {
        VERBOSE,
        ERROR,
        DEBUG,
        WARN
    }
    // --------------------
    // # Public Methods #
    // --------------------

    /**
     * This will close all open shells.
     */
    @Throws(IOException::class)
    fun closeAllShells() {
        Shell.closeAll()
    }

    /**
     * This will close the custom shell that you opened.
     */
    @Throws(IOException::class)
    fun closeCustomShell() {
        Shell.closeCustomShell()
    }

    /**
     * This will close either the root shell or the standard shell depending on what you specify.
     *
     * @param root a `boolean` to specify whether to close the root shell or the standard shell.
     */
    @Throws(IOException::class)
    fun closeShell(root: Boolean) {
        if (root) {
            Shell.closeRootShell()
        } else {
            Shell.closeShell()
        }
    }

    /**
     * Use this to check whether or not a file OR directory exists on the filesystem.
     *
     * @param file  String that represent the file OR the directory, including the full path to the
     * file and its name.
     * @param isDir boolean that represent whether or not we are looking for a directory
     * @return a boolean that will indicate whether or not the file exists.
     */
    @JvmOverloads
    fun exists(file: String, isDir: Boolean = false): Boolean {
        val result = ArrayList<String>()

        val cmdToExecute = "ls " + if (isDir) "-d " else " "

        var command: Command = object : Command(0, false, cmdToExecute + file) {
            override fun commandOutput(id: Int, line: String?) {
                log(line)
                result.add(line?:"")

                super.commandOutput(id, line)
            }
        }

        try {
            //Try without root...
            getShell(false).add(command)
            commandWait(getShell(false), command)

        } catch (e: Exception) {
            log("Exception: $e")
            return false
        }

        for (line in result) {
            if (line.trim { it <= ' ' } == file) {
                return true
            }
        }

        result.clear()

        command = object : Command(0, false, cmdToExecute + file) {
            override fun commandOutput(id: Int, line: String?) {
                log(line)
                result.add(line?:"")

                super.commandOutput(id, line)
            }
        }

        try {
            getShell(true).add(command)
            commandWait(getShell(true), command)

        } catch (e: Exception) {
            log("Exception: $e")
            return false
        }

        //Avoid concurrent modification...
        val final_result = ArrayList<String>()
        final_result.addAll(result)

        for (line in final_result) {
            if (line.trim { it <= ' ' } == file) {
                return true
            }
        }

        return false

    }

    /**
     * @param binaryName String that represent the binary to find.
     * @param singlePath boolean that represents whether to return a single path or multiple.
     *
     * @return `List<String></String>` containing the locations the binary was found at.
     */
    fun findBinary(binaryName: String, singlePath: Boolean): List<String> {
        return findBinary(binaryName, null, singlePath)
    }

    /**
     * @param binaryName `String` that represent the binary to find.
     * @param searchPaths `List<String></String>` which contains the paths to search for this binary in.
     * @param singlePath boolean that represents whether to return a single path or multiple.
     *
     * @return `List<String></String>` containing the locations the binary was found at.
     */
    fun findBinary(binaryName: String, searchPaths: List<String>?, singlePath: Boolean): List<String> {
        var searchPaths = searchPaths

        val foundPaths = ArrayList<String>()

        var found = false

        if (searchPaths == null) {
            searchPaths = RootShell.path
        }

        log("Checking for $binaryName")

        //Try to use stat first
        try {
            for (path_ in searchPaths) {
                var path = path_
                if (!path.endsWith("/")) {
                    path += "/"
                }
                val currentPath = path
                var cc: Command = object : Command(0, false, "stat $path$binaryName") {
                    override fun commandOutput(id: Int, line: String?) {
                        if (line?.contains("File: ") == true && line.contains(binaryName)) {
                            foundPaths.add(currentPath)
                            log("$binaryName was found here: $currentPath")
                        }
                        log(line)
                        super.commandOutput(id, line)
                    }
                }
                cc = getShell(false).add(cc)
                commandWait(getShell(false), cc)

                if (foundPaths.size > 0 && singlePath) {
                    break
                }
            }

            found = !foundPaths.isEmpty()

        } catch (e: Exception) {
            log("$binaryName was not found, more information MAY be available with Debugging on.")
        }

        if (!found) {
            log("Trying second method")

            for (path_ in searchPaths) {
                var path = path_
                if (!path.endsWith("/")) {
                    path += "/"
                }
                if (exists(path + binaryName)) {
                    log("$binaryName was found here: $path")
                    foundPaths.add(path)
                    if (foundPaths.size > 0 && singlePath) {
                        break
                    }
                } else {
                    log("$binaryName was NOT found here: $path")
                }
            }
        }
        foundPaths.reverse()
        return foundPaths
    }

    /**
     * This will open or return, if one is already open, a custom shell, you are responsible for managing the shell, reading the output
     * and for closing the shell when you are done using it.
     *
     * @param shellPath a `String` to Indicate the path to the shell that you want to open.
     * @param timeout   an `int` to Indicate the length of time before giving up on opening a shell.
     * @throws TimeoutException
     * @throws com.stericson.rootshell.exceptions.RootDeniedException
     * @throws IOException
     */
    @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
    fun getCustomShell(shellPath: String, timeout: Int): Shell {
        return RootShell.getCustomShell(shellPath, timeout)
    }

    /**
     * This will open or return, if one is already open, a shell, you are responsible for managing the shell, reading the output
     * and for closing the shell when you are done using it.
     *
     * @param root         a `boolean` to Indicate whether or not you want to open a root shell or a standard shell
     * @param timeout      an `int` to Indicate the length of time to wait before giving up on opening a shell.
     * @param shellContext the context to execute the shell with
     * @param retry        a `int` to indicate how many times the ROOT shell should try to open with root priviliges...
     */
    @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
    @JvmOverloads
    fun getShell(root: Boolean, timeout: Int, shellContext: Shell.ShellContext = Shell.defaultContext, retry: Int = 3): Shell {
        return if (root) {
            Shell.startRootShell(timeout, shellContext, retry)
        } else {
            Shell.startShell(timeout)
        }
    }

    /**
     * This will open or return, if one is already open, a shell, you are responsible for managing the shell, reading the output
     * and for closing the shell when you are done using it.
     *
     * @param root         a `boolean` to Indicate whether or not you want to open a root shell or a standard shell
     * @param shellContext the context to execute the shell with
     */
    @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
    fun getShell(root: Boolean, shellContext: Shell.ShellContext): Shell {
        return getShell(root, 0, shellContext, 3)
    }

    /**
     * This will open or return, if one is already open, a shell, you are responsible for managing the shell, reading the output
     * and for closing the shell when you are done using it.
     *
     * @param root a `boolean` to Indicate whether or not you want to open a root shell or a standard shell
     */
    @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
    fun getShell(root: Boolean): Shell {
        return getShell(root, 0)
    }

    /**
     * This method allows you to output debug messages only when debugging is on. This will allow
     * you to add a debug option to your app, which by default can be left off for performance.
     * However, when you need debugging information, a simple switch can enable it and provide you
     * with detailed logging.
     *
     *
     * This method handles whether or not to log the information you pass it depending whether or
     * not RootShell.debugMode is on. So you can use this and not have to worry about handling it
     * yourself.
     *
     * @param msg The message to output.
     */
    fun log(msg: String?) {
        log(null, msg, LogLevel.DEBUG, null)
    }

    /**
     * This method allows you to output debug messages only when debugging is on. This will allow
     * you to add a debug option to your app, which by default can be left off for performance.
     * However, when you need debugging information, a simple switch can enable it and provide you
     * with detailed logging.
     *
     *
     * This method handles whether or not to log the information you pass it depending whether or
     * not RootShell.debugMode is on. So you can use this and not have to worry about handling it
     * yourself.
     *
     * @param msg  The message to output.
     * @param type The type of log, 1 for verbose, 2 for error, 3 for debug, 4 for warn
     * @param e    The exception that was thrown (Needed for errors)
     */
    fun log(msg: String, type: LogLevel, e: Exception) {
        log(null, msg, type, e)
    }

    /**
     * This method allows you to check whether logging is enabled.
     * Yes, it has a goofy name, but that's to keep it as short as possible.
     * After all writing logging calls should be painless.
     * This method exists to save Android going through the various Java layers
     * that are traversed any time a string is created (i.e. what you are logging)
     *
     *
     * Example usage:
     * if(islog) {
     * StrinbBuilder sb = new StringBuilder();
     * // ...
     * // build string
     * // ...
     * log(sb.toString());
     * }
     *
     * @return true if logging is enabled
     */
    fun islog(): Boolean {
        return debugMode
    }

    /**
     * This method allows you to output debug messages only when debugging is on. This will allow
     * you to add a debug option to your app, which by default can be left off for performance.
     * However, when you need debugging information, a simple switch can enable it and provide you
     * with detailed logging.
     *
     *
     * This method handles whether or not to log the information you pass it depending whether or
     * not RootShell.debugMode is on. So you can use this and not have to worry about handling it
     * yourself.
     *
     * @param TAG  Optional parameter to define the tag that the Log will use.
     * @param msg  The message to output.
     * @param type The type of log, 1 for verbose, 2 for error, 3 for debug
     * @param e    The exception that was thrown (Needed for errors)
     */
    @JvmOverloads
    fun log(TAG_: String?, msg: String?, type: LogLevel = LogLevel.DEBUG, e: Exception? = null) {
        var tag = TAG_
        if (msg != null && msg != "") {
            if (debugMode) {
                if (tag == null) {
                    tag = version
                }
                when (type) {
                    RootShell.LogLevel.VERBOSE -> Log.v(tag, msg)
                    RootShell.LogLevel.ERROR -> Log.e(tag, msg, e)
                    RootShell.LogLevel.DEBUG -> Log.d(tag, msg)
                    RootShell.LogLevel.WARN -> Log.w(tag, msg)
                }
            }
        }
    }

    // --------------------
    // # Public Methods #
    // --------------------

    @Throws(Exception::class)
    private fun commandWait(shell: Shell, cmd: Command) {
        while (!cmd.isFinished) {

            log(version, shell.getCommandQueuePositionString(cmd))
            log(version, "Processed " + cmd.totalOutputProcessed + " of " + cmd.totalOutput + " output from command.")

            synchronized(cmd) {
                try {
                    if (!cmd.isFinished) {
                        cmd.p_wait(2000)
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }

            if (!cmd.isExecuting && !cmd.isFinished) {
                if (!shell.isExecuting && !shell.isReading) {
                    log(version, "Waiting for a command to be executed in a shell that is not executing and not reading! \n\n Command: " + cmd.command)
                    val e = Exception()
                    e.stackTrace = Thread.currentThread().stackTrace
                    e.printStackTrace()
                } else if (shell.isExecuting && !shell.isReading) {
                    log(version, "Waiting for a command to be executed in a shell that is executing but not reading! \n\n Command: " + cmd.command)
                    val e = Exception()
                    e.stackTrace = Thread.currentThread().stackTrace
                    e.printStackTrace()
                } else {
                    log(version, "Waiting for a command to be executed in a shell that is not reading! \n\n Command: " + cmd.command)
                    val e = Exception()
                    e.stackTrace = Thread.currentThread().stackTrace
                    e.printStackTrace()
                }
            }

        }
    }
}
/**
 * Use this to check whether or not a file exists on the filesystem.
 *
 * @param file String that represent the file, including the full path to the
 * file and its name.
 * @return a boolean that will indicate whether or not the file exists.
 */
/**
 * This will open or return, if one is already open, a shell, you are responsible for managing the shell, reading the output
 * and for closing the shell when you are done using it.
 *
 * @param root         a `boolean` to Indicate whether or not you want to open a root shell or a standard shell
 * @param timeout      an `int` to Indicate the length of time to wait before giving up on opening a shell.
 * @param shellContext the context to execute the shell with
 */
/**
 * This will open or return, if one is already open, a shell, you are responsible for managing the shell, reading the output
 * and for closing the shell when you are done using it.
 *
 * @param root    a `boolean` to Indicate whether or not you want to open a root shell or a standard shell
 * @param timeout an `int` to Indicate the length of time to wait before giving up on opening a shell.
 */
/**
 * This method allows you to output debug messages only when debugging is on. This will allow
 * you to add a debug option to your app, which by default can be left off for performance.
 * However, when you need debugging information, a simple switch can enable it and provide you
 * with detailed logging.
 *
 *
 * This method handles whether or not to log the information you pass it depending whether or
 * not RootShell.debugMode is on. So you can use this and not have to worry about handling it
 * yourself.
 *
 * @param TAG Optional parameter to define the tag that the Log will use.
 * @param msg The message to output.
 */
