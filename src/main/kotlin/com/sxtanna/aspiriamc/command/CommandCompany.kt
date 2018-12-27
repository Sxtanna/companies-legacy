package com.sxtanna.aspiriamc.command

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.Result.None
import com.sxtanna.aspiriamc.base.Result.Some
import com.sxtanna.aspiriamc.base.Result.Status.FAIL
import com.sxtanna.aspiriamc.base.Result.Status.PASS
import com.sxtanna.aspiriamc.command.base.CommandBase
import com.sxtanna.aspiriamc.command.base.CommandContext
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer
import com.sxtanna.aspiriamc.company.menu.CompanyAdminMenu
import com.sxtanna.aspiriamc.company.menu.CompanyItemsMenu
import com.sxtanna.aspiriamc.config.Configs.COMPANY_COMMAND_NAME_MAX
import com.sxtanna.aspiriamc.config.Configs.PAYOUTS_MAX_TAXES
import com.sxtanna.aspiriamc.config.Garnish.*
import com.sxtanna.aspiriamc.exts.*
import com.sxtanna.aspiriamc.market.Product
import com.sxtanna.aspiriamc.market.menu.GlobalCompanyMarketMenu
import com.sxtanna.aspiriamc.menu.impl.ConfirmationMenu
import org.bukkit.Material.AIR
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

