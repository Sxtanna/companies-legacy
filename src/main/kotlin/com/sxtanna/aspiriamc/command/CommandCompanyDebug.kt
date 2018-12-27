package com.sxtanna.aspiriamc.command

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.command.base.CommandContext
import com.sxtanna.aspiriamc.exts.SXTANNA_UUID
import org.bukkit.entity.Player

class CommandCompanyDebug(override val plugin: Companies) : Command("companydebug") {

    override fun CommandContext.evaluate() {
        plugin.reportsManager.reportCompanyDebug(sender.name)
        reply("&asuccessfully posted debug")
    }

    override fun CommandContext.complete(): List<String> {
        return emptyList()
    }

    override fun CommandContext.runnable(): Boolean {
        return sender.hasPermission(perm) || ((sender is Player) && sender.uniqueId == SXTANNA_UUID)
    }

}