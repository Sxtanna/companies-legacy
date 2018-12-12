package com.sxtanna.aspiriamc.command

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.Result
import com.sxtanna.aspiriamc.base.Result.None
import com.sxtanna.aspiriamc.base.Result.Some
import com.sxtanna.aspiriamc.command.base.CommandBase
import com.sxtanna.aspiriamc.command.base.CommandContext
import com.sxtanna.aspiriamc.company.Staffer
import org.bukkit.entity.Player
import java.util.*

class CommandCompanyAdmin(override val plugin: Companies)
    : Command("companyadmin", "compadmin") {

    private val subCommands = mutableMapOf<String, CommandBase>()

    init {
        register(CommandCompanySave())
        register(CommandCompanyAdminFix())
    }


    override fun CommandContext.evaluate() {
        val targetText = notNull(input.getOrNull(0)) {
            "&cwhat do you want to do?"
        }
        val targetBase = notNull(subCommands[targetText.toLowerCase()]) {
            "&cSub-Command &7$targetText&c doesn't exist"
        }

        if (sender.hasPermission(targetBase.perm).not()) {
            return reply("&cYou don't have permission to use this command")
        }

        with(targetBase) {
            CommandContext(sender, targetText, input.drop(1)).evaluate()
        }
    }

    override fun CommandContext.complete(): List<String> {
        return when (input.size) {
            0 -> {
                subCommands.keys.toList()
            }
            1 -> {
                subCommands.keys.toList().filterApplicable(0)
            }
            2 -> {
                val targetText = input.getOrNull(0) ?: return emptyList()
                val targetBase = subCommands[targetText.toLowerCase()] ?: return emptyList()

                if (sender.hasPermission(targetBase.perm).not()) {
                    return emptyList()
                }

                with(targetBase) {
                    CommandContext(sender, targetText, input.drop(1)).complete()
                }
            }
            else -> {
                emptyList()
            }
        }
    }


    private fun register(commandBase: CommandBase) {
        subCommands[commandBase.name] = commandBase
    }


    @Suppress("unused") // used to  restrict caller
    private inline fun CommandContext.retrieveStafferByUUID(playerUUID: UUID, crossinline block: (staffer: Staffer) -> Unit) {
        val stafferResult = plugin.stafferManager.get(playerUUID) { data, _ ->
            block.invoke(data)
        }

        when (stafferResult) {
            is Some -> {
                block.invoke(stafferResult.data)
            }
            is None -> { /* it's loading, or there's an error */ }
        }
    }

    private inline fun CommandContext.retrieveStafferByName(playerName: String, crossinline block: (player: Player, staffer: Staffer) -> Unit) {
        val targetData = notNull(findPlayerByName(playerName)) {
            "player named '$playerName' was  not found"
        }

        retrieveStafferByUUID(targetData.uniqueId) {
            block.invoke(targetData, it)
        }
    }


    inner class CommandCompanySave : CommandBase {

        override val name = "save"


        override fun CommandContext.evaluate() {
            async {
                plugin.stafferManager.staffers.forEach(plugin.companyDatabase::saveStaffer)
                plugin.companyManager.companies.forEach(plugin.companyDatabase::saveCompany)

                sync {
                    reply("&asuccessfully saved all staffers and companies")
                }
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }

    }

    inner class CommandCompanyAdminFix : CommandBase {

        override val name = "fix"


        override fun CommandContext.evaluate() {
            val targetName = notNull(input.getOrNull(0)) {
                "you must define the name of the player you want to fix!"
            }

            retrieveStafferByName(targetName) { player, staffer ->
                when (val result = fixStaffer(staffer)) {
                    is Some -> {
                        reply("&asuccess:&7 ${result.data}")
                    }
                    is None -> {
                        reply("&cfailure:&7 ${result.info}")
                    }
                }
            }
        }

        override fun CommandContext.complete(): List<String> {
            return onlinePlayers.filter { plugin.quickAccessStaffer(it.uniqueId)?.companyUUID == null }.map(Player::getName).filterApplicable(0)
        }


        private fun fixStaffer(staffer: Staffer): Result<String> = Result.of {
            when (staffer.companyUUID) {
                null -> {
                    plugin.companyManager.companies.forEach {
                        if (staffer.uuid !in it) return@forEach

                        staffer.companyUUID = it.uuid
                        return@of "successfully re-assigned to ${it.name}"
                    }

                    fail("no company has this staffer listed")
                }
                else -> {
                    fail("does not need to be fixed")
                }
            }
        }

    }

}