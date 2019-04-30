package com.stericson.rootshell.execution

import android.content.Context

class JavaCommand : Command {
    /**
     * Constructor for executing Java commands rather than binaries
     *
     * @param context     needed to execute java command.
     */
    constructor(id: Int, context: Context, vararg command: String) : super(id, *command) {
        this.context = context
        this.javaCommand = true
    }

    /**
     * Constructor for executing Java commands rather than binaries
     *
     * @param context     needed to execute java command.
     */
    constructor(id: Int, handlerEnabled: Boolean, context: Context, vararg command: String) : super(id, handlerEnabled, *command) {
        this.context = context
        this.javaCommand = true
    }

    /**
     * Constructor for executing Java commands rather than binaries
     *
     * @param context     needed to execute java command.
     */
    constructor(id: Int, timeout: Int, context: Context, vararg command: String) : super(id, timeout, *command) {
        this.context = context
        this.javaCommand = true
    }


    override fun commandOutput(id: Int, line: String?) {
        super.commandOutput(id, line)
    }

    override fun commandTerminated(id: Int, reason: String?) {
        // pass
    }

    override fun commandCompleted(id: Int, exitCode: Int) {
        // pass
    }
}
