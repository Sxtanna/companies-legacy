package com.sxtanna.aspiriamc.menu.item

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.PluginDependant
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class UpdatingItemStack(override val plugin: Companies, type: Material, amount: Int, private val updateTask: UpdatingItemStack.() -> Unit) : ItemStack(type, amount), PluginDependant {

    var extra: ()  -> Unit = {}


    /*private lateinit var task: BukkitTask

    fun load(period: Long = 20, extra: () -> Unit = {}) {
        if (::task.isInitialized && task.isCancelled.not()) {
            task.cancel()
        }

        task = repeat(period) {
            updateTask.invoke(this@UpdatingItemStack)
            extra.invoke()
        }
    }

    fun kill() {
        if (::task.isInitialized.not() || task.isCancelled) return

        task.cancel()
    }*/


    fun update() {
        updateTask.invoke(this@UpdatingItemStack)
        extra.invoke()
    }

}