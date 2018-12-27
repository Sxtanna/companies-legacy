package com.sxtanna.aspiriamc.manager

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.command.Command
import com.sxtanna.aspiriamc.command.CommandCompany
import com.sxtanna.aspiriamc.command.CommandCompanyAdmin
import com.sxtanna.aspiriamc.command.CommandCompanyDebug
import com.sxtanna.aspiriamc.manager.base.Manager

class CommandManager(override val plugin: Companies) : Manager("Commands") {

    override fun enable() {
        register(CommandCompany(plugin))
        register(CommandCompanyAdmin(plugin))
        register(CommandCompanyDebug(plugin))
    }


    fun register(command: Command) {
        commandMap.register(plugin.name, command.Bukkit())
    }

}