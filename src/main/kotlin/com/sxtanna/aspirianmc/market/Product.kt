package com.sxtanna.aspirianmc.market

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.base.Displayable
import com.sxtanna.aspirianmc.base.Identified
import com.sxtanna.aspirianmc.base.Named
import com.sxtanna.aspirianmc.base.Result.None
import com.sxtanna.aspirianmc.base.Result.Some
import com.sxtanna.aspirianmc.company.Staffer
import com.sxtanna.aspirianmc.exts.base64ToItemStack
import com.sxtanna.aspirianmc.exts.buildItemStack
import com.sxtanna.aspirianmc.exts.itemStackName
import com.sxtanna.aspirianmc.exts.itemStackToBase64
import org.bukkit.Material.BARRIER
import org.bukkit.inventory.ItemStack
import java.util.*

class Product : Named, Identified<UUID>, Displayable {

    @Transient
    internal lateinit var plugin: Companies


    override var name = ""
        internal set
    override var uuid = UUID.randomUUID()
        internal set


    var stafferUUID: UUID? = null
        internal set


    var base = ""
        internal set
    var date = 0L
        internal set
    var cost = 0.0
        internal set


    fun updateData(staffer: Staffer, item: ItemStack, date: Long, cost: Double): Product {
        this.base = when(val result = itemStackToBase64(item)) {
            is Some -> result.data
            is None -> ""
        }

        this.date = date
        this.cost = cost

        this.stafferUUID = staffer.uuid

        return this
    }


    override fun createDisplayIcon(): ItemStack {
        return when (val item = base64ToItemStack(base)) {
            is Some -> buildItemStack(item.data) {
                if (name.isBlank()) {
                    name = itemStackName(item.data)
                }

                displayName = "${if (item.data.type.maxStackSize == 1) "" else "&a${item.data.amount} "}&f$name"


                val loreLines = mutableListOf<String>()

                if (item.data.hasItemMeta() && item.data.itemMeta.hasLore()) {
                    loreLines += item.data.itemMeta.lore
                }

                loreLines += listOf(
                        "",
                        "&8&m                       ",
                        "&7Cost: &a$$cost",
                        "&7Sold By: ${stafferUUID?.let(plugin.stafferManager.names::get) ?: "Unknown"}")

                lore = loreLines
            }
            is None -> buildItemStack(BARRIER) {
                displayName = "&4&lINVALID ITEM OMG SPOOKY"
            }
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Product

        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

}