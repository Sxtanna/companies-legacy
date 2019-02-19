package com.sxtanna.aspiriamc.market

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.Iconable
import com.sxtanna.aspiriamc.base.Named
import com.sxtanna.aspiriamc.base.Result.None
import com.sxtanna.aspiriamc.base.Result.Some
import com.sxtanna.aspiriamc.base.Searchable
import com.sxtanna.aspiriamc.base.Searchable.Query
import com.sxtanna.aspiriamc.base.Searchable.Query.*
import com.sxtanna.aspiriamc.base.Searchable.Query.CostQuery.CostType.ABOVE
import com.sxtanna.aspiriamc.base.Searchable.Query.CostQuery.CostType.BELOW
import com.sxtanna.aspiriamc.base.Unique
import com.sxtanna.aspiriamc.company.Staffer
import com.sxtanna.aspiriamc.exts.*
import org.bukkit.Material.BARRIER
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import java.util.*

class Product : Named, Unique<UUID>, Iconable, Searchable {

    @Transient
    internal lateinit var plugin: Companies

    @Transient
    internal var canNotBuyDecide = false
    @Transient
    internal var canNotBuyBought = false


    override var name = ""
        internal set
    override var uuid = UUID.randomUUID()
        internal set


    var stafferUUID: UUID? = null
        internal set
    var companyUUID: UUID? = null
        internal set


    var base = ""
        internal set
    var date = 0L
        internal set
    var cost = 0.0
        internal set


    fun updateData(staffer: Staffer, item: ItemStack, date: Long, cost: Double): Product {
        this.base = when (val result = itemStackToBase64(item)) {
            is Some -> result.data
            is None -> ""
        }

        this.date = date
        this.cost = cost
        this.name = itemStackName(item)

        this.stafferUUID = staffer.uuid
        this.companyUUID = staffer.companyUUID

        return this
    }


    override fun createIcon(): ItemStack {
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

    override fun passes(query: Query): Boolean {
        if (query.enabled.not()) {
            return true
        }

        if (query is NameQuery && name.contains(query.name, true)) {
            return true
        }
        if (query is CostQuery) {
            return when (query.type) {
                ABOVE -> {
                    cost >= query.cost
                }
                BELOW -> {
                    cost <= query.cost
                }
            }
        }

        val item = base64ToItemStack(base) as? Some<ItemStack> ?: return false

        return when (query) {
            is NameQuery -> {
                item.data.type.name.contains(query.name, true)
            }
            is TypeQuery -> {
                item.data.type == query.type
            }
            is DataQuery -> {
                when (query) {
                    is DataQuery.Count -> {
                        item.data.amount == query.count
                    }
                    is DataQuery.Color -> {
                        query.color?.let { itemStackIsColor(item.data, it) } ?: false
                    }
                    is DataQuery.Kinds -> {
                        query.predicate.invoke(item.data.type)
                    }
                    is DataQuery.Chant -> {
                        query.chant.all {
                            it.value == item.data.getEnchantmentLevel(it.key) || it.value == (item.data.itemMeta as? EnchantmentStorageMeta)?.getStoredEnchantLevel(it.key)
                        }
                    }
                }
            }
            else         -> {
                false
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