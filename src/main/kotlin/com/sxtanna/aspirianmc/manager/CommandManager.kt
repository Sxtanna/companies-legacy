package com.sxtanna.aspirianmc.manager

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.command.Command
import com.sxtanna.aspirianmc.command.CommandCompany
import com.sxtanna.aspirianmc.manager.base.Manager

class CommandManager(override val plugin: Companies) : Manager("Commands") {

    override fun enable() {
        register(CommandCompany(plugin))
    }


    fun register(command: Command) {
        commandMap.register(plugin.name, command.Bukkit())
    }

}