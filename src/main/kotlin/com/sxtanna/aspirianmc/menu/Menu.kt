package com.sxtanna.aspirianmc.menu

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.config.Garnish.MENU_BUTTON_BACK
import com.sxtanna.aspirianmc.exts.BACK
import com.sxtanna.aspirianmc.exts.PREF
import com.sxtanna.aspirianmc.exts.buildItemStack
import com.sxtanna.aspirianmc.exts.colorFormat
import com.sxtanna.aspirianmc.menu.Menu.ButtonToggleState.GREYED
import com.sxtanna.aspirianmc.menu.Menu.ButtonToggleState.REMOVE
import com.sxtanna.aspirianmc.menu.base.Col
import com.sxtanna.aspirianmc.menu.base.Row
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material.BARRIER
import org.bukkit.Material.PAPER
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.util.*

abstract class Menu(val title: String, val rows: Row) : InventoryHolder {

    protected open val cache = false
    protected open val prevCache = mutableMapOf<UUID, Menu>()

    private var bukkitInventory = Bukkit.createInventory(this, rows.size, ChatColor.translateAlternateColorCodes('&', title))


    override fun getInventory(): Inventory {
        return bukkitInventory
    }


    private var built = false
    private val items = mutableMapOf<Col, MutableMap<Row, MenuButton>>()


    internal open fun build() {

    }

    internal open fun clear() {
        items.clear()
        bukkitInventory.clear()
    }

    internal open fun fresh() {
        clear()
        build()
    }


    operator fun set(row: Row, col: Col, item: ItemStack, click: MenuAction.() -> Unit) {
        setButton(row, col, MenuButton(item, click))
    }

    operator fun set(row: Row, item: ItemStack, overwrite: Boolean = false, click: MenuAction.() -> Unit) {
        Col.values().forEach {
            if (overwrite || getButton(row, it) == null) set(row, it, item, click)
        }
    }

    operator fun set(col: Col, item: ItemStack, overwrite: Boolean = false, click: MenuAction.() -> Unit) {
        Row.values().forEach {
            if (it > rows) return
            if (overwrite || getButton(it, col) == null) set(it, col, item, click)
        }
    }


    open fun open(player: Player) {
        if (built.not()) {
            build()
            built = true
        }

        if (cache) {
            val prev = player.openInventory?.topInventory?.holder

            if (prev != null && prev is Menu) {
                prevCache[player.uniqueId] = prev
            }
        }

        player.openInventory(inventory)
    }

    open fun push(player: Player, clickType: ClickType, slot: Int) {
        val (row, col) = slotToGrid(slot)

        val button = getButton(row, col) ?: return
        button.click.invoke(MenuAction(player, clickType, button.stack))
    }


    open fun onClose(player: Player) {

    }

    open fun onEmptyClick(player: Player, type: ClickType) {
        prevCache.remove(player.uniqueId)?.open(player)
    }

    open fun onSlotsClick(player: Player, type: ClickType, slot: Int) = Unit


    private fun getButton(row: Row, col: Col): MenuButton? {
        return items[col]?.get(row)
    }

    private fun setButton(row: Row, col: Col, button: MenuButton) {
        val cols = items.getOrPut(col) { mutableMapOf() }
        cols[row] = button

        bukkitInventory.setItem(gridToSlot(row, col), button.stack)
    }


    protected fun gridToSlot(row: Row, col: Col): Int {
        return row.slot(col)
    }

    protected fun slotToGrid(slot: Int): Pair<Row, Col> {
        val row = slot / 9
        val col = slot - row * 9
        return Row.values()[row] to Col.values()[col]
    }


    protected fun initBackButton(prevMenu: Menu?, plugin: Companies, row: Row, col: Col) {
        if (prevMenu == null) return

        val backButton = buildItemStack(BARRIER) {
            displayName = BACK
        }

        this[row, col, backButton] = {
            prevMenu.open(who)
            prevMenu.fresh()

            plugin.garnishManager.send(who, MENU_BUTTON_BACK)
        }
    }


    data class MenuAction(val who: Player, val how: ClickType, val what: ItemStack) {

        fun reply(msg: String) {
            who.sendMessage(colorFormat("$PREF $msg"))
        }

    }

    data class MenuButton(val stack: ItemStack, val click: (MenuAction) -> Unit)

    enum class ButtonToggleState {

        GREYED,
        REMOVE,

    }

    open inner class Pagination<T : Any>(internal var pages: List<T>) {

        internal var index = 0


        protected open val toggleState = ButtonToggleState.REMOVE

        protected open val prevClick = { _: MenuAction ->
            index--
            fresh()
        }
        protected open val nextClick = { _: MenuAction ->
            index++
            fresh()
        }

        protected open val prevStack: ItemStack
            get() = buildItemStack(PAPER) {
                displayName = "&aLast Page"
                lore = listOf("    &a&m<    ", "", "&7You are on page:&f ${index + 1}")
            }

        protected open val nextStack: ItemStack
            get() = buildItemStack(PAPER) {
                displayName = "&aNext Page"
                lore = listOf("    &a&m    >", "", "&7You are on page:&f ${index + 1}")
            }


        fun page() = pages[index]

        fun init() {
            // prev page
            if (usePrev()) {
                val (row, col) = prevCoords()

                if (pages.size > 1 && index != 0) {
                    this@Menu[row, col, prevStack] = prevClick
                } else when (toggleState) {
                    REMOVE -> Unit
                    GREYED -> {
                        this@Menu[row, col, buildItemStack(prevStack) { displayName.replace("§f", "§7") }] = {}
                    }
                }
            }


            // next page
            if (useNext()) {
                val (row, col) = nextCoords()

                if (pages.size > 1 && index != pages.lastIndex) {
                    this@Menu[row, col, nextStack] = nextClick
                } else when (toggleState) {
                    REMOVE -> Unit
                    GREYED -> {
                        this@Menu[row, col, buildItemStack(nextStack) { displayName.replace("§f", "§7") }] = {}
                    }
                }
            }
        }

        fun push(pages: List<T>) {
            this.pages = pages

            if (this.index >= this.pages.size) {
                this.index = this.pages.lastIndex
            }
        }


        open fun usePrev() = true

        open fun useNext() = true


        open fun prevCoords() = Row.R_6 to Col.C_1

        open fun nextCoords() = Row.R_6 to Col.C_9

    }

}