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
package com.stericson.rootshell.execution

import android.content.Context
import com.stericson.rootshell.RootShell
import com.stericson.rootshell.exceptions.RootDeniedException
import java.io.*
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.TimeoutException

class Shell @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
private constructor(cmd: String, shellType: ShellType, shellContext: ShellContext, shellTimeout: Int) {

    private val obj = java.lang.Object()
    //per shell
    private var shellTimeout = 25000

    private var shellType: ShellType? = null

    private var shellContext = ShellContext.NORMAL

    private var error = ""

    private var proc: Process? = null

    private var inputStream: BufferedReader? = null

    private var errorStream: BufferedReader? = null

    private var outputStream: OutputStreamWriter? = null

    private val commands = ArrayList<Command>()

    //indicates whether or not to close the shell
    private var close = false

    private var isSELinuxEnforcing: Boolean? = null

    var isExecuting = false

    var isReading = false

    var isClosed = false

    private val maxCommands = 5000

    private var read = 0

    private var write = 0

    private var totalExecuted = 0

    private var totalRead = 0

    private var isCleaning = false

    /**
     * Runnable to write commands to the open shell.
     *
     *
     * When writing commands we stay in a loop and wait for new
     * commands to added to "commands"
     *
     *
     * The notification of a new command is handled by the method add in this class
     */
    private val input = Runnable {
        try {
            while (true) {

                synchronized(commands) {
                    /**
                     * While loop is used in the case that notifyAll is called
                     * and there are still no commands to be written, a rare
                     * case but one that could happen.
                     */
                    /**
                     * While loop is used in the case that notifyAll is called
                     * and there are still no commands to be written, a rare
                     * case but one that could happen.
                     */
                    while (!close && write >= commands.size) {
                        isExecuting = false
                        commands.forEach {
                            it.p_wait()
                        }
                    }
                }

                if (write >= maxCommands) {

                    /**
                     * wait for the read to catch up.
                     */

                    /**
                     * wait for the read to catch up.
                     */
                    while (read != write) {
                        RootShell.log("Waiting for read and write to catch up before cleanup.")
                    }
                    /**
                     * Clean up the commands, stay neat.
                     */
                    /**
                     * Clean up the commands, stay neat.
                     */
                    cleanCommands()
                }

                /**
                 * Write the new command
                 *
                 * We write the command followed by the token to indicate
                 * the end of the command execution
                 */

                /**
                 * Write the new command
                 *
                 * We write the command followed by the token to indicate
                 * the end of the command execution
                 */
                if (write < commands.size) {
                    isExecuting = true
                    val cmd = commands[write]
                    cmd.startExecution()
                    RootShell.log("Executing: " + cmd.getCommand() + " with context: " + shellContext)

                    //write the command
                    outputStream?.write(cmd.getCommand())
                    outputStream?.flush()

                    //write the token...
                    val line = "\necho $token $totalExecuted $?\n"
                    outputStream?.write(line)
                    outputStream?.flush()

                    write++
                    totalExecuted++
                } else if (close) {
                    /**
                     * close the thread, the shell is closing.
                     */
                    /**
                     * close the thread, the shell is closing.
                     */
                    isExecuting = false
                    outputStream?.write("\nexit 0\n")
                    outputStream?.flush()
                    RootShell.log("Closing shell")
                    return@Runnable
                }
            }
        } catch (e: IOException) {
            RootShell.log(e.message ?: "", RootShell.LogLevel.ERROR, e)
        } catch (e: InterruptedException) {
            RootShell.log(e.message ?: "", RootShell.LogLevel.ERROR, e)
        } finally {
            write = 0
            closeQuietly(outputStream)
        }
    }

    /**
     * Runnable to monitor the responses from the open shell.
     *
     * This include the output and error stream
     */
    private val output = object : Runnable {
        override fun run() {
            try {
                var command: Command? = null

                //as long as there is something to read, we will keep reading.
                while (!close || inputStream?.ready() == true || read < commands.size) {
                    isReading = false
                    var outputLine: String? = inputStream?.readLine() ?: ""
                    isReading = true

                    /**
                     * If we receive EOF then the shell closed?
                     */
                    if (outputLine == null) {
                        break
                    }

                    if (command == null) {
                        if (read >= commands.size) {
                            if (close) {
                                break
                            }

                            continue
                        }

                        command = commands[read]
                    }

                    /**
                     * trying to determine if all commands have been completed.
                     *
                     * if the token is present then the command has finished execution.
                     */
                    var pos = -1

                    pos = outputLine.indexOf(token)

                    if (pos == -1) {
                        /**
                         * send the output for the implementer to process
                         */
                        command.output(command.id, outputLine)
                    } else if (pos > 0) {
                        /**
                         * token is suffix of output, send output part to implementer
                         */
                        RootShell.log("Found token, line: $outputLine")
                        command.output(command.id, outputLine.substring(0, pos))
                    }

                    if (pos >= 0) {
                        outputLine = outputLine.substring(pos)
                        val fields = outputLine.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                        if (fields.size >= 2 && fields[1] != null) {
                            var id = 0

                            try {
                                id = Integer.parseInt(fields[1])
                            } catch (e: NumberFormatException) {
                            }

                            var exitCode = -1

                            try {
                                exitCode = Integer.parseInt(fields[2])
                            } catch (e: NumberFormatException) {
                            }

                            if (id == totalRead) {
                                processErrors(command)


                                /**
                                 * wait for output to be processed...
                                 *
                                 */
                                var iterations = 0
                                while (command.totalOutput > command.totalOutputProcessed) {

                                    if (iterations == 0) {
                                        iterations++
                                        RootShell.log("Waiting for output to be processed. " + command.totalOutputProcessed + " Of " + command.totalOutput)
                                    }

                                    try {

                                        synchronized(this) {
                                            obj.wait(2000)
                                        }
                                    } catch (e: Exception) {
                                        RootShell.log(e.message)
                                    }

                                }

                                RootShell.log("Read all output")

                                command.setExitCode(exitCode)
                                command.commandFinished()

                                command = null

                                read++
                                totalRead++
                                continue
                            }
                        }
                    }
                }

                try {
                    proc?.waitFor()
                    proc?.destroy()
                } catch (e: Exception) {
                }

                while (read < commands.size) {
                    if (command == null) {
                        command = commands[read]
                    }

                    if (command.totalOutput < command.totalOutputProcessed) {
                        command.terminated("All output not processed!")
                        command.terminated("Did you forget the super.commandOutput call or are you waiting on the command object?")
                    } else {
                        command.terminated("Unexpected Termination.")
                    }

                    command = null
                    read++
                }

                read = 0

            } catch (e: IOException) {
                RootShell.log(e.message ?: "", RootShell.LogLevel.ERROR, e)
            } finally {
                closeQuietly(outputStream)
                closeQuietly(errorStream)
                closeQuietly(inputStream)

                RootShell.log("Shell destroyed")
                isClosed = true
                isReading = false
            }
        }
    }

    enum class ShellType {
        NORMAL,
        ROOT,
        CUSTOM
    }

    //this is only used with root shells
    enum class ShellContext private constructor(//Recovery

            val value: String) {
        NORMAL("normal"), //The normal context...
        SHELL("u:r:shell:s0"), //unprivileged shell (such as an adb shell)
        SYSTEM_SERVER("u:r:system_server:s0"), // system_server, u:r:system:s0 on some firmwares
        SYSTEM_APP("u:r:system_app:s0"), // System apps
        PLATFORM_APP("u:r:platform_app:s0"), // System apps
        UNTRUSTED_APP("u:r:untrusted_app:s0"), // Third-party apps
        RECOVERY("u:r:recovery:s0")

    }

    init {
        var cmd = cmd

        RootShell.log("Starting shell: $cmd")
        RootShell.log("Context: " + shellContext.value)
        RootShell.log("Timeout: $shellTimeout")

        this.shellType = shellType
        this.shellTimeout = if (shellTimeout > 0) shellTimeout else this.shellTimeout
        this.shellContext = shellContext

        if (this.shellContext == ShellContext.NORMAL) {
            this.proc = Runtime.getRuntime().exec(cmd)
        } else {
            val display = getSuVersion(false)
            val internal = getSuVersion(true)

            //only done for root shell...
            //Right now only SUPERSU supports the --context switch
            if (isSELinuxEnforcing() &&
                    display != null &&
                    internal != null &&
                    display.endsWith("SUPERSU") &&
                    Integer.valueOf(internal) >= 190) {
                cmd += " --context " + this.shellContext.value
            } else {
                RootShell.log("Su binary --context switch not supported!")
                RootShell.log("Su binary display version: " + display!!)
                RootShell.log("Su binary internal version: " + internal!!)
                RootShell.log("SELinuxEnforcing: " + isSELinuxEnforcing())
            }

            this.proc = Runtime.getRuntime().exec(cmd)

        }

        this.inputStream = BufferedReader(InputStreamReader(this.proc?.inputStream, "UTF-8"))
        this.errorStream = BufferedReader(InputStreamReader(this.proc?.errorStream, "UTF-8"))
        this.outputStream = OutputStreamWriter(this.proc?.outputStream, "UTF-8")

        /**
         * Thread responsible for carrying out the requested operations
         */
        val worker = Worker(this)
        worker.start()

        try {
            /**
             * The flow of execution will wait for the thread to die or wait until the
             * given timeout has expired.
             *
             * The result of the worker, which is determined by the exit code of the worker,
             * will tell us if the operation was completed successfully or it the operation
             * failed.
             */
            worker.join(this.shellTimeout.toLong())

            /**
             * The operation could not be completed before the timeout occurred.
             */
            if (worker.exit == -911) {

                try {
                    this.proc?.destroy()
                } catch (e: Exception) {
                }

                closeQuietly(this.inputStream)
                closeQuietly(this.errorStream)
                closeQuietly(this.outputStream)

                throw TimeoutException(this.error)
            } else if (worker.exit == -42) {

                try {
                    this.proc?.destroy()
                } catch (e: Exception) {
                }

                closeQuietly(this.inputStream)
                closeQuietly(this.errorStream)
                closeQuietly(this.outputStream)

                throw RootDeniedException("Root Access Denied")
            } else {
                /**
                 * The shell is open.
                 *
                 * Start two threads, one to handle the input and one to handle the output.
                 *
                 * input, and output are runnables that the threads execute.
                 */
                val si = Thread(this.input, "Shell Input")
                si.priority = Thread.NORM_PRIORITY
                si.start()

                val so = Thread(this.output, "Shell Output")
                so.priority = Thread.NORM_PRIORITY
                so.start()
            }
            /**
             * Normal exit
             */
            /**
             * Root access denied?
             */
        } catch (ex: InterruptedException) {
            worker.interrupt()
            Thread.currentThread().interrupt()
            throw TimeoutException()
        }

    }


    @Throws(IOException::class)
    fun add(command: Command): Command {
        if (this.close) {
            throw IllegalStateException(
                    "Unable to add commands to a closed shell")
        }

        if (command.used) {
            //The command has been used, don't re-use...
            throw IllegalStateException(
                    "This command has already been executed. (Don't re-use command instances.)")
        }

        while (this.isCleaning) {
            //Don't add commands while cleaning
        }

        this.commands.add(command)

        this.notifyThreads()

        return command
    }

    @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
    fun useCWD(context: Context) {
        add(
                Command(
                        -1,
                        false,
                        "cd " + context.applicationInfo.dataDir)
        )
    }

    private fun cleanCommands() {
        this.isCleaning = true
        val toClean = Math.abs(this.maxCommands - this.maxCommands / 4)
        RootShell.log("Cleaning up: $toClean")

        for (i in 0 until toClean) {
            this.commands.removeAt(0)
        }

        this.read = this.commands.size - 1
        this.write = this.commands.size - 1
        this.isCleaning = false
    }

    private fun closeQuietly(input: Reader?) {
        try {
            input?.close()
        } catch (ignore: Exception) {
        }

    }

    private fun closeQuietly(output: Writer?) {
        try {
            output?.close()
        } catch (ignore: Exception) {
        }

    }

    @Throws(IOException::class)
    fun close() {
        RootShell.log("Request to close shell!")

        var count = 0
        while (isExecuting) {
            RootShell.log("Waiting on shell to finish executing before closing...")
            count++

            //fail safe
            if (count > 10000) {
                break
            }

        }

        synchronized(this.commands) {
            /**
             * instruct the two threads monitoring input and output
             * of the shell to close.
             */
            this.close = true
            this.notifyThreads()
        }

        RootShell.log("Shell Closed!")

        if (this === rootShell) {
            rootShell = null
        } else if (this === shell) {
            shell = null
        } else if (this === customShell) {
            customShell = null
        }
    }

    fun getCommandQueuePosition(cmd: Command): Int {
        return this.commands.indexOf(cmd)
    }

    fun getCommandQueuePositionString(cmd: Command): String {
        return "Command is in position " + getCommandQueuePosition(cmd) + " currently executing command at position " + this.write + " and the number of commands is " + commands.size
    }

    /**
     * From libsuperuser.
     *
     *
     *
     * Detects the version of the su binary installed (if any), if supported
     * by the binary. Most binaries support two different version numbers,
     * the public version that is displayed to users, and an internal
     * version number that is used for version number comparisons. Returns
     * null if su not available or retrieving the version isn't supported.
     *
     *
     *
     * Note that su binary version and GUI (APK) version can be completely
     * different.
     *
     *
     *
     * This function caches its result to improve performance on multiple
     * calls
     *
     *
     * @param internal Request human-readable version or application
     * internal version
     * @return String containing the su version or null
     */
    @Synchronized
    private fun getSuVersion(internal: Boolean): String? {
        val idx = if (internal) 0 else 1
        if (suVersion[idx] == null) {
            var version: String? = null

            // Replace libsuperuser:Shell.run with manual process execution
            val process: Process
            try {
                process = Runtime.getRuntime().exec(if (internal) "su -V" else "su -v", null)
                process.waitFor()
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return null
            }

            // From libsuperuser:StreamGobbler
            val stdout = ArrayList<String>()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            try {
                var line: String? = null
                while (({ line = reader.readLine();line }()) != null) {
                    stdout.add(line!!)
                }
            } catch (e: IOException) {
            }

            // make sure our stream is closed and resources will be freed
            try {
                reader.close()
            } catch (e: IOException) {
            }

            process.destroy()

            if (stdout.isNotEmpty()) {
                for (line in stdout) {
                    if (!internal) {
                        if (line.contains(".")) {
                            version = line
                            break
                        }
                    } else {
                        try {
                            if (Integer.parseInt(line) > 0) {
                                version = line
                                break
                            }
                        } catch (e: NumberFormatException) {
                        }

                    }
                }
            }

            suVersion[idx] = version ?: ""
        }
        return suVersion[idx]
    }

    /**
     * From libsuperuser.
     *
     * Detect if SELinux is set to enforcing, caches result
     *
     * @return true if SELinux set to enforcing, or false in the case of
     * permissive or not present
     */
    @Synchronized
    fun isSELinuxEnforcing(): Boolean {
        if (isSELinuxEnforcing == null) {
            var enforcing: Boolean? = null

            // First known firmware with SELinux built-in was a 4.2 (17)
            // leak
            if (android.os.Build.VERSION.SDK_INT >= 17) {

                // Detect enforcing through sysfs, not always present
                val f = File("/sys/fs/selinux/enforce")
                if (f.exists()) {
                    try {
                        val `is` = FileInputStream("/sys/fs/selinux/enforce")
                        try {
                            enforcing = `is`.read() == '1'.toInt()
                        } finally {
                            `is`.close()
                        }
                    } catch (e: Exception) {
                    }

                }

                // 4.4+ builds are enforcing by default, take the gamble
                if (enforcing == null) {
                    enforcing = android.os.Build.VERSION.SDK_INT >= 19
                }
            }

            if (enforcing == null) {
                enforcing = false
            }

            isSELinuxEnforcing = enforcing
        }
        return isSELinuxEnforcing!!
    }

    protected fun notifyThreads() {
        val t = object : Thread() {
            override fun run() {
                synchronized(commands) {
                    commands.forEach {
                        it.p_notifyAll()
                    }
                }
            }
        }

        t.start()
    }

    fun processErrors(command: Command?) {
        try {
            while (errorStream?.ready() == true && command != null) {
                val line = errorStream?.readLine() ?: break

                /**
                 * If we recieve EOF then the shell closed?
                 */

                /**
                 * If we recieve EOF then the shell closed?
                 */

                /**
                 * send the output for the implementer to process
                 */
                command.output(command.id, line)
            }
        } catch (e: Exception) {
            RootShell.log(e.message ?: "", RootShell.LogLevel.ERROR, e)
        }

    }

    @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
    fun switchRootShellContext(shellContext: ShellContext): Shell {
        if (this.shellType == ShellType.ROOT) {
            try {
                Shell.closeRootShell()
            } catch (e: Exception) {
                RootShell.log("Problem closing shell while trying to switch context...")
            }

            //create new root shell with new context...

            return Shell.startRootShell(this.shellTimeout, shellContext, 3)
        } else {
            //can only switch context on a root shell...
            RootShell.log("Can only switch context on a root shell!")
            return this
        }
    }

    protected class Worker constructor(var shell: Shell) : Thread() {

        var exit = -911

        override fun run() {

            /**
             * Trying to open the shell.
             *
             * We echo "Started" and we look for it in the output.
             *
             * If we find the output then the shell is open and we return.
             *
             * If we do not find it then we determine the error and report
             * it by setting the value of the variable exit
             */
            try {
                shell.outputStream?.write("echo Started\n")
                shell.outputStream?.flush()
                while (true) {
                    val line = shell.inputStream?.readLine()
                    if (line == null) {
                        throw EOFException()
                    } else if ("" == line) {
                        continue
                    } else if ("Started" == line) {
                        this.exit = 1
                        setShellOom()
                        break
                    }

                    shell.error = "unknown error occurred."
                }
            } catch (e: IOException) {
                exit = -42
                if (e.message != null) {
                    shell.error = e.message ?: ""
                } else {
                    shell.error = "RootAccess denied?."
                }
            }

        }

        /*
         * setOom for shell processes (sh and su if root shell) and discard outputs
         * Negative values make the process LESS likely to be killed in an OOM situation
         * Positive values make the process MORE likely to be killed in an OOM situation
         */
        private fun setShellOom() {
            try {
                val processClass = (shell.proc)!!::class.java
                var field: Field
                try {
                    field = processClass.getDeclaredField("pid")
                } catch (e: NoSuchFieldException) {
                    field = processClass.getDeclaredField("id")
                }

                field.isAccessible = true
                val pid = field.get(shell.proc) as Int
                shell.outputStream?.write("(echo -17 > /proc/$pid/oom_adj) &> /dev/null\n")
                shell.outputStream?.write("(echo -17 > /proc/$$/oom_adj) &> /dev/null\n")
                shell.outputStream?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    companion object {

        //Statics -- visible to all
        private val token = "F*D^W@#FGF"

        private var rootShell: Shell? = null

        private var shell: Shell? = null

        private var customShell: Shell? = null

        private val suVersion = arrayOf<String?>(null, null)

        //the default context for root shells...
        var defaultContext = ShellContext.NORMAL

        @Throws(IOException::class)
        fun closeCustomShell() {
            RootShell.log("Request to close custom shell!")

            if (customShell == null) {
                return
            }

            customShell!!.close()
        }

        @Throws(IOException::class)
        fun closeRootShell() {
            RootShell.log("Request to close root shell!")

            if (rootShell == null) {
                return
            }
            rootShell!!.close()
        }

        @Throws(IOException::class)
        fun closeShell() {
            RootShell.log("Request to close normal shell!")

            if (shell == null) {
                return
            }
            shell!!.close()
        }

        @Throws(IOException::class)
        fun closeAll() {
            RootShell.log("Request to close all shells!")

            Shell.closeShell()
            Shell.closeRootShell()
            Shell.closeCustomShell()
        }

        val openShell: Shell?
            get() = if (customShell != null) {
                customShell
            } else if (rootShell != null) {
                rootShell
            } else {
                shell
            }

        val isShellOpen: Boolean
            get() = shell == null

        val isCustomShellOpen: Boolean
            get() = customShell == null

        val isRootShellOpen: Boolean
            get() = rootShell == null

        val isAnyShellOpen: Boolean
            get() = shell != null || rootShell != null || customShell != null

        @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
        fun runRootCommand(command: Command): Command {
            return Shell.startRootShell().add(command)
        }

        @Throws(IOException::class, TimeoutException::class)
        fun runCommand(command: Command): Command {
            return Shell.startShell().add(command)
        }

        @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
        fun startRootShell(): Shell {
            return Shell.startRootShell(0, 3)
        }

        @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
        fun startRootShell(timeout: Int): Shell {
            return Shell.startRootShell(timeout, 3)
        }

        @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
        fun startRootShell(timeout: Int, retry: Int): Shell {
            return Shell.startRootShell(timeout, Shell.defaultContext, retry)
        }

        @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
        fun startRootShell(timeout: Int, shellContext: ShellContext, retry: Int): Shell {
            // keep prompting the user until they accept for x amount of times...
            var retries = 0

            if (rootShell == null) {

                RootShell.log("Starting Root Shell!")
                val cmd = "su"
                while (rootShell == null) {
                    try {
                        RootShell.log("Trying to open Root Shell, attempt #$retries")
                        rootShell = Shell(cmd, ShellType.ROOT, shellContext, timeout)
                    } catch (e: IOException) {
                        if (retries++ >= retry) {
                            RootShell.log("IOException, could not start shell")
                            throw e
                        }
                    } catch (e: RootDeniedException) {
                        if (retries++ >= retry) {
                            RootShell.log("RootDeniedException, could not start shell")
                            throw e
                        }
                    } catch (e: TimeoutException) {
                        if (retries++ >= retry) {
                            RootShell.log("TimeoutException, could not start shell")
                            throw e
                        }
                    }

                }
            } else if (rootShell!!.shellContext != shellContext) {
                try {
                    RootShell.log("Context is different than open shell, switching context... " + rootShell!!.shellContext + " VS " + shellContext)
                    rootShell!!.switchRootShellContext(shellContext)
                } catch (e: IOException) {
                    if (retries++ >= retry) {
                        RootShell.log("IOException, could not switch context!")
                        throw e
                    }
                } catch (e: RootDeniedException) {
                    if (retries++ >= retry) {
                        RootShell.log("RootDeniedException, could not switch context!")
                        throw e
                    }
                } catch (e: TimeoutException) {
                    if (retries++ >= retry) {
                        RootShell.log("TimeoutException, could not switch context!")
                        throw e
                    }
                }

            } else {
                RootShell.log("Using Existing Root Shell!")
            }

            return rootShell!!
        }

        @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
        fun startCustomShell(shellPath: String): Shell {
            return startCustomShell(shellPath, 0)
        }

        @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
        fun startCustomShell(shellPath: String, timeout: Int): Shell {

            if (customShell == null) {
                RootShell.log("Starting Custom Shell!")
                customShell = Shell(shellPath, ShellType.CUSTOM, ShellContext.NORMAL, timeout)
            } else {
                RootShell.log("Using Existing Custom Shell!")
            }
            return customShell!!
        }

        @Throws(IOException::class, TimeoutException::class)
        fun startShell(): Shell {
            return Shell.startShell(0)
        }

        @Throws(IOException::class, TimeoutException::class)
        fun startShell(timeout: Int): Shell {

            try {
                if (shell == null) {
                    RootShell.log("Starting Shell!")
                    shell = Shell("/system/bin/sh", ShellType.NORMAL, ShellContext.NORMAL, timeout)
                } else {
                    RootShell.log("Using Existing Shell!")
                }
                return shell!!
            } catch (e: RootDeniedException) {
                //Root Denied should never be thrown.
                throw IOException()
            }

        }
    }
}
