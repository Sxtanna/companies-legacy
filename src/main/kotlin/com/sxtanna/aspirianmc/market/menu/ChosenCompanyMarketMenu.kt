package com.sxtanna.aspirianmc.market.menu

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.base.Result
import com.sxtanna.aspirianmc.base.Result.None
import com.sxtanna.aspirianmc.base.Result.Some
import com.sxtanna.aspirianmc.company.Company
import com.sxtanna.aspirianmc.config.Garnish.MARKET_PRODUCT_PURCHASE_FAIL
import com.sxtanna.aspirianmc.config.Garnish.MARKET_PRODUCT_PURCHASE_PASS
import com.sxtanna.aspirianmc.exts.base64ToItemStack
import com.sxtanna.aspirianmc.exts.itemStackName
import com.sxtanna.aspirianmc.market.Product
import com.sxtanna.aspirianmc.market.sort.ProductSorter
import com.sxtanna.aspirianmc.menu.Menu
import com.sxtanna.aspirianmc.menu.base.Col
import com.sxtanna.aspirianmc.menu.base.Row
import org.bukkit.entity.Player

class ChosenCompanyMarketMenu(val plugin: Companies, val company: Company, val prevMenu: Menu? = null) : Menu("&nMarketplace&r » ${company.name}", Row.R_6) {

    private val pagination = ChosenPagination()

    init {
        instances.computeIfAbsent(company) { mutableListOf() }.add(this)
    }

    override fun build() {
        val itemSlots = productSlots
        pagination.page().sortedWith(ProductSorter.ByCost).forEach {
            val (row, col) = slotToGrid(itemSlots.nextInt())

            this[row, col, it.createDisplayIcon()] = {
                when(val item = attemptPurchaseProduct(it, who).with { base64ToItemStack(it.base) }) {
                    is Some -> {
                        if (who.inventory.addItem(item.data).isNotEmpty()) {
                            reply("&cyour inventory is full")

                            plugin.garnishManager.send(who, MARKET_PRODUCT_PURCHASE_FAIL)
                        }
                        else {
                            company.product -= it

                            reply("&fsuccessfully purchased &e${if (item.data.type.maxStackSize == 1) "" else "${item.data.amount} "}${itemStackName(item.data)}&r for &a$${it.cost}")

                            plugin.reportsManager.reportPurchase(it)
                            plugin.garnishManager.send(who, MARKET_PRODUCT_PURCHASE_PASS)

                            refreshInstances(company)
                        }
                    }
                    is None -> {
                        reply("&cfailed to purchase product: ${item.info}")

                        plugin.garnishManager.send(who, MARKET_PRODUCT_PURCHASE_FAIL)
                    }
                }
            }
        }

        pagination.init()

        initBackButton(prevMenu, plugin, Row.R_6, Col.C_5)
    }

    override fun fresh() {
        pagination.push(companyProducts())

        super.fresh()
    }


    override fun onClose(player: Player) {
        val menus = instances[company] ?: return
        menus.remove(this)

        if (menus.isEmpty()) {
            instances.remove(company)
        }
    }


    private fun companyProducts(): List<List<Product>> {
        return company.product.sortedWith(ProductSorter.ByDate).chunked(45).takeIf { it.isNotEmpty() } ?: listOf(emptyList())
    }


    private fun attemptPurchaseProduct(product: Product, player: Player): Result<Unit> = Result.of {
        if (product.stafferUUID == player.uniqueId) {
            fail("you cannot purchase your own products")
        }

        when (val response = plugin.vaultHook.attemptTake(player, product.cost)) {
            is Some -> when (response.data.transactionSuccess()) {
                true -> {

                }
                else -> {
                    fail(response.data.errorMessage)
                }
            }
            is None -> {
                response.rethrow()
            }
        }
    }


    inner class ChosenPagination : Pagination<List<Product>>(companyProducts()) {

        override fun prevCoords() = Row.R_6 to Col.C_4

        override fun nextCoords() = Row.R_6 to Col.C_6

    }


    private companion object {

        val productSlots: IntIterator
            get() = (0..45).iterator()


        val instances = mutableMapOf<Company, MutableList<ChosenCompanyMarketMenu>>()


        fun refreshInstances(company: Company) {
            instances[company]?.forEach { it.fresh() }
        }

    }

}