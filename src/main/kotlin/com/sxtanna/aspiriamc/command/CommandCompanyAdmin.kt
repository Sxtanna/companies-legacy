package com.sxtanna.aspiriamc.command

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.Result
import com.sxtanna.aspiriamc.base.Result.None
import com.sxtanna.aspiriamc.base.Result.Some
import com.sxtanna.aspiriamc.command.base.CommandBase
import com.sxtanna.aspiriamc.command.base.CommandContext
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer
import com.sxtanna.aspiriamc.company.menu.CompanyAdminMenu
import com.sxtanna.aspiriamc.company.menu.CompanyPastsMenu
import com.sxtanna.aspiriamc.config.Configs
import com.sxtanna.aspiriamc.exts.properName
import org.bukkit.Material
import org.bukkit.Material.AIR
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class CommandCompanyAdmin(override val plugin: Companies)
    : Command("companyadmin", "compadmin") {

    private val subCommands = mutableMapOf<String, CommandBase>()

    init {
        register(CommandCompanySave())
        register(CommandCompanyConfig())
        register(CommandCompanyFix())
        register(CommandCompanyPast())
        register(CommandCompanyView())
        register(CommandCompanyInfo())
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
            0    -> {
                subCommands.keys.toList()
            }
            1    -> {
                subCommands.keys.toList().filterApplicable(0)
            }
            else -> {
                val targetText = input.getOrNull(0) ?: return emptyList()
                val targetBase = subCommands[targetText.toLowerCase()] ?: return emptyList()

                if (sender.hasPermission(targetBase.perm).not()) {
                    return emptyList()
                }

                with(targetBase) {
                    CommandContext(sender, targetText, input.drop(1)).complete()
                }
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
            is None -> { /* it's loading, or there's an error */
            }
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

    inner class CommandCompanyFix : CommandBase {

        override val name = "fix"


        override fun CommandContext.evaluate() {
            val targetName = notNull(input.getOrNull(0)) {
                "you must define the name of the player you want to fix!"
            }

            retrieveStafferByName(targetName) { _, staffer ->
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
                    val company = plugin.quickAccessCompanyByCompanyUUID(staffer.companyUUID)

                    if (company == null) {
                        staffer.companyUUID = null
                        return@of "successfully voided employee"
                    }

                    if (staffer.uuid !in company) {
                        staffer.companyUUID = null
                        return@of "successfully re-fired employee, lol..."
                    }

                    fail("does not need to be fixed")
                }
            }
        }

    }

    inner class CommandCompanyConfig : CommandBase {

        override val name = "config"

        private val values = Configs.values()
        private val valuesWithTypes = Configs.valuesWithTypes()


        override fun CommandContext.evaluate() {
            val target = notNull(input.getOrNull(0)) {
                "what config value do you want to target?"
            }

            val config = values.find { it::class.java.simpleName.equals(target, true) } as? Configs<Any> ?: return

            val name = config::class.java.simpleName.toLowerCase()

            if (input.size == 1) {
                reply("value of $name is ${plugin.configsManager.get(config)}")
                return
            }

            val value = notNull(mapValue(valuesWithTypes[config] ?: return, input[1])) {
                "invalid value for $name"
            }

            plugin.configsManager.set(config, value)

            reply("value of $name set to $value")
        }

        override fun CommandContext.complete(): List<String> {
            return when (input.size) {
                1    -> {
                    values.map { it::class.java.simpleName.toLowerCase() }.filterApplicable(0)
                }
                2    -> {
                    val configs = values.find { it::class.java.simpleName.equals(input[0], true) } ?: return emptyList()

                    when (valuesWithTypes[configs]) {
                        TimeUnit::class -> {
                            return TimeUnit.values().drop(3).map { it.name.toLowerCase() }.filterApplicable(1)
                        }
                        Material::class -> {
                            return Material.values().filter { it != AIR && it.isItem && it.isLegacy.not() }.map { it.name.toLowerCase() }.filterApplicable(1)
                        }
                    }

                    listOf(plugin.configsManager.get(configs).toString())
                }
                else -> {
                    emptyList()
                }
            }
        }


        private fun mapValue(clazz: KClass<*>, value: String): Any? {
            return when (clazz) {
                Int::class      -> {
                    value.toIntOrNull()
                }
                Long::class     -> {
                    value.toLongOrNull()
                }
                Double::class   -> {
                    value.toDoubleOrNull()
                }
                TimeUnit::class -> {
                    TimeUnit.values().find { it.name.equals(value, true) }
                }
                Material::class -> {
                    Material.matchMaterial(value)
                }
                else            -> {
                    null
                }
            }
        }

    }

    inner class CommandCompanyPast : CommandBase {

        override val name = "past"


        override fun CommandContext.evaluate() {
            val player = notNull(getAsPlayer) {
                "You must be a player to view reports"
            }

            val time = notNull(input.getOrNull(0)?.toLongOrNull()?.takeIf { it > 0 }) {
                "you must provide a valid time"
            }

            val unit = notNull(input.getOrNull(1)?.let { data -> TimeUnit.values().drop(3).find { it.name.equals(data, true) } }) {
                "you must provide a valid unit"
            }

            val company = notNull(input.drop(2).joinToString(" ").let { plugin.companyManager.get(it) }) {
                "you must provide a valid company"
            }

            plugin.reportsManager.purchasesFromPast(company, time, unit) {
                CompanyPastsMenu(company, time, unit, it).open(player)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return when (input.size) {
                1    -> {
                    (1..9).map { "$it" }
                }
                2    -> {
                    TimeUnit.values().drop(3).map { it.properName().toLowerCase() }.filterApplicable(1)
                }
                3    -> {
                    plugin.companyManager.companies.map(Company::name).filterApplicable(2)
                }
                else -> {
                    emptyList()
                }
            }
        }

    }

    inner class CommandCompanyView : CommandBase {

        override val name = "view"


        override fun CommandContext.evaluate() {
            val player = notNull(getAsPlayer) {
                "You must be a player to view a company"
            }

            val company = notNull(input.joinToString(" ").let { plugin.companyManager.get(it) }) {
                "you must provide a valid company"
            }

            CompanyAdminMenu(company, adminMode = true).open(player)
        }

        override fun CommandContext.complete(): List<String> {
            return when (input.size) {
                1    -> {
                    plugin.companyManager.companies.map(Company::name).filterApplicable(0)
                }
                else -> {
                    emptyList()
                }
            }
        }
    }

    inner class CommandCompanyInfo : CommandBase {

        override val name = "info"


        override fun CommandContext.evaluate() {
            val targetName = notNull(input.getOrNull(0)) {
                "you must define the name of the player you who's info you want!"
            }

            retrieveStafferByName(targetName) { _, staffer ->

                val company = plugin.quickAccessCompanyByCompanyUUID(staffer.companyUUID)

                if (company != null) {
                    return@retrieveStafferByName reply("&7player is ${if (company.isOwner(staffer.uuid)) "the owner" else "a member"} of the company &a${company.name}")
                }

                reply("&cplayer is not in a company")
            }
        }

        override fun CommandContext.complete(): List<String> {
            return when (input.size) {
                1    -> {
                    plugin.server.onlinePlayers.map(Player::getName).filterApplicable(0)
                }
                else -> {
                    emptyList()
                }
            }
        }

    }

}