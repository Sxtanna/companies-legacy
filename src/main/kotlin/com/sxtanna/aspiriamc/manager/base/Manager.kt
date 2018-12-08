package com.sxtanna.aspiriamc.manager.base

import com.sxtanna.aspiriamc.base.Named
import com.sxtanna.aspiriamc.base.PluginDependant
import com.sxtanna.aspiriamc.exts.colorFormat
import com.sxtanna.aspiriamc.exts.PREF
import org.bukkit.command.CommandSender

abstract class Manager(final override val name: String) : Named, PluginDependant {

    var enabled = false
        internal set


    open fun enable() {

    }

    open fun disable() {

    }


    fun reply(sender: CommandSender, msg: String) {
        sender.sendMessage(colorFormat("$PREF $msg"))
    }

}