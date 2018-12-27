package com.sxtanna.aspiriamc.market.menu

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.PluginDependant
import com.sxtanna.aspiriamc.base.PluginDependant.PluginTask.Cont.ASync
import com.sxtanna.aspiriamc.base.PluginDependant.PluginTask.Cont.Sync
import com.sxtanna.aspiriamc.base.Result
import com.sxtanna.aspiriamc.base.Result.None
import com.sxtanna.aspiriamc.base.Result.Some
import com.sxtanna.aspiriamc.base.Searchable.Query
import com.sxtanna.aspiriamc.base.Searchable.Query.CostQuery.CostType.ABOVE
import com.sxtanna.aspiriamc.base.Searchable.Query.CostQuery.CostType.BELOW
import com.sxtanna.aspiriamc.base.Searchable.Query.DataQuery.Kinds
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.config.Garnish.*
import com.sxtanna.aspiriamc.exts.*
import com.sxtanna.aspiriamc.market.Product
import com.sxtanna.aspiriamc.market.sort.ProductSorter
import com.sxtanna.aspiriamc.menu.Menu
import com.sxtanna.aspiriamc.menu.base.Col
import com.sxtanna.aspiriamc.menu.base.Row
import com.sxtanna.aspiriamc.menu.chat.ChatMenu
import com.sxtanna.aspiriamc.menu.impl.ConfirmationMenu
import me.tom.sparse.spigot.chat.menu.element.ButtonElement
import me.tom.sparse.spigot.chat.menu.element.InputElement
import me.tom.sparse.spigot.chat.menu.element.TextElement
import org.bukkit.DyeColor
import org.bukkit.DyeColor.*
import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag.*

sealed class FilterCompanyMarketMenu(target: String) : Menu("&nFiltering&r &l»&r $target", Row.R_6), PluginDependant {

    protected val queries = mutableListOf<Query>()


    private val name = Query.NameQuery("")
    private val type = Query.TypeQuery(AIR)
    private val cost = Query.CostQuery(-1.0, BELOW)

    private val count = Query.DataQuery.Count(-1)
    private val color = Query.DataQuery.Color(null)

    private val isBlock = Query.DataQuery.Kinds.IsBlock()
    private val isFoods = Query.DataQuery.Kinds.IsFoods()
    private val isItems = Query.DataQuery.Kinds.IsItems()
    private val isFuels = Query.DataQuery.Kinds.IsFuels()
    private val isDiscs = Query.DataQuery.Kinds.IsDiscs()


    init {
        queries += name
        queries += type
        queries += cost

        queries += count
        queries += color

        queries += isBlock
        queries += isFoods
        queries += isItems
        queries += isFuels
        queries += isDiscs
    }


    override fun build() {
        createCategories()
    }


    private fun createCategories() {
        this[Row.R_2, Col.C_2, name.createIcon()] = {
            who.closeInventory()
            NameSelectionMenu(plugin).open(who)
        }
        this[Row.R_2, Col.C_3, type.createIcon()] = {
            TypeSelectionMenu(plugin).open(who)
        }
        this[Row.R_3, Col.C_2, cost.createIcon()] = {
            CostSelectionMenu(plugin).open(who)
        }

        this[Row.R_2, Col.C_5, count.createIcon()] = {
            CountSelectionMenu(plugin).open(who)
        }
        this[Row.R_2, Col.C_6, color.createIcon()] = {
            ColorSelectionMenu(plugin).open(who)
        }

        val kinds = buildItemStack(COMMAND_BLOCK) {
            displayName = "&eItem Options"

            addItemFlags(HIDE_ENCHANTS,
                         HIDE_ATTRIBUTES,
                         HIDE_UNBREAKABLE,
                         HIDE_DESTROYS,
                         HIDE_PLACED_ON,
                         HIDE_POTION_EFFECTS)

            val kinds = queries.filterIsInstance<Kinds>().filter(Query::enabled)

            if (kinds.isNotEmpty()) {
                addEnchant(Enchantment.DAMAGE_ALL, 1, true)

                lore = listOf("",
                              *kinds.map { "&f${it.name}" }.toTypedArray())
            }
        }

        this[Row.R_2, Col.C_8, kinds] = {
            KindsSelectionMenu(plugin).open(who)
        }
    }


    abstract fun executeSearch(player: Player)


