package com.sxtanna.aspirianmc.company.menu

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.base.Result.None
import com.sxtanna.aspirianmc.base.Result.Some
import com.sxtanna.aspirianmc.company.Staffer
import com.sxtanna.aspirianmc.config.Garnish.MENU_BUTTON_CLICK
import com.sxtanna.aspirianmc.exts.base64ToItemStack
import com.sxtanna.aspirianmc.menu.Menu
import com.sxtanna.aspirianmc.menu.base.Col
import com.sxtanna.aspirianmc.menu.base.Row

class CompanyVoidsMenu(val plugin: Companies, val staffer: Staffer, val prevMenu: Menu?) : Menu("&nCompany&r &l»&r Voided Items", Row.R_6) {

    val pagination = ChosenPagination()

    override fun build() {
        val itemSlots = itemStackSlots
        pagination.page().forEach {
            val (row, col) = slotToGrid(itemSlots.nextInt())

            val item = when (val item = base64ToItemStack(it)) {
                is Some -> {
                    item.data
                }
                is None -> {
                    return@forEach
                }
            }

            this[row, col, item] = out@{
                plugin.garnishManager.send(who, MENU_BUTTON_CLICK)

                if (who.inventory.addItem(item).isNotEmpty()) {
                    reply("&cyour inventory is full")
                } else {
                    staffer.voidedItems -= it
                    fresh()
                }
            }
        }

        pagination.init()

        initBackButton(prevMenu, plugin, Row.R_6, Col.C_5)
    }

    override fun fresh() {
        pagination.push(stafferVoidedItems())

        super.fresh()
    }

    private fun stafferVoidedItems(): List<List<String>> {
        return staffer.voidedItems.chunked(45).takeIf { it.isNotEmpty() } ?: listOf(emptyList())
    }


    inner class ChosenPagination : Pagination<List<String>>(stafferVoidedItems()) {

        override fun prevCoords() = Row.R_6 to Col.C_4

        override fun nextCoords() = Row.R_6 to Col.C_6

    }


    companion object {

        private val itemStackSlots: IntIterator
            get() = (0..45).iterator()

    }

}