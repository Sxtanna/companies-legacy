package com.sxtanna.aspiriamc.manager

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.Result.Some
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer
import com.sxtanna.aspiriamc.exts.consume
import com.sxtanna.aspiriamc.exts.ensureUsable
import com.sxtanna.aspiriamc.exts.formatToTwoPlaces
import com.sxtanna.aspiriamc.exts.mapToWithin
import com.sxtanna.aspiriamc.manager.base.Manager
import com.sxtanna.aspiriamc.market.Product
import com.sxtanna.aspiriamc.reports.CompanyDebug
import com.sxtanna.aspiriamc.reports.CompanyDebug.*
import com.sxtanna.aspiriamc.reports.CompanyDebug.AspiriaInfo.*
import com.sxtanna.aspiriamc.reports.CompanyDebug.AspiriaInfo.PluginState.PluginInfo
import com.sxtanna.aspiriamc.reports.CompanyDebug.AspiriaInfo.ServerState.PaperInfo
import com.sxtanna.aspiriamc.reports.Format.PURCHASE_ITEM
import com.sxtanna.aspiriamc.reports.Reports
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

class ReportsManager(override val plugin: Companies) : Manager("Reports") {

    val popularity = Popularity(plugin)


    override fun enable() {
        popularity.enable()
    }

    override fun disable() {
        popularity.disable()
    }


    fun reportPurchase(product: Product, buyer: Player, itemStack: ItemStack) {

        fun retrieveCompany(staffer: Staffer) {
            val result = plugin.companyManager.get(staffer.companyUUID ?: return) {
                reportPurchase(it ?: return@get, product, buyer, itemStack)
            }

            when (result) {
                is Some -> {
                    reportPurchase(result.data, product, buyer, itemStack)
                }
            }
        }

        val result = plugin.stafferManager.get(product.stafferUUID ?: return) { data, _ ->
            retrieveCompany(data)
        }

        when (result) {
            is Some -> {
                retrieveCompany(result.data)
            }
        }
    }

    fun reportException(exception: Throwable) {
        val text = "Exception Message: \n    ${exception.message ?: "none"}\n\n${exception.consume()}"

        plugin.korm.push(text, pluginFolder.resolve("exception").resolve("exception-${System.currentTimeMillis()}.korm").ensureUsable())
    }

    fun reportCompanyDebug(requestedBy: String) {
        val companyInfo = CompanyInfo(plugin.companyManager.companies)
        val stafferInfo = StafferInfo(plugin.stafferManager.staffers)

        val runtime = Runtime.getRuntime()

        val free = runtime.freeMemory()
        val used = runtime.totalMemory() - free

        val memoryState = MemoryState(used, free)


        val ticks = plugin.server.tps.toList()
        val paper = PaperInfo(plugin.server.version, plugin.server.bukkitVersion)

        val serverState = ServerState(ticks, paper)


        val pluginInfos = plugin.server.pluginManager.plugins.map {
            val desc = it.description
            PluginInfo(desc.name, desc.version, desc.apiVersion ?: "unknown", desc.authors.joinToString())
        }

        val pluginState = PluginState(pluginInfos)

        val aspiriaInfo = AspiriaInfo(memoryState, serverState, pluginState)

        val debug = CompanyDebug(requestedBy, companyInfo, stafferInfo, aspiriaInfo)

        plugin.korm.push(debug, pluginFolder.resolve("debug").resolve("debug-${System.currentTimeMillis()}.korm").ensureUsable())
    }


    fun reportSellItem(player: Player, product: Product, company: Company) {
        val report = Reports.Transaction.SellItem(product.cost, company.uuid, player.uniqueId, product.uuid, product.base)
        plugin.companyDatabase.saveReports(report)
    }

    fun reportTakeItem(player: Player, product: Product, company: Company) {
        val report = Reports.Transaction.TakeItem(product.cost, company.uuid, player.uniqueId, product.uuid, product.base)
        plugin.companyDatabase.saveReports(report)
    }


    fun reportPurchaseComp(player: Player, createFee: Double, company: Company) {
        val report = Reports.Transaction.Purchase.Comp(createFee, company.uuid, player.uniqueId, company.name)
        plugin.companyDatabase.saveReports(report)
    }

    fun reportPurchaseName(player: Player, renameFee: Double, company: Company, oldName: String, newName: String) {
        val report = Reports.Transaction.Purchase.Name(renameFee, company.uuid, player.uniqueId, oldName, newName)
        plugin.companyDatabase.saveReports(report)
    }

    fun reportPurchaseIcon(player: Player, iconFee: Double, company: Company, oldType: Material, newType: Material) {
        val report = Reports.Transaction.Purchase.Icon(iconFee, company.uuid, player.uniqueId, oldType, newType)
        plugin.companyDatabase.saveReports(report)
    }

