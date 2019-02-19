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
import org.bukkit.inventory.ItemStack

sealed class FilterCompanyMarketMenu(target: String) : Menu("&nFiltering&r &l»&r $target", Row.R_6), PluginDependant {

    protected val queries = mutableListOf<Query>()


    private val name = Query.NameQuery()
    private val type = Query.TypeQuery()
    private val cost = Query.CostQuery()

    private val count = Query.DataQuery.Count()
    private val color = Query.DataQuery.Color()
    private val chant = Query.DataQuery.Chant()

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
        queries += chant

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
            hideEverything()

            displayName = "&eItem Options"

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

        this[Row.R_3, Col.C_8, chant.createIcon()] = {
            ChantSelectionMenu(plugin).open(who)
        }
    }


    abstract fun executeSearch(player: Player)


    protected fun initExecButton() {
        var runnable = false

        val execButton = buildItemStack(EMERALD_BLOCK) {
            hideEverything()

            displayName = "&fExecute filter"

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
            } else {
                executeSearch(who)
                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
        }
    }


    class Global(override val plugin: Companies, private val prevMenu: Menu? = null) : FilterCompanyMarketMenu("All Companies") {

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

    class Chosen(override val plugin: Companies, val company: Company, private val prevMenu: Menu? = null) : FilterCompanyMarketMenu(company.name) {

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

    class Output(override val plugin: Companies, private val products: MutableList<Product>, private val prevMenu: Menu? = null) : Menu("&a${products.size}&7 Results", Row.R_6), PluginDependant {

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

                    if (it.stafferUUID == who.uniqueId) {
                        return@out reply("&cfailed to purchase product: you cannot purchase your own products")
                    }

                    when(val attempt = it.attemptBuy()) {
                        is None -> {
                            return@out reply("&cfailed to purchase product: ${attempt.info}")
                        }
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
                            it.closeDecideLock()
                        }

                        override fun onFail(action: MenuAction) {
                            this@Output.open(action.who)
                            it.closeDecideLock()
                        }


                        override fun onClose(player: Player) {
                            it.closeDecideLock()
                        }

                    }


                    it.startDecideLock()
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
                    hideEverything()

                    displayName = "&f${it.properName()}"
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
            if (count.count == -1) {
                count.count = 1
            }

            super.open(player)
        }

        private fun refreshDisplay() {
            val display = buildItemStack(STONE, count.count.coerceIn(1, 64)) {
                displayName = "&fCurrent"
                lore = listOf("  &a${count.count}")
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


    inner class ChantSelectionMenu(val plugin: Companies) : Menu("&nSelect The Enchantments", Row.R_6) {

        private val tool = ToolTypeMenu()
        private val rods = RodsTypeMenu()

        private val head = HeadTypeMenu()
        private val body = BodyTypeMenu()
        private val feet = FeetTypeMenu()

        private val weap = WeapTypeMenu()
        private val bows = BowsTypeMenu()
        private val trid = TridTypeMenu()

        private val misc = MiscTypeMenu()


        override fun build() {
            createTypeButton(Row.R_2, Col.C_2, tool)
            createTypeButton(Row.R_3, Col.C_2, rods)

            createTypeButton(Row.R_2, Col.C_4, head)
            createTypeButton(Row.R_3, Col.C_4, body)
            createTypeButton(Row.R_4, Col.C_4, feet)

            createTypeButton(Row.R_2, Col.C_6, weap)
            createTypeButton(Row.R_3, Col.C_6, bows)
            createTypeButton(Row.R_4, Col.C_6, trid)

            createTypeButton(Row.R_2, Col.C_8, misc)

            initBackButton(this@FilterCompanyMarketMenu, plugin, Row.R_6, Col.C_5)

            val clearItem = buildItemStack(REDSTONE_BLOCK) {
                displayName = "&cReset to nothing"
            }

            this[Row.R_6, Col.C_1, clearItem] = {
                chant.chant.clear()
                chant.enabled = false

                this@FilterCompanyMarketMenu.open(who)
                this@FilterCompanyMarketMenu.fresh()

                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
        }


        private fun createTypeButton(row: Row, col: Col, typeMenu: ChantTypeMenu) {
            this[row, col, typeMenu.createIcon()] = {
                typeMenu.open(who)
            }
        }


        abstract inner class ChantTypeMenu(type: String, rows: Row) : Menu("&n$type Enchantments", rows) {

            abstract override fun build()

            abstract fun createIcon(): ItemStack


            protected fun createEnchantButton(row: Row, col: Col, enchantment: Enchantment) {
                this[row, col, chant.createIconFor(enchantment)] = {
                    executeChantClick(enchantment, who)
                }
            }

            protected fun createTypeIcon(type: Material, name: String, vararg enchantments: Enchantment): ItemStack {
                return buildItemStack(type) {
                    hideEverything()

                    displayName = "&e$name"

                    val loreLines = loreLinesFor(*enchantments)

                    if (loreLines.isNotEmpty()) {
                        lore = loreLines

                        addEnchant(Enchantment.DAMAGE_ALL, 1, true)
                    }
                }
            }

            protected fun initNoneButton(menu: Menu, row: Row, col: Col, vararg enchantments: Enchantment) {
                val clearItem = buildItemStack(REDSTONE_BLOCK) {
                    displayName = "&cReset to nothing"
                }

                this[row, col, clearItem] = {
                    enchantments.forEach {
                        chant.chant -= it
                    }

                    chant.enabled = chant.chant.isNotEmpty()

                    menu.open(who)
                    menu.fresh()

                    plugin.garnishManager.send(who, MENU_BUTTON_BACK)
                }
            }

            private fun executeChantClick(enchantment: Enchantment, player: Player) {
                if (enchantment.maxLevel > 1) {
                    return ChantLevelMenu(enchantment, this).open(player)
                }

                if (chant.chant.remove(enchantment) == null) {
                    chant.chant[enchantment] = 1
                }

                chant.enabled = chant.chant.isNotEmpty()

                fresh()
            }

            private fun loreLinesFor(vararg enchantments: Enchantment): List<String> {
                val levels = enchantments.associate { it to chant.chant[it] }.mapNotNull { data ->
                    data.value?.takeIf { it > 0 }?.let {
                        data.key to it
                    }
                }

                return if (levels.isEmpty()) {
                    emptyList()
                } else {
                    listOf("",
                           *levels.map { "&7${it.first.properName()}: &a${it.second}" }.toTypedArray())
                }
            }

        }


        inner class ToolTypeMenu : ChantTypeMenu("Tool", Row.R_3) {

            override fun build() {
                createEnchantButton(Row.R_2, Col.C_3, Enchantment.SILK_TOUCH)
                createEnchantButton(Row.R_2, Col.C_5, Enchantment.DIG_SPEED)
                createEnchantButton(Row.R_2, Col.C_7, Enchantment.LOOT_BONUS_BLOCKS)

                initBackButton(this@ChantSelectionMenu, plugin, Row.R_3, Col.C_1)
                initNoneButton(this@ChantSelectionMenu, Row.R_3, Col.C_9, Enchantment.SILK_TOUCH, Enchantment.DIG_SPEED, Enchantment.LOOT_BONUS_BLOCKS)
            }

            override fun createIcon(): ItemStack {
                return createTypeIcon(DIAMOND_PICKAXE, "Tools", Enchantment.SILK_TOUCH, Enchantment.DIG_SPEED, Enchantment.LOOT_BONUS_BLOCKS)
            }

        }

        inner class RodsTypeMenu : ChantTypeMenu("Fishing Rod", Row.R_3) {

            override fun build() {
                createEnchantButton(Row.R_2, Col.C_4, Enchantment.LURE)
                createEnchantButton(Row.R_2, Col.C_6, Enchantment.LUCK)

                initBackButton(this@ChantSelectionMenu, plugin, Row.R_3, Col.C_1)
                initNoneButton(this@ChantSelectionMenu, Row.R_3, Col.C_9, Enchantment.LURE, Enchantment.LUCK)
            }

            override fun createIcon(): ItemStack {
                return createTypeIcon(FISHING_ROD, "Fishing Rods", Enchantment.LURE, Enchantment.LUCK)
            }

        }


        inner class HeadTypeMenu : ChantTypeMenu("Helmet", Row.R_3) {

            override fun build() {
                createEnchantButton(Row.R_2, Col.C_4, Enchantment.WATER_WORKER)
                createEnchantButton(Row.R_2, Col.C_6, Enchantment.OXYGEN)

                initBackButton(this@ChantSelectionMenu, plugin, Row.R_3, Col.C_1)
                initNoneButton(this@ChantSelectionMenu, Row.R_3, Col.C_9, Enchantment.WATER_WORKER, Enchantment.OXYGEN)
            }

            override fun createIcon(): ItemStack {
                return createTypeIcon(DIAMOND_HELMET, "Helmets", Enchantment.WATER_WORKER, Enchantment.OXYGEN)
            }

        }

        inner class BodyTypeMenu : ChantTypeMenu("Armor", Row.R_3) {

            override fun build() {
                createEnchantButton(Row.R_2, Col.C_2, Enchantment.PROTECTION_ENVIRONMENTAL)

                createEnchantButton(Row.R_2, Col.C_4, Enchantment.PROTECTION_FIRE)
                createEnchantButton(Row.R_2, Col.C_5, Enchantment.PROTECTION_EXPLOSIONS)
                createEnchantButton(Row.R_2, Col.C_6, Enchantment.PROTECTION_PROJECTILE)

                createEnchantButton(Row.R_2, Col.C_8, Enchantment.THORNS)

                initBackButton(this@ChantSelectionMenu, plugin, Row.R_3, Col.C_1)
                initNoneButton(this@ChantSelectionMenu, Row.R_3, Col.C_9,
                               Enchantment.PROTECTION_ENVIRONMENTAL,
                               Enchantment.PROTECTION_FIRE,
                               Enchantment.PROTECTION_EXPLOSIONS,
                               Enchantment.PROTECTION_PROJECTILE,
                               Enchantment.THORNS)
            }

            override fun createIcon(): ItemStack {
                return createTypeIcon(DIAMOND_CHESTPLATE, "Armor",
                                      Enchantment.PROTECTION_ENVIRONMENTAL,
                                      Enchantment.PROTECTION_FIRE,
                                      Enchantment.PROTECTION_EXPLOSIONS,
                                      Enchantment.PROTECTION_PROJECTILE,
                                      Enchantment.THORNS)
            }

        }

        inner class FeetTypeMenu : ChantTypeMenu("Boots", Row.R_3) {

            override fun build() {
                createEnchantButton(Row.R_2, Col.C_3, Enchantment.PROTECTION_FALL)
                createEnchantButton(Row.R_2, Col.C_5, Enchantment.DEPTH_STRIDER)
                createEnchantButton(Row.R_2, Col.C_7, Enchantment.FROST_WALKER)

                initBackButton(this@ChantSelectionMenu, plugin, Row.R_3, Col.C_1)
                initNoneButton(this@ChantSelectionMenu, Row.R_3, Col.C_9, Enchantment.PROTECTION_FALL, Enchantment.DEPTH_STRIDER, Enchantment.FROST_WALKER)
            }

            override fun createIcon(): ItemStack {
                return createTypeIcon(DIAMOND_BOOTS, "Boots", Enchantment.PROTECTION_FALL, Enchantment.DEPTH_STRIDER, Enchantment.FROST_WALKER)
            }

        }


        inner class WeapTypeMenu : ChantTypeMenu("Weapon", Row.R_4) {

            override fun build() {
                createEnchantButton(Row.R_2, Col.C_2, Enchantment.DAMAGE_ALL)

                createEnchantButton(Row.R_2, Col.C_4, Enchantment.DAMAGE_UNDEAD)
                createEnchantButton(Row.R_3, Col.C_4, Enchantment.DAMAGE_ARTHROPODS)

                createEnchantButton(Row.R_2, Col.C_6, Enchantment.FIRE_ASPECT)
                createEnchantButton(Row.R_3, Col.C_6, Enchantment.SWEEPING_EDGE)

                createEnchantButton(Row.R_2, Col.C_8, Enchantment.LOOT_BONUS_MOBS)
                createEnchantButton(Row.R_3, Col.C_8, Enchantment.KNOCKBACK)

                initBackButton(this@ChantSelectionMenu, plugin, Row.R_4, Col.C_1)
                initNoneButton(this@ChantSelectionMenu, Row.R_4, Col.C_9,
                               Enchantment.DAMAGE_ALL,
                               Enchantment.DAMAGE_UNDEAD,
                               Enchantment.DAMAGE_ARTHROPODS,
                               Enchantment.FIRE_ASPECT,
                               Enchantment.SWEEPING_EDGE,
                               Enchantment.LOOT_BONUS_MOBS,
                               Enchantment.KNOCKBACK)
            }

            override fun createIcon(): ItemStack {
                return createTypeIcon(DIAMOND_SWORD, "Weapons",
                                      Enchantment.DAMAGE_ALL,
                                      Enchantment.DAMAGE_UNDEAD,
                                      Enchantment.DAMAGE_ARTHROPODS,
                                      Enchantment.FIRE_ASPECT,
                                      Enchantment.SWEEPING_EDGE,
                                      Enchantment.LOOT_BONUS_MOBS,
                                      Enchantment.KNOCKBACK)
            }

        }

        inner class BowsTypeMenu : ChantTypeMenu("Bow", Row.R_3) {

            override fun build() {
                createEnchantButton(Row.R_2, Col.C_2, Enchantment.ARROW_DAMAGE)
                createEnchantButton(Row.R_2, Col.C_4, Enchantment.ARROW_KNOCKBACK)
                createEnchantButton(Row.R_2, Col.C_6, Enchantment.ARROW_FIRE)
                createEnchantButton(Row.R_2, Col.C_8, Enchantment.ARROW_INFINITE)

                initBackButton(this@ChantSelectionMenu, plugin, Row.R_3, Col.C_1)
                initNoneButton(this@ChantSelectionMenu, Row.R_3, Col.C_9, Enchantment.ARROW_DAMAGE, Enchantment.ARROW_KNOCKBACK, Enchantment.ARROW_FIRE, Enchantment.ARROW_INFINITE)
            }

            override fun createIcon(): ItemStack {
                return createTypeIcon(BOW, "Bows", Enchantment.ARROW_DAMAGE, Enchantment.ARROW_KNOCKBACK, Enchantment.ARROW_FIRE, Enchantment.ARROW_INFINITE)
            }

        }

        inner class TridTypeMenu : ChantTypeMenu("Trident", Row.R_3) {

            override fun build() {
                createEnchantButton(Row.R_2, Col.C_2, Enchantment.IMPALING)
                createEnchantButton(Row.R_2, Col.C_4, Enchantment.LOYALTY)
                createEnchantButton(Row.R_2, Col.C_6, Enchantment.RIPTIDE)
                createEnchantButton(Row.R_2, Col.C_8, Enchantment.CHANNELING)

                initBackButton(this@ChantSelectionMenu, plugin, Row.R_3, Col.C_1)
                initNoneButton(this@ChantSelectionMenu, Row.R_3, Col.C_9, Enchantment.IMPALING, Enchantment.LOYALTY, Enchantment.RIPTIDE, Enchantment.CHANNELING)
            }

            override fun createIcon(): ItemStack {
                return createTypeIcon(TRIDENT, "Tridents", Enchantment.IMPALING, Enchantment.LOYALTY, Enchantment.RIPTIDE, Enchantment.CHANNELING)
            }

        }


        inner class MiscTypeMenu : ChantTypeMenu("Misc", Row.R_3) {

            override fun build() {
                createEnchantButton(Row.R_2, Col.C_2, Enchantment.DURABILITY)
                createEnchantButton(Row.R_2, Col.C_4, Enchantment.MENDING)
                createEnchantButton(Row.R_2, Col.C_6, Enchantment.BINDING_CURSE)
                createEnchantButton(Row.R_2, Col.C_8, Enchantment.VANISHING_CURSE)

                initBackButton(this@ChantSelectionMenu, plugin, Row.R_3, Col.C_1)
                initNoneButton(this@ChantSelectionMenu, Row.R_3, Col.C_9, Enchantment.DURABILITY, Enchantment.MENDING, Enchantment.BINDING_CURSE, Enchantment.VANISHING_CURSE)
            }

            override fun createIcon(): ItemStack {
                return createTypeIcon(COMMAND_BLOCK, "Miscellaneous", Enchantment.DURABILITY, Enchantment.MENDING, Enchantment.BINDING_CURSE, Enchantment.VANISHING_CURSE)
            }

        }

    }

    inner class ChantLevelMenu(private val enchantment: Enchantment, private val prevMenu: Menu? = null) : Menu("&nLevel of ${enchantment.properName()}", Row.R_3) {

        override fun build() {
            refreshDisplay()

            val decrease = buildItemStack(RED_WOOL) {
                displayName = "&fDecrease Level"
            }
            val increase = buildItemStack(GREEN_WOOL) {
                displayName = "&fIncrease Level"
            }

            this[Row.R_2, Col.C_4, decrease] = {
                chant.chant.compute(enchantment) { _, level ->
                    ((level ?: 1) - 1).coerceAtLeast(1)
                }
                refreshDisplay()
            }
            this[Row.R_2, Col.C_6, increase] = {
                chant.chant.compute(enchantment) { _, level ->
                    ((level ?: 1) + 1).coerceAtMost(enchantment.maxLevel)
                }
                refreshDisplay()
            }

            val cancel = buildItemStack(REDSTONE_BLOCK) {
                displayName = "&cCancel filter"
            }
            val accept = buildItemStack(EMERALD_BLOCK) {
                displayName = "&aAccept filter"
            }

            this[Row.R_3, Col.C_1, cancel] = {
                chant.chant.remove(enchantment)
                chant.enabled = chant.chant.isNotEmpty()

                prevMenu?.open(who)
                prevMenu?.fresh()

                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
            this[Row.R_3, Col.C_9, accept] = {
                chant.enabled = true

                prevMenu?.open(who)
                prevMenu?.fresh()

                plugin.garnishManager.send(who, MENU_BUTTON_BACK)
            }
        }

        override fun open(player: Player) {
            if (enchantment !in chant.chant) {
                chant.chant[enchantment] = 1
            }

            super.open(player)
        }

        private fun refreshDisplay() {
            val display = buildItemStack(EXPERIENCE_BOTTLE, chant.chant[enchantment] ?: 1) {
                displayName = "&fCurrent"
                lore = listOf("  &a${chant.chant[enchantment] ?: 1}")
            }

            this[Row.R_1, Col.C_5, display] = {}
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