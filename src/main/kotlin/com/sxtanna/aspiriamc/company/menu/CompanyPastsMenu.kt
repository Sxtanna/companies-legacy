package com.sxtanna.aspiriamc.company.menu

import com.sxtanna.aspiriamc.base.Result.None
import com.sxtanna.aspiriamc.base.Result.Some
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.exts.base64ToItemStack
import com.sxtanna.aspiriamc.exts.buildItemStack
import com.sxtanna.aspiriamc.exts.formatToTwoPlaces
import com.sxtanna.aspiriamc.exts.properName
import com.sxtanna.aspiriamc.menu.Menu
import com.sxtanna.aspiriamc.menu.base.Col
import com.sxtanna.aspiriamc.menu.base.Row
import com.sxtanna.aspiriamc.reports.Reports
import org.bukkit.Material.STRUCTURE_VOID
import java.util.concurrent.TimeUnit

class CompanyPastsMenu(val company: Company, val time: Long, val unit: TimeUnit, val reports: List<Reports.Purchase.Item>, val prevMenu: Menu? = null) : Menu("&a${reports.size} &7Purchases", Row.R_6) {

    private val pagination = ChosenPagination()

    override fun build() {

        val info = buildItemStack(STRUCTURE_VOID) {
            displayName = "&fPurchases from the past &9$time &f${unit.properName()}"

            lore = listOf("",
                          "&7Company: ${company.name}")
        }

        this[Row.R_1, Col.C_5, info] = {}


        val itemSlots = selections
        pagination.page().forEach {
            val (row, col) = slotToGrid(itemSlots.nextInt())

            val item = when (val result = base64ToItemStack(it.base)) {
                is Some -> {
                    result.data
                }
                is None -> {
                    return@forEach
                }
            }

            val icon = buildItemStack(item) {
                lore = listOf(*(if (lore != null) lore else listOf("")).toTypedArray(),
                              "&8&m                       ",
                              "&7Cost: &a$${it.amount}",
                              "&7Sold By: ${it.from.let(company.plugin.stafferManager.names::get)}",
                              "&7Bought By: ${it.into.let(company.plugin.stafferManager.names::get)}",
                              "&8&m                       ",
                              "",
                              "&7Transactions:",
                              *transactionData(it))
            }

            this[row, col, icon] = {}
        }

        pagination.init()

        initBackButton(prevMenu, company.plugin, Row.R_6, Col.C_5)
    }


    private fun companyPurchases(): List<List<Reports.Purchase.Item>> {
        return reports.sortedBy { it.occurredAt }
                       .chunked(36)
                       .takeIf {
                           it.isNotEmpty()
                       } ?: listOf(emptyList())
    }

    private fun transactionData(report: Reports.Purchase.Item): Array<String> {
        return report.transactions.map {
            val name = if (it.key == company.uuid) company.name else company.plugin.stafferManager.names[it.key]
            val data = it.value.formatToTwoPlaces()

            "&f$name: &a+$$data"
        }.toTypedArray()
    }


    inner class ChosenPagination : Pagination<List<Reports.Purchase.Item>>(companyPurchases()) {

        override fun prevCoords() = Row.R_6 to Col.C_4

        override fun nextCoords() = Row.R_6 to Col.C_6

    }


    private companion object {

        private val selections: IntIterator
            get() = (9..44).iterator()

    }

}