    fun reportPurchaseItem(player: Player, product: Product, company: Company, transactions: Map<UUID, Double>) {
        val report = Reports.Transaction.Purchase.Item(product.cost, company.uuid, product.stafferUUID ?: UUID.randomUUID(), player.uniqueId, product.base, product.uuid, transactions)
        plugin.companyDatabase.saveReports(report)
    }


    fun reportCompanyHire(player: Player, company: Company) {
        val report = Reports.Staffing.Hire(company.uuid, player.uniqueId)
        plugin.companyDatabase.saveReports(report)
    }

    fun reportCompanyFire(player: UUID, company: Company, firedBy: UUID) {
        val report = Reports.Staffing.Fire(company.uuid, player, firedBy)
        plugin.companyDatabase.saveReports(report)
    }

    fun reportCompanyQuit(player: Player, company: Company) {
        val report = Reports.Staffing.Quit(company.uuid, player.uniqueId)
        plugin.companyDatabase.saveReports(report)
    }


    fun reportCompanyWithdraw(player: Player, company: Company, amount: Double) {
        val report = Reports.Withdraw(company.uuid, player.uniqueId, amount)
        plugin.companyDatabase.saveReports(report)
    }

    fun reportCompanyTransfer(player: Player, company: Company, toPlayer: UUID) {
        val report = Reports.Transfer(company.uuid, player.uniqueId, toPlayer)
        plugin.companyDatabase.saveReports(report)
    }

    fun reportCompanyDeletion(player: Player, company: Company) {
        val report = Reports.Deletion(company.uuid, player.uniqueId)
        plugin.companyDatabase.saveReports(report)
    }


    fun purchasesFromPast(company: Company, time: Long, unit: TimeUnit, onLoad: (List<Reports.Transaction.Purchase.Item>) -> Unit) {
        plugin.companyDatabase.loadReports(PURCHASE_ITEM, System.currentTimeMillis() - (MILLISECONDS.convert(time, unit))) { reports ->
            val output = reports.filterIsInstance<Reports.Transaction.Purchase.Item>().filter { it.idComp == company.uuid }

            onLoad.invoke(output)
        }
    }


    private fun reportPurchase(company: Company, product: Product, buyer: Player, itemStack: ItemStack) {
        val stafferUUID = product.stafferUUID ?: return // should be impossible

        var revenue = product.cost
        val tariffs = ((revenue / 100.0) * company.finance.tariffs)

        // tax product
        revenue -= tariffs

        company.finance.balance = (company.finance.balance + tariffs).formatToTwoPlaces()

        val account = company.finance[stafferUUID]
        val payouts = if (company.staffer.size == 1) revenue else ((revenue / 100.0) * account.payoutRatio)

        account.soldItem(product.cost, payouts)
        plugin.economyHook.attemptGive(stafferUUID, payouts)

        revenue -= payouts

        plugin.messageManager.sendSoldMessageFor(product, company, buyer.name, payouts.formatToTwoPlaces(), tariffs, itemStack)

        if (company.staffer.size == 1 || revenue <= 0.0) {
            val transactions = mapOf(company.uuid to tariffs,
                                     stafferUUID to payouts)

            return reportPurchaseItem(buyer, product, company, transactions)
        }

        revenue /= company.staffer.size - 1

        val remaining = company.staffer.filterNot { it == stafferUUID }

        remaining.forEach {
            company.finance[it].playerPayout += revenue
            plugin.economyHook.attemptGive(it, revenue)
        }

        val transactions = mutableMapOf(company.uuid to tariffs,
                                        stafferUUID to payouts)

        transactions.putAll(remaining.associate { it to revenue })

        reportPurchaseItem(product, company, buyer, transactions)
    }


    inner class Popularity(override val plugin: Companies) : Manager("Popularity") {

        private var update: BukkitTask? = null
        /**
         * score is based on purchases within the past week
         *
         *  - mapped between 0 and 100
         *
         */
        private val scores = ConcurrentHashMap<UUID, Int>()


        operator fun get(uuid: UUID): Int {
            return scores[uuid] ?: -1
        }


        override fun enable() {
            // this is a super heavy operation, lucky for us, it's asynchronous :)
            loadPopularityInfo(this.scores)

            update?.cancel()
            update = repeat((20 * 60) * 5) {
                popularity.pollPopularityInfo()
            }
        }

        override fun disable() {
            scores.clear()
            update?.cancel()
        }


        internal fun pollPopularityInfo() {
            this.scores.clear()
            loadPopularityInfo(this.scores)
        }

        private fun loadPopularityInfo(scores: MutableMap<UUID, Int>) {
            plugin.companyManager.companies.forEach { company ->
                purchasesFromPast(company, 7, TimeUnit.DAYS) {
                    scores[company.uuid] = mapToWithin(it.size.coerceAtMost(200), 0, 200, 0, 100)
                }
            }
        }

    }

}