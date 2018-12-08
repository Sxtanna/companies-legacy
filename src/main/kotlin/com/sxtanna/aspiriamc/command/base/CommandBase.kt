package com.sxtanna.aspiriamc.command.base

import com.sxtanna.aspiriamc.base.Named

interface CommandBase : Named {

    val perm: String
        get() = "companies.command.$name"

    val show: Boolean
        get() = true

    val aliases: List<String>
        get() = emptyList()


    fun CommandContext.evaluate()

    fun CommandContext.complete(): List<String>

    fun CommandContext.runnable(): Boolean {
        return sender.hasPermission(perm)
    }

}