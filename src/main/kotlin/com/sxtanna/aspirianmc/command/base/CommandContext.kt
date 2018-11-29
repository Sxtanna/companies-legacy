package com.sxtanna.aspirianmc.command.base

import com.sxtanna.aspirianmc.command.Command
import com.sxtanna.aspirianmc.command.Command.CommandException
import com.sxtanna.aspirianmc.exts.PREF
import com.sxtanna.aspirianmc.exts.colorFormat
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

data class CommandContext(val sender: CommandSender, val alias: String, val input: List<String>) {

    val getAsPlayer: Player?
        get() = sender as? Player


    fun Command.reply(msg: String, receiver: CommandSender = sender) {
        receiver.sendMessage(colorFormat("$PREF $msg"))
    }

    fun List<String>.filterApplicable(index: Int): List<String> {
        return filter { it.startsWith(input[index], true) }
    }

    inline fun <T : Any> Command.notNull(value: T?, replyMessage: () -> String): T {
        return value ?: throw CommandException(replyMessage())
    }

}