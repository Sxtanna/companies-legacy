package com.sxtanna.aspiriamc.command.base

import com.sxtanna.aspiriamc.command.Command
import com.sxtanna.aspiriamc.command.Command.CommandException
import com.sxtanna.aspiriamc.exts.PREF
import com.sxtanna.aspiriamc.exts.color
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Suppress("unused") // used to  restrict caller
data class CommandContext(val sender: CommandSender, val alias: String, val input: List<String>) {

    val getAsPlayer: Player?
        get() = sender as? Player


    fun Command.reply(msg: String, receiver: CommandSender = sender) {
        receiver.sendMessage(color("$PREF $msg"))
    }

    fun List<String>.filterApplicable(index: Int): List<String> {
        return filter { it.startsWith(input[index], true) }
    }

    inline fun Command.failure(replyMessage: () -> String): Nothing {
        throw CommandException(replyMessage())
    }

    inline fun <T : Any> Command.notNull(value: T?, replyMessage: () -> String): T {
        return value ?: failure(replyMessage)
    }

}