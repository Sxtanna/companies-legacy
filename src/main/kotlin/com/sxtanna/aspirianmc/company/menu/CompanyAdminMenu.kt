package com.sxtanna.aspirianmc.company.menu

import com.sxtanna.aspirianmc.company.Company
import com.sxtanna.aspirianmc.config.Garnish.COMPANY_PAYOUTS_CHANGE
import com.sxtanna.aspirianmc.config.Garnish.COMPANY_PAYOUTS_RESET
import com.sxtanna.aspirianmc.exts.buildItemStack
import com.sxtanna.aspirianmc.menu.Menu
import com.sxtanna.aspirianmc.menu.base.Col
import com.sxtanna.aspirianmc.menu.base.Row
import org.bukkit.Material.PLAYER_HEAD
import org.bukkit.Material.REDSTONE_BLOCK
import org.bukkit.event.inventory.ClickType.MIDDLE
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

class CompanyAdminMenu(private val company: Company, val prevMenu: Menu? = null) : Menu("&nManage&r » ${company.name}", Row.R_4) {

    private val pagination = StafferPagination()


    private val resetButton = buildItemStack(REDSTONE_BLOCK) {
        displayName = "&eEmployee Percentages"
        lore = listOf(
                "&7Employee percentages determine what",
                "&7amount of money an employee gets when",
                "&7their item sells. The rest of the money",
                "&7is split equally to all the employees.",
                "",
                "&eMiddle Mouse Button Click",
                "&7to reset all employee percentages."
        )
    }

    private val resetAction = { action: MenuAction ->
        company.finance.resetPayoutRatios()
        fresh()

        company.plugin.garnishManager.send(action.who, COMPANY_PAYOUTS_RESET)
    }


    override fun build() {
        val stafferSlots = staffSlots
        pagination.page().forEach {
            val (row, col) = slotToGrid(stafferSlots.nextInt())

            this[row, col, playerHeadIcon(it)] = {
                val account = company.finance[it]
                val payouts = account.payoutRatio

                when {
                    how.isLeftClick -> {
                        account.payoutRatio = (payouts - if (how.isShiftClick) 10 else 5).coerceAtLeast(0)
                    }
                    how.isRightClick -> {
                        account.payoutRatio = (payouts + if (how.isShiftClick) 10 else 5).coerceAtMost(100)
                    }
                }

                if (payouts != account.payoutRatio) {
                    fresh()

                    company.plugin.garnishManager.send(who, COMPANY_PAYOUTS_CHANGE)
                }
            }
        }

        pagination.init()

        this[Row.R_4, Col.C_9, resetButton] = {
            if (how == MIDDLE) resetAction.invoke(this)
        }

        initBackButton(prevMenu, company.plugin, Row.R_4, Col.C_5)
    }

    override fun fresh() {
        pagination.push(allStaffers())

        super.fresh()
    }


    private fun playerHeadIcon(uuid: UUID): ItemStack {
        val account = company.finance[uuid]
        val display = company.plugin.stafferManager.names[uuid]

        return buildItemStack(PLAYER_HEAD) {
            displayName = "&f$display "
            lore = listOf(
                    "&7Items Sold: &a${account.itemsSold}",
                    "&7Items Selling: &a${company.product.count { it.stafferUUID ==  uuid }}",
                    "",
                    "&7Payout Ratio: &a${account.payoutRatio}&7%",
                    "&7Player Earnings: &a$${account.playerProfit}",
                    "&7Player Earnings for Company: &a$${account.playerPayout}",
                    "",
                    "&eLeft-Click&7 to decrease payout percentage",
                    "&eRight-Click&7 to increase payout percentage"
            )

            (this as SkullMeta).owner = display
        }
    }

    private fun allStaffers(): List<List<UUID>> {
        return company.staffer.chunked(7)
    }


    inner class StafferPagination : Pagination<List<UUID>>(allStaffers()) {

        override fun prevCoords() = Row.R_2 to Col.C_1

        override fun nextCoords() = Row.R_2 to Col.C_9

    }


    companion object {

        private val staffSlots: IntIterator
            get() = (1..7).iterator()

    }

}