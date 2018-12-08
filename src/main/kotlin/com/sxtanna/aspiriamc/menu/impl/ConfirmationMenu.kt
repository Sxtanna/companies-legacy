package com.sxtanna.aspiriamc.menu.impl

import com.sxtanna.aspiriamc.exts.buildItemStack
import com.sxtanna.aspiriamc.menu.Menu
import com.sxtanna.aspiriamc.menu.base.Col
import com.sxtanna.aspiriamc.menu.base.Row
import com.sxtanna.aspiriamc.menu.base.Row.R_3
import org.bukkit.Material.*

abstract class ConfirmationMenu(prompt: String) : Menu(prompt, R_3) {

    abstract fun onPass(action: MenuAction)

    abstract fun onFail(action: MenuAction)


    open fun passName(): String {
        return "&aYes"
    }

    open fun failName(): String {
        return "&cNo"
    }


    open fun passLore(): List<String> {
        return emptyList()
    }

    open fun failLore(): List<String> {
        return emptyList()
    }


    override fun build() {

        val passItem = buildItemStack(LIME_STAINED_GLASS_PANE) {
            displayName = passName()
            lore = passLore()
        }

        val failItem = buildItemStack(RED_STAINED_GLASS_PANE) {
            displayName = failName()
            lore = failLore()
        }

        val lineItem = buildItemStack(WHITE_STAINED_GLASS_PANE) {
            displayName = " "
        }


        val passFunc = ::onPass
        val failFunc = ::onFail


        this[Col.C_1, passItem] = passFunc
        this[Col.C_2, passItem] = passFunc
        this[Col.C_3, passItem] = passFunc

        this[Col.C_7, failItem] = failFunc
        this[Col.C_8, failItem] = failFunc
        this[Col.C_9, failItem] = failFunc

        this[Row.R_2, lineItem] =  {}
    }

}