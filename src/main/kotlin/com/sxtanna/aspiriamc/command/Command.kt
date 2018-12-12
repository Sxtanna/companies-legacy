package com.sxtanna.aspiriamc.command

import com.sxtanna.aspiriamc.base.PluginDependant
import com.sxtanna.aspiriamc.command.base.CommandBase
import com.sxtanna.aspiriamc.command.base.CommandContext
import com.sxtanna.aspiriamc.exts.BukkitCommand
import org.bukkit.command.CommandSender

abstract class Command(final override val name: String, final override val aliases: List<String> = emptyList()) : CommandBase, PluginDependant {
    constructor(name: String, vararg aliases: String) : this(name, aliases.toList())

    private lateinit var bukkitCommand: BukkitCommand


    internal fun CommandContext.reportException(ex: Exception) {
        when (ex) {
            is CommandException -> with(this) {
                reply(ex.reason)
            }
            else -> {
                logger.severe("Failed to evaluate command: $name for ${sender.name} :")
                ex.printStackTrace()
            }
        }
    }


    internal inner class Bukkit : BukkitCommand(name, "", "", aliases) {

        init {
            permission = this@Command.perm
            bukkitCommand = this
        }


        override fun execute(sender: CommandSender, alias: String, args: Array<out String>): Boolean {
            val context = CommandContext(sender, alias, args.toList())

            try {
                context.evaluate()
            } catch (ex: Exception) {
                context.reportException(ex)
            }

            return true
        }

        override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
            try {
                val context = CommandContext(sender, alias, args.toList())
                return context.complete().toMutableList()
            } catch (ex: Exception) {
                logger.severe("Failed to complete command: $name for ${sender.name} :")
                ex.printStackTrace()
            }

            return mutableListOf()
        }

    }


    @PublishedApi
    internal class CommandException(val reason: String) : IllegalStateException(reason)

}