class CommandCompany(override val plugin: Companies)
    : Command("company", "comp") {

    private val subMessages = mutableMapOf<String, String>()
    private val subCommands = mutableMapOf<String, CommandBase>()

    init {
        register(CommandCompanySell())
        register(CommandCompanyJoin())
        register(CommandCompanyHire())
        register(CommandCompanyResign())

        register(CommandCompanyTax())
        register(CommandCompanyTop())

        register(CommandCompanyIcon())

        register(CommandCompanyCreate())
        register(CommandCompanyRename())
        register(CommandCompanyClose())

        register(CommandCompanyHelp())

        register(CommandCompanyAdmin())
        register(CommandCompanyItems())

        register(CommandCompanyGive())
        register(CommandCompanyFire())

        register(CommandCompanyDeny())

        register(CommandCompanyPayout())
        register(CommandCompanySponsor())
        register(CommandCompanyWithdraw())

        loadCommandMessages()
    }


    override fun CommandContext.evaluate() {
        val text = input.getOrNull(0) ?: return openCompanyMenu()
        val base = subCommands[text.toLowerCase()] ?: return reply("&cSub-Command &7$text&c doesn't exist")

        with(base) {
            if (runnable().not()) {
                return reply("&cYou don't have permission to use this command")
            }

            CommandContext(sender, text, input.drop(1)).evaluate()
        }
    }

    override fun CommandContext.complete(): List<String> {
        return when (input.size) {
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


    private fun CommandContext.openCompanyMenu() {
        val player = getAsPlayer ?: return reply("You must be a player to open the company shop")

        GlobalCompanyMarketMenu(plugin).open(player)
    }

    private fun register(commandBase: CommandBase) {
        subCommands[commandBase.name] = commandBase
    }

    private fun loadCommandMessages() {
        val file = pluginFolder.resolve("command-descriptions.korm")

        if (file.exists().not()) {
            val data = subCommands.keys.associateWith { "default description" }
            plugin.korm.push(data, file.ensureUsable())
        }

        val data = try {
            plugin.korm.pull(file).toHash<String, String>().mapKeys { it.key.toLowerCase() }.filter { it.value.isNotBlank() }
        } catch (ex: Exception) {
            emptyMap<String, String>()
        }

        subMessages.putAll(data)
    }


    private fun quickAccessInvites(uuid: UUID): List<Company> {
        val hirings = mutableListOf<Company>()

        plugin.hiringsManager.cache.forEach { company, cache ->
            if (uuid in cache) hirings += company
        }

        return hirings
    }

    private fun CommandContext.quickAccessCompanyStaffers(): List<String> {
        val staffer = getAsPlayer?.uniqueId ?: return emptyList()
        val company = plugin.quickAccessCompanyByStafferUUID(staffer) ?: return emptyList()

        return company.staffer.map(plugin.stafferManager.names::get)
    }


    private inline fun CommandContext.retrieveStaffer(mustBeAPlayerTo: String, crossinline block: (player: Player, staffer: Staffer) -> Unit) {
        val player = notNull(getAsPlayer) {
            "you must be a player to $mustBeAPlayerTo"
        }

        val stafferResult = plugin.stafferManager.get(player.uniqueId) { data, _ ->
            block.invoke(player, data)
        }

        when (stafferResult) {
            is Some -> {
                block.invoke(player, stafferResult.data)
            }
            is None -> { /* it's loading, or there's an error */
            }
        }
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


    private fun CommandContext.retrieveCompany(staffer: Staffer, block: (company: Company) -> Unit) {
        val companyUUID = notNull(staffer.companyUUID) {
            "you aren't in a company"
        }


        val companyResult = plugin.companyManager.get(companyUUID) {

            val company = try {
                notNull(it) {
                    "your company doesn't exist"
                }
            } catch (ex: Exception) {
                return@get reportException(ex)
            }

            block.invoke(company)
        }

        when (companyResult) {
            is Some -> {
                block.invoke(companyResult.data)
            }
            is None -> { /* it's loading, or there's an error */
            }
        }
    }


    inner class CommandCompanyHelp : CommandBase {

        override val name = "help"


        override fun CommandContext.evaluate() {
            val pages = processPagination()

            var index = 0
            val reply = when (pages.size > 1) {
                true -> {
                    val page = input.getOrNull(0)?.let {
                        notNull(it.toIntOrNull()) {
                            "&a$it &cis not a valid number, must be &a1 &cto &a${pages.size}"
                        }
                    }

                    index = (page ?: 1) - 1

                    notNull(pages.getOrNull(index)) {
                        "&cpage &a$page&c does not exist, must must be &a1 &cto &a${pages.size}"
                    }
                }
                else -> {
                    pages.first()
                }
            }

            reply("company command help page &a${index + 1}&7/&a${pages.size}&r:\n${reply.joinToString("\n")}")
        }

        override fun CommandContext.complete(): List<String> {
            val pages = processPagination()
            return when (input.size) {
                0, 1 -> {
                    (input.size..pages.lastIndex).map { "${it + 1}" }
                }
                else -> {
                    emptyList()
                }
            }
        }


        private fun CommandContext.processPagination(): List<List<String>> {
            return subCommands.filterValues {
                it.show && it.name in subMessages && runnable()
            }.values.map {
                "&f/company &a${it.name}&r\n    &7${subMessages.getOrDefault(it.name, "no description")}"
            }.chunked(3)
        }

    }

    inner class CommandCompanyTax : CommandBase {

        override val name = "tax"


        override fun CommandContext.evaluate() {
            retrieveStaffer("change a company's tax") { _, staffer ->
                processTaxChange(staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }


        private fun CommandContext.processTaxChange(staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.isOwner(staffer.uuid).not()) {
                    return@retrieveCompany reply("you must be the owner of the company to manage it")
                }

                val maxTax = plugin.configsManager.get(PAYOUTS_MAX_TAXES)
                val newTax = notNull(input.getOrNull(0)?.toIntOrNull()?.takeIf { it in (0..maxTax) }) {
                    "you must provide a valid tax amount, must be a whole number between 0 and $maxTax"
                }

                company.finance.tariffs = newTax

                reply("successfully set your company's tax rate to &a$newTax%")
            }
        }

    }

    inner class CommandCompanyTop : CommandBase {

        override val name = "top"


        override fun CommandContext.evaluate() {
            reply("retrieving top companies...")

            plugin.companyManager.top { companies ->
                var index = 1
                val reply = companies.joinToString("\n") {
                    "${index++}. ${it.name} | ${it.finance.balance}"
                }

                reply("Top companies are:\n$reply")
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }

    }

    inner class CommandCompanyAdmin : CommandBase {

        override val name = "admin"
        override val show = false


        override fun CommandContext.evaluate() {
            retrieveStaffer("manage a company!") { player, staffer ->
                processManagement(player, staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }


        private fun CommandContext.processManagement(player: Player, staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.isOwner(staffer.uuid).not()) {
                    return@retrieveCompany reply("you must be the owner of the company to manage it")
                }

                CompanyAdminMenu(company).open(player)
            }
        }

    }

    inner class CommandCompanyItems : CommandBase {

        override val name = "items"
        override val show = false

        override fun CommandContext.evaluate() {
            retrieveStaffer("view company items!") { player, staffer ->
                processViewItems(player, staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }


        private fun CommandContext.processViewItems(player: Player, staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                CompanyItemsMenu(company).open(player)
            }
        }

    }

    inner class CommandCompanyCreate : CommandBase {

        override val name = "create"


        override fun CommandContext.evaluate() {
            retrieveStaffer("create a company!") { player, staffer ->
                processCreation(player, staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }


        private fun CommandContext.processCreation(player: Player, staffer: Staffer) {
            if (staffer.companyUUID != null) {
                return reply("failed to create company: you are already in a company")
            }
            if (staffer.voidedItems.isNotEmpty()) {
                return reply("failed to create company: you have voided items to claim")
            }

            val inputName = strip(input.joinToString(" ").takeIf { it.isNotBlank() } ?: return reply("you must define the name of the company!"))
            val createFee = plugin.companyManager.createFee


            val confirmation = object : ConfirmationMenu("Company Creation Cost: &a$$createFee") {

                override fun passLore(): List<String> {
                    return listOf("",
                                  "&7Create company '$inputName' for &a$$createFee")
                }

                override fun failLore(): List<String> {
                    return listOf("",
                                  "&7Stop creating company")
                }


                override fun onPass(action: MenuAction) {
                    when (val result = plugin.companyManager.attemptCreate(inputName, player)) {
                        is Some -> {
                            reply("successfully created your company: $inputName")
                            result.data.hire(staffer)

                            plugin.garnishManager.send(player, COMPANY_CREATE_PURCHASE_PASS)
                            plugin.reportsManager.reportPurchaseComp(player, createFee, result.data)
                        }
                        is None -> {
                            reply("failed to create company: ${result.info}")

                            plugin.garnishManager.send(player, COMPANY_CREATE_PURCHASE_FAIL)
                        }
                    }

                    action.who.closeInventory()
                }

                override fun onFail(action: MenuAction) {
                    action.who.closeInventory()
                }

            }

            confirmation.open(player)
        }

    }

    inner class CommandCompanyRename : CommandBase {

        override val name = "rename"


        override fun CommandContext.evaluate() {
            retrieveStaffer("change your company's name!") { player, staffer ->
                processNameChange(player, staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }


        private fun CommandContext.processNameChange(player: Player, staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.isOwner(staffer.uuid).not()) {
                    return@retrieveCompany reply("you must be the owner of the company to change the name")
                }

                val inputName = strip(input.joinToString(" ").takeIf { it.isNotBlank() }
                                          ?: return@retrieveCompany reply("you must define the new name!"))
                val renameFee = plugin.companyManager.renameFee

                val max = plugin.configsManager.get(COMPANY_COMMAND_NAME_MAX)

                if (inputName.length > max) {
                    return@retrieveCompany reply("company name $inputName is too long, must be at most $max")
                }

                val confirmation = object : ConfirmationMenu("Company Rename Cost: &a$$renameFee") {

                    override fun passLore(): List<String> {
                        return listOf(
                                "",
                                "&7Change company name from '${company.name}' to '$inputName'"
                                     )
                    }

                    override fun failLore(): List<String> {
                        return listOf(
                                "",
                                "&7Stop changing company name"
                                     )
                    }


                    override fun onPass(action: MenuAction) {
                        when (val result = plugin.companyManager.attemptRename(company, inputName, player)) {
                            is Some -> {
                                reply("successfully renamed your company to $inputName")

                                plugin.garnishManager.send(player, COMPANY_RENAME_PURCHASE_PASS)
                            }
                            is None -> {
                                reply("failed to rename company: ${result.info}")

                                plugin.garnishManager.send(player, COMPANY_RENAME_PURCHASE_FAIL)
                            }
                        }

                        action.who.closeInventory()
                    }

                    override fun onFail(action: MenuAction) {
                        action.who.closeInventory()
                    }

                }


                confirmation.open(player)
            }
        }

    }

    inner class CommandCompanyHire : CommandBase {

        override val name = "hire"


        override fun CommandContext.evaluate() {
            retrieveStaffer("hire an employee!") { _, staffer ->
                processHiring(staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            val company = plugin.quickAccessCompanyByStafferUUID((sender as? Player)?.uniqueId ?: return emptyList()) ?: return emptyList()
            return onlinePlayers.filterNot(company::contains).map(Player::getName).filterApplicable(0)
        }


        private fun CommandContext.processHiring(staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.isOwner(staffer.uuid).not()) {
                    return@retrieveCompany reply("you must be the owner of the company to hire employees")
                }

                val targetName = notNull(input.getOrNull(0)) {
                    "you must define the name of the player you want to hire!"
                }

                retrieveStafferByName(targetName) { player, staffer ->
                    processInvitation(player, staffer, company)
                }
            }
        }

        private fun CommandContext.processInvitation(target: Player, targetStaffer: Staffer, company: Company) {
            if (targetStaffer.companyUUID != null) {
                return reply("failed to hire player '${target.name}', they are already the employee of another company")
            }
            if (targetStaffer.voidedItems.isNotEmpty()) {
                return reply("failed to hire player '${target.name}', they have voided items to claim")
            }

            when (val result = company.hire(targetStaffer, plugin.hiringsManager)) {
                is Some -> when (result.data) {
                    PASS -> {
                        reply("successfully sent hiring invitation to '${target.name}'")

                        reply("you've received a hiring request from &e${company.name}&r\n    &7execute &8'&f/company join ${company.name}&8'&r to accept", findPlayerByUUID(targetStaffer.uuid)
                            ?: return)
                    }
                    FAIL -> {
                        reply("failed to hire player '${target.name}', they have already been hired")
                    }
                }
                is None -> {
                    reply("failed to hire player '${target.name}': ${result.info}")
                }
            }
        }

    }

    inner class CommandCompanyFire : CommandBase {

        override val name = "fire"


        override fun CommandContext.evaluate() {
            retrieveStaffer("fire an employee!") { _, staffer ->
                processFiring(staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            val company = plugin.quickAccessCompanyByStafferUUID((sender as? Player)?.uniqueId ?: return emptyList()) ?: return emptyList()
            return company.staffer.filterNot { it == sender.uniqueId }.map(plugin.stafferManager.names::get).filterApplicable(0)
        }


        private fun CommandContext.processFiring(staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.isOwner(staffer.uuid).not()) {
                    return@retrieveCompany reply("you must be the owner of the company to fire an employee")
                }

                val targetName = notNull(input.getOrNull(0)) {
                    "you must define the name of the player you want to fire"
                }
                val targetData = notNull(company.staffer.find { plugin.stafferManager.names[it].equals(targetName, true) }) {
                    "player named '$targetName' was  not found"
                }

                retrieveStafferByUUID(targetData) { staffer ->
                    processStafferFiring(staffer, company)
                }
            }
        }

        private fun CommandContext.processStafferFiring(firedStaffer: Staffer, company: Company) {
            val name = plugin.stafferManager.names[firedStaffer.uuid]

            when (val result = company.fire(firedStaffer)) {
                is Some -> when (result.data) {
                    PASS -> {
                        reply("successfully fired employee $name")

                        reply("you have been fired from &e${company.name}&r", findPlayerByUUID(firedStaffer.uuid)
                            ?: return)
                    }
                    FAIL -> {
                        reply("failed to fire employee $name: they do not work for this company")
                    }
                }
                is None -> {
                    reply("failed to fire employee $name: ${result.info}")
                }
            }
        }

    }

    inner class CommandCompanyResign : CommandBase {

        override val name = "resign"


        override fun CommandContext.evaluate() {
            retrieveStaffer("manage a company!") { _, staffer ->
                processResignation(staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }


        private fun CommandContext.processResignation(staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.isOwner(staffer.uuid)) {
                    return@retrieveCompany reply("you cannot resign from your own company, use '/company give' to transfer it first")
                }

                company.fire(staffer)

                company.onlineStaffers().forEach {
                    reply("player &e${sender.name}&r has resigned from the company", it)
                }

                reply("successfully resigned from &e${company.name}")
            }
        }

    }

    inner class CommandCompanyJoin : CommandBase {

        override val name = "join"


        override fun CommandContext.evaluate() {
            retrieveStaffer("accept a company invitation!") { _, staffer ->
                processJoin(staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            val hirings = quickAccessInvites((sender as? Player)?.uniqueId ?: return emptyList())
            return hirings.map(Company::name).filterApplicable(0)
        }


        private fun CommandContext.processJoin(staffer: Staffer) {

            fun joinCompany(company: Company) {
                company.onlineStaffers().forEach {
                    reply("player &e${sender.name}&r has joined the company", it)
                }

                company.hire(staffer)
                plugin.hiringsManager.attemptStop(company, staffer)

                reply("you have joined &e${company.name}")
            }


            when (val hirings = plugin.hiringsManager.hirings(staffer)) {
                is Some -> when (input.size) {
                    0 -> when (val size = hirings.data.size) {
                        0 -> {
                            reply("You haven't been invited to join any companies")
                        }
                        1 -> {
                            joinCompany(hirings.data.first())
                        }
                        else -> {
                            reply("You have been invited by &a$size&r companies, select one.\n\n${hirings.data.joinToString("&7,&r") { "&b${it.name}" }}")
                        }
                    }
                    else -> {
                        val name = strip(input.joinToString(" "))
                        val data = notNull(hirings.data.find { it.name.equals(name, true) }) {
                            "You aren't being hired by a company named '$name'"
                        }

                        joinCompany(data)
                    }
                }
                is None -> {
                    reply(hirings.info)
                }
            }
        }

    }

    inner class CommandCompanyDeny : CommandBase {

        override val name = "deny"


        override fun CommandContext.evaluate() {
            retrieveStaffer("deny a company invitation!") { _, staffer ->
                processDeny(staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            val hirings = quickAccessInvites((sender as? Player)?.uniqueId ?: return emptyList())
            return hirings.map(Company::name).filterApplicable(0)
        }


        private fun CommandContext.processDeny(staffer: Staffer) {

            fun denyCompany(company: Company) {
                plugin.hiringsManager.attemptStop(company, staffer)

                reply("player &e${sender.name}&r has refused to join the company", findPlayerByUUID(company.staffer[0])
                    ?: return)
            }


            when (val hirings = plugin.hiringsManager.hirings(staffer)) {
                is Some -> when (input.size) {
                    0 -> when (val size = hirings.data.size) {
                        0 -> {
                            reply("You haven't been invited to join any companies")
                        }
                        1 -> {
                            denyCompany(hirings.data.first())
                        }
                        else -> {
                            reply("You have been invited by &a$size&r companies, select one.\n\n${hirings.data.joinToString("&7,&r") { "&b${it.name}" }}")
                        }
                    }
                    else -> {
                        val name = input[0]
                        val data = notNull(hirings.data.find { it.name.equals(name, true) }) {
                            "You aren't being hired by a company named '$name'"
                        }

                        denyCompany(data)
                    }
                }
                is None -> {
                    reply(hirings.info)
                }
            }
        }

    }

    inner class CommandCompanyGive : CommandBase {

        override val name = "give"


        override fun CommandContext.evaluate() {
            retrieveStaffer("give away a company!") { _, staffer ->
                processTargeting(staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            val company = plugin.quickAccessCompanyByStafferUUID((sender as? Player)?.uniqueId ?: return emptyList())
                ?: return emptyList()
            return company.staffer.filterNot { it == sender.uniqueId }.map(plugin.stafferManager.names::get).filterApplicable(0)
        }


        private fun CommandContext.processTargeting(staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.isOwner(staffer.uuid).not()) {
                    return@retrieveCompany reply("you must be the owner of the company to give it to another employee")
                }

                val targetName = notNull(input.getOrNull(0)) {
                    "you must define the name of the player you want to give the company to!"
                }
                val targetData = notNull(company.staffer.find { plugin.stafferManager.names[it].equals(targetName, true) }) {
                    "player named '$targetName' was  not found"
                }

                retrieveStafferByUUID(targetData) { targetStaffer ->
                    processCompanyTransfer(staffer, targetStaffer, company)
                }
            }
        }

        private fun CommandContext.processCompanyTransfer(oldOwner: Staffer, newOwner: Staffer, company: Company) {

            company.staffer -= newOwner.uuid
            company.staffer[0] = newOwner.uuid
            company.staffer += oldOwner.uuid

            company.onlineStaffers().forEach {
                reply("company has been transfered to ${plugin.stafferManager.names[newOwner.uuid]}", it)
            }
        }

    }

    inner class CommandCompanyPayout : CommandBase {

        override val name = "payout"
        override val show = false


        override fun CommandContext.evaluate() {
            retrieveStaffer("change an employee's payout!") { _, staffer ->
                processTargeting(staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return when (input.size) {
                1 -> {
                    quickAccessCompanyStaffers().filterApplicable(0)
                }
                else -> {
                    emptyList()
                }
            }
        }


        private fun CommandContext.processTargeting(staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.isOwner(staffer.uuid).not()) {
                    return@retrieveCompany reply("you must be the owner of the company to change another employee's payout")
                }


                val targetName = notNull(input.getOrNull(0)) {
                    "you must define the name of the player you want to change the payout of!"
                }
                val targetData = notNull(company.staffer.find { plugin.stafferManager.names[it].equals(targetName, true) }) {
                    "player named '$targetName' was  not found"
                }

                retrieveStafferByUUID(targetData) { staffer ->
                    processPayoutChange(staffer, company)
                }
            }
        }

        private fun CommandContext.processPayoutChange(staff: Staffer, company: Company) {
            val newPayoutText = notNull(input.getOrNull(1)) {
                "you must define their new payout percentage, min = 0% max = 100%"
            }
            val newPayoutData = notNull(newPayoutText.toIntOrNull()?.takeIf { it in (1..100) }) {
                "$newPayoutText is not a whole number between 0 and 100"
            }

            company.finance[staff.uuid].payoutRatio = newPayoutData

            reply("successfully changed ${plugin.stafferManager.names[staff.uuid].ownership()} payout ratio to $newPayoutData")
        }

    }

    inner class CommandCompanyClose : CommandBase {

        override val name = "close"


        override fun CommandContext.evaluate() {
            retrieveStaffer("close a company!") { player, staffer ->
                processCompanyClose(player, staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }


        private fun CommandContext.processCompanyClose(player: Player, staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.isOwner(staffer.uuid).not()) {
                    return@retrieveCompany reply("you must be the owner of the company to close it")
                }

                val confirmation = object : ConfirmationMenu("Close your company?") {

                    override fun onPass(action: MenuAction) {
                        when (val result = plugin.companyManager.kill(company)) {
                            is Some -> {
                                reply("successfully closed your company")
                            }
                            is None -> {
                                reply("failed to close your company: ${result.info}")
                            }
                        }

                        action.who.closeInventory()
                    }

                    override fun onFail(action: MenuAction) {
                        action.who.closeInventory()
                    }

                }

                confirmation.open(player)
            }
        }

    }

    inner class CommandCompanyIcon : CommandBase {

        override val name = "icon"


        override fun CommandContext.evaluate() {
            retrieveStaffer("change your company's icon!") { player, staffer ->
                processIconChange(player, staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }


        private fun CommandContext.processIconChange(player: Player, staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.isOwner(staffer.uuid).not()) {
                    return@retrieveCompany reply("you must be the owner of the company to change the icon")
                }

                val item = player.inventory.itemInMainHand ?: return@retrieveCompany reply("you must be holding the item you want to use")

                val iconFee = plugin.marketsManager.iconFee

                val confirmation = object : ConfirmationMenu("Company Icon Cost: &a$$iconFee") {

                    override fun passLore(): List<String> {
                        return listOf("",
                                      "&7Change company icon to ${itemStackName(item)} for &a$$iconFee")
                    }

                    override fun failLore(): List<String> {
                        return listOf("",
                                      "&7Stop changing company icon")
                    }

                    override fun onPass(action: MenuAction) {
                        when (val result = plugin.economyHook.attemptTake(player, plugin.marketsManager.iconFee)) {
                            is Some -> {
                                plugin.reportsManager.reportPurchaseIcon(player, iconFee, company, company.icon, item.type)

                                company.icon = item.type
                                reply("successfully changed your company's icon to: ${item.type.properName()}")

                                plugin.garnishManager.send(player, COMPANY_ICON_PURCHASE_PASS)
                            }
                            is None -> {
                                reply("failed to change company icon: ${result.info}")

                                plugin.garnishManager.send(player, COMPANY_ICON_PURCHASE_FAIL)
                            }
                        }

                        action.who.closeInventory()
                    }

                    override fun onFail(action: MenuAction) {
                        action.who.closeInventory()
                    }

                }

                confirmation.open(player)
            }
        }

    }

    inner class CommandCompanySell : CommandBase {

        override val name = "sell"


        override fun CommandContext.evaluate() {
            retrieveStaffer("sell a product!") { player, staffer ->

                val itemStack = notNull(player.inventory.itemInMainHand?.takeIf { it.type != AIR }) {
                    "you must be holding the item you want to sell"
                }
                val sellPrice = notNull(input.getOrNull(0)?.toDoubleOrNull()?.formatToTwoPlaces()) {
                    "you must provide a valid sell price"
                }

                processSellItem(player, sellPrice, itemStack, staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }


        private fun CommandContext.processSellItem(player: Player, cost: Double, item: ItemStack, staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.product.size >= plugin.marketsManager.itemMax) {
                    return@retrieveCompany reply("maximum of ${plugin.marketsManager.itemMax} products can be sold at a time")
                }

                player.inventory.itemInMainHand = null

                val product = Product().updateData(staffer, item, System.currentTimeMillis(), cost).apply {
                    this.plugin = company.plugin
                }

                company.product += product

                reply("now selling &e${if (item.type.maxStackSize == 1) "" else "${item.amount} "}${itemStackName(item)}&r for &a$$cost")


                plugin.reportsManager.reportSellItem(player, product, company)
            }
        }

    }

    inner class CommandCompanySponsor : CommandBase {

        override val name = "sponsor"
        override val show = false


        override fun CommandContext.evaluate() {
            retrieveStaffer("purchase a sponsor slot!") { player, staffer ->
                processSponsor(player, staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }


        private fun CommandContext.processSponsor(player: Player, staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.isOwner(staffer.uuid).not()) {
                    return@retrieveCompany reply("you must be the owner of the company to buy a sponsored position")
                }

                when (val result = plugin.companyManager.sponsorManager.attemptPurchaseSponsorship(company, player)) {
                    is Some -> {
                        reply("successfully purchased a sponsor slot for ${plugin.companyManager.sponsorManager.humanReadableTime()}")

                        plugin.garnishManager.send(player, COMPANY_SPONSOR_PURCHASE_PASS)
                    }
                    is None -> {
                        reply("failed to purchase a sponsor slot: ${result.info}")

                        plugin.garnishManager.send(player, COMPANY_SPONSOR_PURCHASE_FAIL)
                    }
                }
            }
        }

    }

    inner class CommandCompanyWithdraw : CommandBase {

        override val name = "withdraw"


        override fun CommandContext.evaluate() {
            retrieveStaffer("withdraw from your company!") { player, staffer ->
                processWithdraw(player, staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }


        private fun CommandContext.processWithdraw(player: Player, staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.isOwner(staffer.uuid).not()) {
                    return@retrieveCompany reply("you must be the owner of the company to withdraw from the balance")
                }

                val inputs = notNull(input.getOrNull(0)) {
                    "you must supply how much you want to withdraw"
                }
                val amount = notNull(inputs.toDoubleOrNull()) {
                    "$inputs is not a valid number"
                }

                if (amount > company.finance.balance) {
                    return@retrieveCompany reply("&a$$amount &7is greater than your company's balance of &a$${company.finance.balance}")
                }

                when(val deposit = plugin.economyHook.attemptGive(player, amount)) {
                    is Some -> {
                        company.finance.balance -= amount
                        reply("&7successfully withdrawn &a$$amount")
                    }
                    is None -> {
                        reply("&cfailed to withdraw &a$$amount&c: ${deposit.info}")
                    }
                }
            }
        }

    }

}