package com.sxtanna.aspiriamc.company.menu

import com.sxtanna.aspiriamc.base.Result.None
import com.sxtanna.aspiriamc.base.Result.Some
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.config.Configs.COMPANY_HISTORY_TIME
import com.sxtanna.aspiriamc.config.Configs.COMPANY_HISTORY_UNIT
import com.sxtanna.aspiriamc.config.Garnish.MENU_BUTTON_CLICK
import com.sxtanna.aspiriamc.exts.*
import com.sxtanna.aspiriamc.market.Product
import com.sxtanna.aspiriamc.market.sort.ProductSorter
import com.sxtanna.aspiriamc.menu.Menu
import com.sxtanna.aspiriamc.menu.base.Col
import com.sxtanna.aspiriamc.menu.base.Row
import org.bukkit.Material.*
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType.RIGHT
import org.bukkit.event.inventory.ClickType.SHIFT_RIGHT
import java.util.concurrent.TimeUnit

class CompanyItemsMenu(private val company: Company, val prevMenu: Menu? = null, val adminMode: Boolean = false) : Menu("&nProducts&r &l»&r ${company.name}", Row.R_6) {

    private var clicked = false
    private val pagination = ChosenPagination()

    override fun build() {
        val itemSlots = productSlots
        pagination.page().sortedWith(ProductSorter.ByCost).forEach {
            val (row, col) = slotToGrid(itemSlots.nextInt())

            val item = buildItemStack(it.createIcon()) {
                lore = listOf(*(lore ?: emptyList()).toTypedArray(),
                              "",
                              "&e${if (adminMode) "Shift + Right-Click" else "Right-Click"} &7to ${if (adminMode) "remove" else "stop selling"} this item")
            }

            this[row, col, item] = out@{


                if (!adminMode && who.uniqueId != it.stafferUUID) {
                    return@out // not seller and not admin
                }

                if ((adminMode && how != SHIFT_RIGHT) || (!adminMode && how != RIGHT)) {
                    return@out
                }

                if (!adminMode) {
                    when (val attempt = it.attemptBuy()) {
                        is None -> {
                            return@out reply("&cfailed to reclaim item: ${attempt.info}")
                        }
                    }
                }

                company.plugin.garnishManager.send(who, MENU_BUTTON_CLICK)

                when (val result = base64ToItemStack(it.base)) {
                    is Some -> {
                        if (who.inventoryCanHold(result.data)) {
                            who.inventory.addItem(result.data)

                            company.product -= it

                            company.plugin.reportsManager.reportTakeItem(who, it, company)

                            it.startSellerLock()

                            fresh()
                        } else {
                            reply("&cfailed to ${if (adminMode) "remove" else "reclaim"} item: your inventory is full")
                        }
                    }
                }
            }
        }

        pagination.init()

        val statsButton = buildItemStack(BOOK) {
            setDisplayName("&f${company.name}")

            lore = listOf("&7Items Selling: &a${company.product.size}",
                          "&7Employees: &a${company.staffer.size}",
                          "",
                          "&fMoney",
                          "&7Company Vault: &a${CURRENCIES_FORMAT.format(company.finance.balance)}",
                          "&7Company Tax: &a${company.finance.tariffs}&7%",
                          "&7Total Revenue: &a${CURRENCIES_FORMAT.format(company.finance.account.values.sumByDouble { it.playerProfit })}",
                          "&7Total Employee Earnings: &a${CURRENCIES_FORMAT.format(company.finance.account.values.sumByDouble { it.playerPayout })}")
        }

        this[Row.R_6, Col.C_1, statsButton] = {}

        initBackButton(prevMenu, company.plugin, Row.R_6, Col.C_5)
    }


    override fun open(player: Player) {
        super.open(player)

        setupHistoryButton()
        setupManageButton(player)
    }

    override fun fresh() {
        pagination.push(companyProducts())

        super.fresh()

        clicked = false
        setupHistoryButton()
        setupManageButton(inventory.viewers.first() as? Player ?: return)
    }

    private fun setupHistoryButton() {
        val time = company.plugin.configsManager.get(COMPANY_HISTORY_TIME)
        val unit = company.plugin.configsManager.get(COMPANY_HISTORY_UNIT)

        val button = buildItemStack(SIGN) {
            setDisplayName("&fView past &9$time&f ${unit.properName()} of purchases")
        }

        this[Row.R_6, Col.C_2, button] = out@ {
            if (clicked) return@out

            clicked = true

            createCompanyPastsMenu(who, time, unit)
            company.plugin.garnishManager.send(who, MENU_BUTTON_CLICK)
        }
    }

    private fun setupManageButton(player: Player) {
        if (company.isOwner(player.uniqueId).not()) return

        val button = buildItemStack(REPEATING_COMMAND_BLOCK) {
            setDisplayName("&fManage Company&e ${company.name}")
        }

        this[Row.R_6, Col.C_9, button] = {
            createCompanyAdminMenu().open(who)
            company.plugin.garnishManager.send(who, MENU_BUTTON_CLICK)
        }
    }


    private fun companyProducts(): List<List<Product>> {
        return company.product.sortedWith(ProductSorter.ByDate).chunked(45).takeIf { it.isNotEmpty() } ?: listOf(emptyList())
    }

    private fun createCompanyAdminMenu(): Menu {
        return CompanyAdminMenu(company, this)
    }

    private fun createCompanyPastsMenu(player: Player, time: Long, unit: TimeUnit) {
        company.plugin.reportsManager.purchasesFromPast(company, time, unit) {
            CompanyPastsMenu(company, time, unit, it, this).open(player)
        }
    }


    inner class ChosenPagination : Pagination<List<Product>>(companyProducts()) {

        override fun prevCoords() = Row.R_6 to Col.C_4

        override fun nextCoords() = Row.R_6 to Col.C_6

    }


    companion object {

        private val productSlots: IntIterator
            get() = (0..44).iterator()

    }

}