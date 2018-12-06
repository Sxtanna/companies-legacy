package com.sxtanna.aspirianmc.menu.impl

import com.sxtanna.aspirianmc.exts.buildItemStack
import com.sxtanna.aspirianmc.menu.Menu
import com.sxtanna.aspirianmc.menu.base.Col
import com.sxtanna.aspirianmc.menu.base.Row
import com.sxtanna.aspirianmc.menu.base.Row.R_3
import org.bukkit.Material.*

abstract class ConfirmationMenu(action: String) : Menu("&nConfirm:&r $action", R_3) {

    abstract fun onPass(action: MenuAction)

    abstract fun onFail(action: MenuAction)


    override fun build() {

        val passItem = buildItemStack(LIME_STAINED_GLASS_PANE) {
            displayName = "&a&l  Confirm  "
        }

        val failItem = buildItemStack(RED_STAINED_GLASS_PANE) {
            displayName = "&c&l  Cancel  "
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