    protected fun initExecButton() {
        var runnable = false

        val execButton = buildItemStack(EMERALD_BLOCK) {
            displayName = "&fExecute filter"

            addItemFlags(HIDE_ENCHANTS,
                         HIDE_ATTRIBUTES,
                         HIDE_UNBREAKABLE,
                         HIDE_DESTROYS,
                         HIDE_PLACED_ON,
                         HIDE_POTION_EFFECTS)

            val enabled = queries.filter(Query::enabled)

            if (enabled.isNotEmpty()) {
                runnable = true
                addEnchant(Enchantment.DAMAGE_ALL, 1, true)

                lore = listOf("",
                              *enabled.flatMap { it.createLore() }.toTypedArray())
            }
        }

        this[Row.R_5, Col.C_5, execButton] = {
            if (runnable.not()) {
                // fail sound?
            }
            else {
                executeSearch(who)
                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
        }
    }


    class Global(override val plugin: Companies, val prevMenu: Menu? = null) : FilterCompanyMarketMenu("All Companies") {

        override fun build() {
            super.build()

            initExecButton()
            initBackButton(prevMenu, plugin, Row.R_6, Col.C_5)
        }


        override fun executeSearch(player: Player) {
            task(ASync) {
                plugin.companyManager.companies.flatMap { company ->
                    company.product.filter { product ->
                        queries.all { product.passes(it) }
                    }
                }
            }.then(Sync) {
                Output(plugin, it.toMutableList(), this).open(player)
            }.exec()
        }

    }

    class Chosen(override val plugin: Companies, val company: Company, val prevMenu: Menu? = null) : FilterCompanyMarketMenu(company.name) {

        override fun build() {
            super.build()

            initExecButton()
            initBackButton(prevMenu, plugin, Row.R_6, Col.C_5)
        }


        override fun executeSearch(player: Player) {
            task(ASync) {
                company.product.filter { product ->
                    queries.all { product.passes(it) }
                }
            }.then(Sync) {
                Output(plugin, it.toMutableList(), this).open(player)
            }.exec()
        }

    }

    class Output(override val plugin: Companies, private val products: MutableList<Product>, val prevMenu: Menu? = null) : Menu("&a${products.size}&7 Results", Row.R_6), PluginDependant {

        private val pagination = ChosenPagination()


        override fun build() {
            val itemSlots = selections
            pagination.page().sortedWith(ProductSorter.ByCost).forEach {
                val (row, col) = slotToGrid(itemSlots.nextInt())

                val icon = it.createIcon()

                icon.itemMeta = icon.itemMeta.apply {
                    lore = listOf(*lore.toTypedArray(),
                                  color("&7Company: ${plugin.quickAccessCompanyByCompanyUUID(it.companyUUID)?.name ?: "Unknown"}"))
                }

                this[row, col, icon] = out@{

                    if (it.canNotBuyDecide) {
                        return@out reply("&cfailed to purchase product: someone else is deciding on it")
                    }
                    if (it.canNotBuyBought) {
                        return@out reply("&cfailed to purchase product: someone else has already bought it")
                    }
                    if (it.stafferUUID == who.uniqueId) {
                        return@out reply("&cfailed to purchase product: you cannot purchase your own products")
                    }

                    val confirmation = object : ConfirmationMenu("Cost: &a$${it.cost}") {

                        override fun passLore(): List<String> {
                            return listOf(
                                "",
                                "&7Buy &a${icon.itemMeta.displayName} &7for &a$${it.cost}"
                                         )
                        }

                        override fun failLore(): List<String> {
                            return listOf(
                                "",
                                "&7Stop buying item"
                                         )
                        }


                        override fun onPass(action: MenuAction) {
                            when (val item = attemptPurchaseProduct(it, who).with { base64ToItemStack(it.base) }) {
                                is Some -> {
                                    if (who.inventoryCanHold(item.data)) {
                                        who.inventory.addItem(item.data)

                                        products.remove(it)
                                        plugin.quickAccessCompanyByCompanyUUID(it.companyUUID)?.product?.remove(it)

                                        plugin.reportsManager.reportPurchase(it, who, item.data)
                                        plugin.garnishManager.send(who, MARKET_PRODUCT_PURCHASE_PASS)

                                        this@Output.fresh()
                                        this@Output.open(action.who)

                                        reply("&fsuccessfully purchased &e${if (item.data.type.maxStackSize == 1) "" else "${item.data.amount} "}${itemStackName(item.data)}&r for &a$${it.cost}")
                                        return
                                    } else {
                                        plugin.economyHook.attemptGive(who.uniqueId, it.cost)

                                        plugin.garnishManager.send(who, MARKET_PRODUCT_PURCHASE_FAIL)

                                        reply("&cfailed to purchase product: your inventory is full")
                                    }
                                }
                                is None -> {
                                    reply("&cfailed to purchase product: ${item.info}")

                                    plugin.garnishManager.send(who, MARKET_PRODUCT_PURCHASE_FAIL)
                                }
                            }

                            action.who.closeInventory()
                            it.canNotBuyDecide = false
                        }

                        override fun onFail(action: MenuAction) {
                            this@Output.open(action.who)
                            it.canNotBuyDecide = false
                        }


                        override fun onClose(player: Player) {
                            it.canNotBuyDecide = false
                        }

                    }


                    it.canNotBuyDecide = true
                    confirmation.open(who)
                }
            }

            pagination.init()

            initBackButton(prevMenu, plugin, Row.R_6, Col.C_5)
        }

        override fun fresh() {
            pagination.push(companyProducts())

            super.fresh()
        }


        private fun companyProducts(): List<List<Product>> {
            return products.sortedWith(ProductSorter.ByDate)
                           .chunked(45)
                           .takeIf {
                               it.isNotEmpty()
                           } ?: listOf(emptyList())
        }

        private fun attemptPurchaseProduct(product: Product, player: Player): Result<Unit> = Result.of {
            when (val response = plugin.economyHook.attemptTake(player, product.cost)) {
                is Some -> { /* do nothing, handled elsewhere */
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

    }


    inner class NameSelectionMenu(val plugin: Companies) : ChatMenu() {

        override fun build() {
            +TextElement("Name: ", 0, 16)
            val input = +InputElement(35, 16, 80, name.name.takeIf { it.isNotBlank() } ?: "")

            val accept = { player: Player ->
                name.name = input.getValue() ?: ""
                name.enabled = name.name.isNotBlank()

                this@FilterCompanyMarketMenu.open(player)
                this@FilterCompanyMarketMenu.fresh()

                plugin.garnishManager.send(player, MENU_BUTTON_BACK)
                kill(player)
            }
            val cancel = { player: Player ->
                this@FilterCompanyMarketMenu.open(player)
                this@FilterCompanyMarketMenu.fresh()

                plugin.garnishManager.send(player, MENU_BUTTON_BACK)
                kill(player)
            }
            val resets = { player: Player ->
                name.name = ""
                name.enabled = false

                this@FilterCompanyMarketMenu.open(player)
                this@FilterCompanyMarketMenu.fresh()

                plugin.garnishManager.send(player, MENU_BUTTON_BACK)
                kill(player)
            }

            +ButtonElement(0, 19, color("&a[Accept]"), accept)
            +ButtonElement(50, 19, color("&c[Cancel]"), cancel)
            +ButtonElement(100, 19, color("&4[Reset]"), resets)
        }

        override fun open(player: Player) {
            super.open(player)

            /*val input = menu.elements.find { it is InputElement } as? InputElement ?: return

            delay(5) {
                val command = menu.getCommand(input)
                player.chat(command)
            }*/
        }

    }

    inner class TypeSelectionMenu(val plugin: Companies) : Menu("&nSelect The Type", Row.R_6) {

        private val pagination = TypePagination()


        override fun build() {
            val itemSlots = selections
            pagination.page().forEach {
                val (row, col) = slotToGrid(itemSlots.nextInt())

                val icon = buildItemStack(it) {
                    displayName = "&f${it.properName()}"

                    addItemFlags(HIDE_ENCHANTS,
                                 HIDE_ATTRIBUTES,
                                 HIDE_UNBREAKABLE,
                                 HIDE_DESTROYS,
                                 HIDE_PLACED_ON,
                                 HIDE_POTION_EFFECTS)
                }

                this[row, col, icon] = {
                    type.type = it
                    type.enabled = true

                    this@FilterCompanyMarketMenu.open(who)
                    this@FilterCompanyMarketMenu.fresh()

                    plugin.garnishManager.send(who, MENU_BUTTON_BACK)
                }
            }

            pagination.init()

            initBackButton(this@FilterCompanyMarketMenu, plugin, Row.R_6, Col.C_5)

            val clearItem = buildItemStack(REDSTONE_BLOCK) {
                displayName = "&cReset to nothing"
            }

            this[Row.R_6, Col.C_1, clearItem] = {
                type.type = AIR
                type.enabled = false

                this@FilterCompanyMarketMenu.open(who)
                this@FilterCompanyMarketMenu.fresh()

                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
        }


        private fun materialTypes(): List<List<Material>> {
            return Material.values()
                           .filter {
                               it != AIR && it.isItem && it.isLegacy.not()
                           }
                           .chunked(45)
                           .takeIf {
                               it.isNotEmpty()
                           } ?: listOf(emptyList())
        }


        inner class TypePagination : Pagination<List<Material>>(materialTypes()) {

            override fun prevCoords() = Row.R_6 to Col.C_4

            override fun nextCoords() = Row.R_6 to Col.C_6

        }

    }

    inner class CostSelectionMenu(val plugin: Companies) : Menu("&nSelect The Cost", Row.R_6) {

        override fun build() {
            refreshDisplay()

            val numbers = Array(10) {
                buildItemStack(GRAY_DYE, 1) {
                    displayName = "&f&l$it"
                }
            }

            val iter = numbers.reversedArray().iterator()

            this[Row.R_2, Col.C_6, iter.next()] = {
                appendDigit(9)
                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
            this[Row.R_2, Col.C_7, iter.next()] = {
                appendDigit(8)
                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
            this[Row.R_2, Col.C_8, iter.next()] = {
                appendDigit(7)
                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }

            this[Row.R_3, Col.C_6, iter.next()] = {
                appendDigit(6)
                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
            this[Row.R_3, Col.C_7, iter.next()] = {
                appendDigit(5)
                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
            this[Row.R_3, Col.C_8, iter.next()] = {
                appendDigit(4)
                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }

            this[Row.R_4, Col.C_6, iter.next()] = {
                appendDigit(3)
                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
            this[Row.R_4, Col.C_7, iter.next()] = {
                appendDigit(2)
                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
            this[Row.R_4, Col.C_8, iter.next()] = {
                appendDigit(1)
                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }

            this[Row.R_5, Col.C_7, iter.next()] = {
                appendDigit(0)
                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }


            val cancel = buildItemStack(BARRIER) {
                displayName = "&cCancel"
            }

            val delete = buildItemStack(REDSTONE_BLOCK) {
                displayName = "&cBackspace"
            }
            val accept = buildItemStack(EMERALD_BLOCK) {
                displayName = "&aAccept"
            }

            this[Row.R_6, Col.C_1, cancel] = {
                cost.cost = -1.0
                cost.enabled = false

                this@FilterCompanyMarketMenu.open(who)
                this@FilterCompanyMarketMenu.fresh()

                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }

            this[Row.R_6, Col.C_6, delete] = {
                val text = cost.cost.toLong().toString()

                cost.cost = if (text.isEmpty()) {
                    0.0
                } else {
                    text.dropLast(1).toDoubleOrNull() ?: 0.0
                }

                plugin.garnishManager.send(who, MENU_BUTTON_BACK)

                refreshDisplay()
            }
            this[Row.R_6, Col.C_8, accept] = {
                cost.enabled = true

                this@FilterCompanyMarketMenu.open(who)
                this@FilterCompanyMarketMenu.fresh()

                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
        }

        override fun open(player: Player) {
            if (cost.cost == -1.0) {
                cost.cost = 0.0
            }

            super.open(player)
        }


        private fun refreshDisplay() {
            val display = buildItemStack(GHAST_TEAR) {
                displayName = "&fCurrent"

                lore = listOf("",
                              "&f${if (cost.type == ABOVE) "More Than" else " Less Than"}",
                              "  &a$${cost.cost}")
            }

            this[Row.R_3, Col.C_3, display] = {}

            val direct = buildItemStack(LAPIS_BLOCK) {
                displayName = "&bToggle Direction"

                lore = listOf("",
                              "&f${if (cost.type == ABOVE) "More Than" else "Less Than"}")
            }

            this[Row.R_4, Col.C_3, direct] = {
                cost.type = when (cost.type) {
                    ABOVE -> BELOW
                    BELOW -> ABOVE
                }

                refreshDisplay()
            }
        }

        private fun appendDigit(number: Int) {
            val text = cost.cost.toLong().toString()

            cost.cost = if (text.isEmpty()) {
                number.toDouble()
            } else {
                "$text$number".toDoubleOrNull() ?: cost.cost
            }

            refreshDisplay()
        }

    }


    inner class CountSelectionMenu(val plugin: Companies) : Menu("&nSelect The Count", Row.R_3) {

        override fun build() {
            refreshDisplay()

            val decrease = buildItemStack(RED_WOOL) {
                displayName = "&fDecrease Count"
            }
            val increase = buildItemStack(GREEN_WOOL) {
                displayName = "&fIncrease Count"
            }

            this[Row.R_2, Col.C_4, decrease] = {
                count.count = (count.count - 1).coerceAtLeast(1)
                refreshDisplay()
            }
            this[Row.R_2, Col.C_6, increase] = {
                count.count = (count.count + 1).coerceAtMost(64)
                refreshDisplay()
            }

            val cancel = buildItemStack(REDSTONE_BLOCK) {
                displayName = "&cCancel filter"
            }
            val accept = buildItemStack(EMERALD_BLOCK) {
                displayName = "&aAccept filter"
            }

            this[Row.R_3, Col.C_1, cancel] = {
                count.count = -1

                count.enabled = false

                this@FilterCompanyMarketMenu.open(who)
                this@FilterCompanyMarketMenu.fresh()

                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
            this[Row.R_3, Col.C_9, accept] = {
                count.enabled = true

                this@FilterCompanyMarketMenu.open(who)
                this@FilterCompanyMarketMenu.fresh()

                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
        }

        override fun open(player: Player) {
            super.open(player)

            if (count.count == -1) {
                count.count = 1
            }
        }

        private fun refreshDisplay() {
            val display = buildItemStack(STONE, count.count.coerceIn(1, 64)) {
                displayName = "&fCurrent"
            }

            this[Row.R_1, Col.C_5, display] = {}
        }


    }

    inner class ColorSelectionMenu(val plugin: Companies) : Menu("&nSelect The Color", Row.R_6) {

        private val colors = DyeColor.values().associate {
            it to buildItemStack(Material.getMaterial("${it.name}_WOOL")) {
                displayName = "&f${it.properName()}"
            }
        }


        override fun build() {
            this[Row.R_2, Col.C_2] = PINK
            this[Row.R_3, Col.C_2] = MAGENTA
            this[Row.R_4, Col.C_2] = PURPLE

            this[Row.R_2, Col.C_3] = YELLOW
            this[Row.R_3, Col.C_3] = ORANGE
            this[Row.R_4, Col.C_3] = RED

            this[Row.R_2, Col.C_5] = LIME
            this[Row.R_3, Col.C_5] = GREEN
            this[Row.R_4, Col.C_5] = BROWN

            this[Row.R_2, Col.C_6] = LIGHT_BLUE
            this[Row.R_3, Col.C_6] = CYAN
            this[Row.R_4, Col.C_6] = BLUE

            this[Row.R_2, Col.C_8] = WHITE
            this[Row.R_3, Col.C_8] = LIGHT_GRAY
            this[Row.R_4, Col.C_8] = GRAY
            this[Row.R_5, Col.C_8] = BLACK


            initBackButton(this@FilterCompanyMarketMenu, plugin, Row.R_6, Col.C_5)

            val clearItem = buildItemStack(REDSTONE_BLOCK) {
                displayName = "&cReset to nothing"
            }

            this[Row.R_6, Col.C_1, clearItem] = {
                color.color = null
                color.enabled = false

                this@FilterCompanyMarketMenu.open(who)
                this@FilterCompanyMarketMenu.fresh()

                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
        }


        private operator fun set(row: Row, col: Col, dye: DyeColor) {
            val item = colors[dye] ?: return

            this[row, col, item] = {
                color.color = dye
                color.enabled = true

                this@FilterCompanyMarketMenu.open(who)
                this@FilterCompanyMarketMenu.fresh()

                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
        }

    }


    inner class KindsSelectionMenu(val plugin: Companies) : Menu("&nSelect The Options", Row.R_4) {

        override fun build() {
            this[Row.R_2, Col.C_3, isItems.createIcon()] = {
                isItems.enabled = isItems.enabled.not()
                fresh()
            }
            this[Row.R_2, Col.C_4, isBlock.createIcon()] = {
                isBlock.enabled = isBlock.enabled.not()
                fresh()
            }
            this[Row.R_2, Col.C_5, isFoods.createIcon()] = {
                isFoods.enabled = isFoods.enabled.not()
                fresh()
            }
            this[Row.R_2, Col.C_6, isFuels.createIcon()] = {
                isFuels.enabled = isFuels.enabled.not()
                fresh()
            }
            this[Row.R_2, Col.C_7, isDiscs.createIcon()] = {
                isDiscs.enabled = isDiscs.enabled.not()
                fresh()
            }

            initBackButton(this@FilterCompanyMarketMenu, plugin, Row.R_4, Col.C_5)

            val clearItem = buildItemStack(REDSTONE_BLOCK) {
                displayName = "&cReset to nothing"
            }

            this[Row.R_4, Col.C_1, clearItem] = {
                isItems.enabled = false
                isBlock.enabled = false
                isFoods.enabled = false
                isFuels.enabled = false
                isDiscs.enabled = false

                this@FilterCompanyMarketMenu.open(who)
                this@FilterCompanyMarketMenu.fresh()

                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
        }

    }


    private companion object {

        private val selections: IntIterator
            get() = (0..44).iterator()

    }

}