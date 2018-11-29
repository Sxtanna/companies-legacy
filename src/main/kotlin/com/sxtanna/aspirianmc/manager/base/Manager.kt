package com.sxtanna.aspirianmc.manager.base

import com.sxtanna.aspirianmc.base.Named
import com.sxtanna.aspirianmc.base.PluginDependant
import com.sxtanna.aspirianmc.exts.colorFormat
import com.sxtanna.aspirianmc.exts.PREF
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