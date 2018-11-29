package com.sxtanna.aspirianmc.command

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.base.Result.None
import com.sxtanna.aspirianmc.base.Result.Some
import com.sxtanna.aspirianmc.base.Result.Status.FAIL
import com.sxtanna.aspirianmc.base.Result.Status.PASS
import com.sxtanna.aspirianmc.command.base.CommandBase
import com.sxtanna.aspirianmc.command.base.CommandContext
import com.sxtanna.aspirianmc.company.Company
import com.sxtanna.aspirianmc.company.Staffer
import com.sxtanna.aspirianmc.company.menu.CompanyAdminMenu
import com.sxtanna.aspirianmc.company.menu.CompanyItemsMenu
import com.sxtanna.aspirianmc.config.Configs.PAYOUTS_MAX_TAXES
import com.sxtanna.aspirianmc.config.Garnish.*
import com.sxtanna.aspirianmc.exts.*
import com.sxtanna.aspirianmc.market.Product
import com.sxtanna.aspirianmc.market.menu.GlobalCompanyMarketMenu
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
        register(CommandCompanyClose())

        register(CommandCompanyHelp())


        register(CommandCompanyAdmin())
        register(CommandCompanyItems())

        register(CommandCompanyGive())
        register(CommandCompanyFire())

        register(CommandCompanyDeny())

        register(CommandCompanyPayout())
        register(CommandCompanySponsor())

        loadCommandMessages()
    }


    override fun CommandContext.evaluate() {
        val targetText = input.getOrNull(0) ?: return openCompanyMenu()
        val targetBase = subCommands[targetText.toLowerCase()] ?: return reply("&cSub-Command &7$targetText&c doesn't exist")

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


    private fun CommandContext.openCompanyMenu() {
        val player = getAsPlayer ?: return reply("You must be a player to open the company shop")

        val menu = GlobalCompanyMarketMenu(plugin)
        menu.open(player)
    }

    private fun register(commandBase: CommandBase) {
        subCommands[commandBase.name] = commandBase
    }

    private fun loadCommandMessages() {
        val file = pluginFolder.resolve("command-descriptions.korm")

        if (file.exists().not()) {
            val data = subCommands.keys.associateWith { "default description" }
            korm.push(data, ensureUsable(file))
        }

        val data = try {
            korm.pull(file).toHash<String, String>().mapKeys { it.key.toLowerCase() }.filter { it.value.isNotBlank() }
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

    private fun CommandContext.quickAccessCompanyStaffers(includeOwner: Boolean = false): List<String> {
        val company = plugin.quickAccessCompany((sender as? Player)?.uniqueId ?: return emptyList())
                ?: return emptyList()

        return if (includeOwner) {
            company.staffer
        } else {
            company.staffer.filterNot { it == sender.uniqueId }
        }.map(plugin.stafferManager.names::get).filterApplicable(0)
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
            }
            catch (ex: Exception) {
                return@get reportException(ex)
            }

            block.invoke(company)
        }

        when (companyResult) {
            is Some -> {
                block.invoke(companyResult.data)
            }
            is None -> {

            }
        }
    }

    private fun CommandContext.retrieveIsOwner(): Boolean {
        val stafferUUID = getAsPlayer?.uniqueId ?: return false
        return plugin.quickAccessCompany(stafferUUID)?.isOwner(stafferUUID) ?: false
    }

    private fun CommandContext.retrieveInCompany(): Boolean {
        val stafferUUID = getAsPlayer?.uniqueId ?: return false
        return plugin.quickAccessCompany(stafferUUID) != null
    }


    // [DONE]
    inner class CommandCompanyHelp : CommandBase {

        override val name = "help"


        override fun CommandContext.evaluate() {
            val pages = processPagination()

            var index = 0
            val reply = when(pages.size > 1) {
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
            return when(input.size) {
                0, 1 -> {
                    (0..pages.lastIndex).map { "${it + 1}" }
                }
                else -> {
                    emptyList()
                }
            }
        }


        private fun CommandContext.processPagination(): List<List<String>> {
            return subCommands.filterValues { it.show && it.name in subMessages && runnable() }.values.map {
                "&f/company &a${it.name}&r\n    &7${subMessages.getOrDefault(it.name, "no description")}"
            }.chunked(3)
        }

    }

    // [DONE]
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

    // [DONE]
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

    // [DONE]
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

    // [DONE]
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

    // [DONE]
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

            val company = input.joinToString(" ").takeIf { it.isNotBlank() } ?: return reply("you must define the name of the company!")

            when (val result = plugin.companyManager.attemptCreate(company, player)) {
                is Some -> {
                    reply("successfully created your company: $company")
                    result.data.hire(staffer)

                    plugin.garnishManager.send(player, COMPANY_CREATE_PURCHASE_PASS)
                }
                is None -> {
                    reply("failed to create company: ${result.info}")

                    plugin.garnishManager.send(player, COMPANY_CREATE_PURCHASE_FAIL)
                }
            }
        }

    }


    // [DONE]
    inner class CommandCompanyHire : CommandBase {

        override val name = "hire"


        override fun CommandContext.evaluate() {
            retrieveStaffer("hire an employee!") { _, staffer ->
                processHiring(staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            val company = plugin.quickAccessCompany((sender as? Player)?.uniqueId ?: return emptyList()) ?: return emptyList()
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

    // [DONE]
    inner class CommandCompanyFire : CommandBase {

        override val name = "fire"


        override fun CommandContext.evaluate() {
            retrieveStaffer("fire an employee!") { _, staffer ->
                processFiring(staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            val company = plugin.quickAccessCompany((sender as? Player)?.uniqueId ?: return emptyList()) ?: return emptyList()
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


    // [DONE]
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
                        val name = input[0]
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

    // [DONE]
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


    // [DONE]
    inner class CommandCompanyGive : CommandBase {

        override val name = "give"


        override fun CommandContext.evaluate() {
            retrieveStaffer("give away a company!") { _, staffer ->
                processTargeting(staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            val company = plugin.quickAccessCompany((sender as? Player)?.uniqueId ?: return emptyList())
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


    // [DONE]
    inner class CommandCompanyPayout : CommandBase {

        override val name = "payout"
        override val show = false


        override fun CommandContext.evaluate() {
            retrieveStaffer("change an employee's payout!") { _, staffer ->
                processTargeting(staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return when(input.size){
                0 -> {
                    quickAccessCompanyStaffers(includeOwner = true)
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


    // [DONE]
    inner class CommandCompanyClose : CommandBase {

        override val name = "close"


        override fun CommandContext.evaluate() {
            retrieveStaffer("close a company!") { _, staffer ->
                processCompanyClose(staffer)
            }
        }

        override fun CommandContext.complete(): List<String> {
            return emptyList()
        }


        private fun CommandContext.processCompanyClose(staffer: Staffer) {
            retrieveCompany(staffer) { company ->
                if (company.isOwner(staffer.uuid).not()) {
                    return@retrieveCompany reply("you must be the owner of the company to close it")
                }

                when (val result = plugin.companyManager.kill(company)) {
                    is Some -> {
                        reply("successfully closed your company")
                    }
                    is None -> {
                        reply("failed to close your company: ${result.info}")
                    }
                }
            }
        }

    }


    // [DONE]
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

                when (val result = plugin.vaultHook.attemptTake(player, plugin.marketsManager.iconFee)) {
                    is Some -> {
                        company.icon = item.type
                        reply("successfully changed your company's icon to: ${item.type.properName()}")

                        plugin.garnishManager.send(player, COMPANY_ICON_PURCHASE_PASS)
                    }
                    is None -> {
                        reply("failed to change company icon: ${result.info}")

                        plugin.garnishManager.send(player, COMPANY_ICON_PURCHASE_FAIL)
                    }
                }
            }
        }

    }

    // [DONE]
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

                player.inventory.remove(item)

                company.product += Product().updateData(staffer, item, System.currentTimeMillis(), cost).apply {
                    this.plugin = company.plugin
                }

                reply("now selling &e${if (item.type.maxStackSize == 1) "" else "${item.amount} "}${itemStackName(item)}&r for &a$$cost")
            }
        }

    }

    // [DONE]
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

}