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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.stericson.rootshell.RootShell
import java.io.IOException

open class Command {

    //directly modified by JavaCommand
    protected var javaCommand = false
    protected var context: Context? = null

    private val obj=java.lang.Object()
    var totalOutput = 0

    var totalOutputProcessed = 0

    internal var executionMonitor: ExecutionMonitor? = null

    internal var mHandler: Handler? = null

    //Has this command already been used?
    var used = false

    var isExecuting = false
        internal set

    internal var command = arrayOf<String>()

    var isFinished = false
        internal set

    internal var terminated = false

    var isHandlerEnabled = true
        internal set

    internal var exitCode = -1

    internal var id = 0

    internal var timeout = RootShell.defaultCommandTimeout

    /**
     * Constructor for executing a normal shell command
     *
     * @param id      the id of the command being executed
     * @param command the command, or commands, to be executed.
     */
    constructor(id: Int, vararg command: String) {
        this.command = command as Array<String>
        this.id = id

        createHandler(RootShell.handlerEnabled)
    }

    /**
     * Constructor for executing a normal shell command
     *
     * @param id             the id of the command being executed
     * @param handlerEnabled when true the handler will be used to call the
     * callback methods if possible.
     * @param command        the command, or commands, to be executed.
     */
    constructor(id: Int, handlerEnabled: Boolean, vararg command: String) {
        this.command = command as Array<String>
        this.id = id

        createHandler(handlerEnabled)
    }

    /**
     * Constructor for executing a normal shell command
     *
     * @param id      the id of the command being executed
     * @param timeout the time allowed before the shell will give up executing the command
     * and throw a TimeoutException.
     * @param command the command, or commands, to be executed.
     */
    constructor(id: Int, timeout: Int, vararg command: String) {
        this.command = command as Array<String>
        this.id = id
        this.timeout = timeout

        createHandler(RootShell.handlerEnabled)
    }

    //If you override this you MUST make a final call
    //to the super method. The super call should be the last line of this method.
    open fun commandOutput(id: Int, line: String?) {
        RootShell.log("Command", "ID: $id, $line")
        totalOutputProcessed++
    }

    open fun commandTerminated(id: Int, reason: String?) {
        //pass
    }

    open fun commandCompleted(id: Int, exitcode: Int) {
        //pass
    }

    fun commandFinished() {
        if (!terminated) {
            synchronized(this) {
                if (mHandler != null && isHandlerEnabled) {
                    val msg = mHandler!!.obtainMessage()
                    val bundle = Bundle()
                    bundle.putInt(ACTION, COMMAND_COMPLETED)
                    msg.data = bundle
                    mHandler!!.sendMessage(msg)
                } else {
                    commandCompleted(id, exitCode)
                }

                RootShell.log("Command $id finished.")
                finishCommand()
            }
        }
    }

    private fun createHandler(handlerEnabled: Boolean) {

        this.isHandlerEnabled = handlerEnabled

        if (Looper.myLooper() != null && handlerEnabled) {
            RootShell.log("CommandHandler created")
            mHandler = CommandHandler()
        } else {
            RootShell.log("CommandHandler not created")
        }
    }

    fun finish() {
        RootShell.log("Command finished at users request!")
        commandFinished()
    }

    protected fun finishCommand() {
        this.isExecuting = false
        this.isFinished = true
        this.p_notifyAll()
    }

    fun p_wait(){
        try {
            obj.wait()
        }catch (e:Exception){}
    }
    fun p_wait(time:Long){
        try {
            obj.wait(time)
        }catch (e:Exception){}
    }

    fun p_notifyAll(){
        try {
            obj.notifyAll()
        }catch (e:Exception){}
    }


    fun getCommand(): String {
        val sb = StringBuilder()

        for (i in command.indices) {
            if (i > 0) {
                sb.append('\n')
            }

            sb.append(command[i])
        }

        return sb.toString()
    }

    fun getExitCode(): Int {
        return this.exitCode
    }

    fun setExitCode(code: Int) {
        synchronized(this) {
            exitCode = code
        }
    }

    fun startExecution() {
        this.used = true
        executionMonitor = ExecutionMonitor(this)
        executionMonitor!!.priority = Thread.MIN_PRIORITY
        executionMonitor!!.start()
        isExecuting = true
    }

    fun terminate() {
        RootShell.log("Terminating command at users request!")
        terminated("Terminated at users request!")
    }

    protected fun terminate(reason: String) {
        try {
            Shell.closeAll()
            RootShell.log("Terminating all shells.")
            terminated(reason)
        } catch (e: IOException) {
        }

    }

    fun terminated(reason: String) {
        synchronized(this@Command) {

            if (mHandler != null && isHandlerEnabled) {
                val msg = mHandler!!.obtainMessage()
                val bundle = Bundle()
                bundle.putInt(ACTION, COMMAND_TERMINATED)
                bundle.putString(TEXT, reason)
                msg.data = bundle
                mHandler!!.sendMessage(msg)
            } else {
                commandTerminated(id, reason)
            }

            RootShell.log("Command $id did not finish because it was terminated. Termination reason: $reason")
            setExitCode(-1)
            terminated = true
            finishCommand()
        }
    }

    fun output(id: Int, line: String) {
        totalOutput++

        if (mHandler != null && isHandlerEnabled) {
            val msg = mHandler!!.obtainMessage()
            val bundle = Bundle()
            bundle.putInt(ACTION, COMMAND_OUTPUT)
            bundle.putString(TEXT, line)
            msg.data = bundle
            mHandler!!.sendMessage(msg)
        } else {
            commandOutput(id, line)
        }
    }

    inner class ExecutionMonitor(private val command: Command) : Thread() {

        override fun run() {

            if (command.timeout > 0) {
                synchronized(command) {
                    try {
                        RootShell.log("Command " + command.id + " is waiting for: " + command.timeout)
                        command.p_wait(command.timeout.toLong())
                    } catch (e: InterruptedException) {
                        RootShell.log("Exception: $e")
                    }

                    if (!command.isFinished) {
                        RootShell.log("Timeout Exception has occurred for command: " + command.id + ".")
                        terminate("Timeout Exception")
                    }
                }
            }
        }
    }

    private inner class CommandHandler : Handler() {

        override fun handleMessage(msg: Message) {
            val action = msg.data.getInt(ACTION)
            val text = msg.data.getString(TEXT)

            when (action) {
                COMMAND_OUTPUT -> commandOutput(id, text)
                COMMAND_COMPLETED -> commandCompleted(id, exitCode)
                COMMAND_TERMINATED -> commandTerminated(id, text)
            }
        }
    }

    companion object {

        val ACTION = "action"

        val TEXT = "text"

        val COMMAND_OUTPUT = 0x01

        val COMMAND_COMPLETED = 0x02

        val COMMAND_TERMINATED = 0x03
    }
}
