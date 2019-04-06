package com.sxtanna.aspiriamc.market.menu

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.PluginDependant.PluginRunnable
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer
import com.sxtanna.aspiriamc.company.menu.CompanyItemsMenu
import com.sxtanna.aspiriamc.company.menu.CompanyVoidsMenu
import com.sxtanna.aspiriamc.config.Garnish.MENU_BUTTON_CLICK
import com.sxtanna.aspiriamc.exts.buildItemStack
import com.sxtanna.aspiriamc.exts.formatToTwoPlaces
import com.sxtanna.aspiriamc.market.sort.CompanySorter
import com.sxtanna.aspiriamc.menu.Menu
import com.sxtanna.aspiriamc.menu.base.Col
import com.sxtanna.aspiriamc.menu.base.Row
import com.sxtanna.aspiriamc.menu.item.UpdatingItemStack
import org.bukkit.Material.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class GlobalCompanyMarketMenu(val plugin: Companies, val prevMenu: Menu? = null) : Menu("&nCompany Marketplace", Row.R_6) {

    private var sponsored = plugin.companyManager.sponsored
    private var companies = plugin.companyManager.companies

    private val pagination = GlobalPagination()
    private val executions = mutableListOf<UpdatingItemStack>()

    override fun build() {
        val sponSlots = sponsoredSlots
        sponsored.forEach {
            if (sponSlots.hasNext().not()) return@forEach // edge case

            val (row, col) = slotToGrid(sponSlots.nextInt())

            val icon: ItemStack = it.createIcon()
            val func: Menu.MenuAction.() -> Unit = {
                plugin.garnishManager.send(who, MENU_BUTTON_CLICK)
                createChosenCompanyMarketMenu(it).open(who)
            }

            if (icon is UpdatingItemStack) {
                icon.extra = {
                    this[row, col, icon] = func
                }

                executions += icon

                icon.update()
            }

            this[row, col, icon] = func
        }

        while (sponSlots.hasNext()) {
            val (row, col) = slotToGrid(sponSlots.nextInt())

            this[row, col, createEmptySponsorSlot()] = {
                plugin.garnishManager.send(who, MENU_BUTTON_CLICK)
                plugin.server.dispatchCommand(who, "company sponsor")
                fresh()
            }
        }

        val compSlots = companiesSlots
        pagination.page().forEach {
            if (compSlots.hasNext().not()) return@forEach // edge case

            val (row, col) = slotToGrid(compSlots.nextInt())

            this[row, col, it.createIcon()] = {
                plugin.garnishManager.send(who, MENU_BUTTON_CLICK)
                createChosenCompanyMarketMenu(it).open(who)
            }
        }

        pagination.init()

        initBackButton(prevMenu, plugin, Row.R_6, Col.C_5)
    }

    override fun fresh() {
        executions.clear()

        sponsored = plugin.companyManager.sponsored
        companies = plugin.companyManager.companies

        pagination.push(allCompanies())

        super.fresh()

        setupCompanyFilter()
        setupItemsButton(inventory.viewers.first() as? Player ?: return)
    }


    override fun open(player: Player) {
        super.open(player)

        setupCompanyFilter()
        setupItemsButton(player)

        instances += this
    }

    override fun onClose(player: Player) {
        super.onClose(player)

        executions.clear()

        instances -= this
    }


    private fun setupCompanyFilter() {
        val button = buildItemStack(HOPPER) {
            setDisplayName("&fFilter items")
        }

        this[Row.R_6, Col.C_1, button] = {
            createFilterCompanyMarketMenu().open(who)
        }
    }

    private fun setupItemsButton(player: Player) {
        val company = plugin.quickAccessCompanyByStafferUUID(player.uniqueId) ?: return setupVoidsButton(player)

        val button = buildItemStack(EMERALD) {
            setDisplayName("&fView Company&e ${company.name}")

            lore = listOf("&7Items Selling: &a${company.product.size}",
                          "&7Employees: &a${company.staffer.size}",
                          "",
                          "&7Total Items Sold: &a${company.finance.account.values.sumBy { it.itemsSold }}",
                          "&7Total Earnings: &a${company.finance.account.values.sumByDouble { it.playerPayout }.formatToTwoPlaces()}")
        }

        this[Row.R_6, Col.C_9, button] = {
            plugin.garnishManager.send(who, MENU_BUTTON_CLICK)
            createCompanyItemsMenu(company).open(who)
        }
    }

    private fun setupVoidsButton(player: Player) {
        val staffer = plugin.quickAccessStaffer(player.uniqueId)?.takeIf { it.voidedItems.isNotEmpty() } ?: return

        val button = buildItemStack(STRUCTURE_VOID) {
            setDisplayName("&fView your voided items")

            lore = listOf(
                "&7Items Voided: &a${staffer.voidedItems.size}"
                         )
        }

        this[Row.R_6, Col.C_9, button] = {
            plugin.garnishManager.send(who, MENU_BUTTON_CLICK)
            createCompanyVoidsMenu(staffer).open(who)
        }
    }


    private fun createCompanyItemsMenu(company: Company): Menu {
        return CompanyItemsMenu(company, this)
    }

    private fun createCompanyVoidsMenu(staffer: Staffer): Menu {
        return CompanyVoidsMenu(plugin, staffer, this)
    }


    private fun createFilterCompanyMarketMenu(): Menu {
        return FilterCompanyMarketMenu.Global(plugin, this)
    }

    private fun createChosenCompanyMarketMenu(company: Company): Menu {
        return ChosenCompanyMarketMenu(plugin, company, this)
    }


    private fun createEmptySponsorSlot(): ItemStack {
        return buildItemStack(GRAY_STAINED_GLASS_PANE) {
            setDisplayName("&eSponsor Slot")
            lore = listOf(
                "",
                "&fBuy a sponsor slot for: &a${plugin.companyManager.sponsorManager.humanReadableTime()}",
                "&7Cost: &a$${plugin.companyManager.sponsorManager.slotCost.formatToTwoPlaces()}"
                         )
        }
    }

    private fun allCompanies(): List<List<Company>> {
        return companies.filter { it.product.isNotEmpty() && it !in sponsored }.sortedWith(CompanySorter.ByPopularity.reversed()).chunked(36).takeIf { it.isNotEmpty() } ?: listOf(emptyList())
    }


    inner class GlobalPagination : Pagination<List<Company>>(allCompanies()) {

        override fun prevCoords() = Row.R_6 to Col.C_4

        override fun nextCoords() = Row.R_6 to Col.C_6

    }


    companion object {

        private val sponsoredSlots: IntIterator
            get() = (0..8).iterator()
        private val companiesSlots: IntIterator
            get() = (9..44).iterator()


        private val instances = mutableSetOf<GlobalCompanyMarketMenu>()

        private var refresher = PluginRunnable {
            instances.forEach {
                it.executions.forEach(UpdatingItemStack::update)
            }
        }

        internal fun loadRefresher(plugin: Companies) {
            refresher.runTaskTimer(plugin, 0L, 20L)
        }

        internal fun killRefresher() {
            refresher.cancel()
            refresher = PluginRunnable {
                instances.forEach {
                    it.executions.forEach(UpdatingItemStack::update)
                }
            }
        }

    }

}