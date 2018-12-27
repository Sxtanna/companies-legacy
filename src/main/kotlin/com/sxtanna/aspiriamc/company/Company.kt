package com.sxtanna.aspiriamc.company

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.*
import com.sxtanna.aspiriamc.base.Result.*
import com.sxtanna.aspiriamc.base.Searchable.Query
import com.sxtanna.aspiriamc.company.Company.Finance.Account
import com.sxtanna.aspiriamc.config.Configs.DISPLAY_DEF_ICON
import com.sxtanna.aspiriamc.config.Configs.PAYOUTS_DEF_RATIO
import com.sxtanna.aspiriamc.exts.buildItemStack
import com.sxtanna.aspiriamc.exts.buildUpdatingItemStack
import com.sxtanna.aspiriamc.exts.formatToTwoPlaces
import com.sxtanna.aspiriamc.exts.updateItemMeta
import com.sxtanna.aspiriamc.manager.HiringsManager
import com.sxtanna.aspiriamc.market.Product
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Material.AIR
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag.*
import org.bukkit.inventory.ItemStack
import java.util.*

class Company() : Named, Unique<UUID>, Iconable, Searchable {
    constructor(name: String) : this() {
        this.name = name
    }

    override var name = ""
        internal set
    override var uuid = UUID.randomUUID()
        internal set


    var icon = AIR

    val finance = Finance()
    val staffer = mutableListOf<UUID>()
    val product = mutableListOf<Product>()


    @Transient
    internal lateinit var plugin: Companies


    override fun createIcon(): ItemStack {
        val type = when (icon) {
            AIR  -> {
                plugin.configsManager.get(DISPLAY_DEF_ICON)
            }
            else -> {
                icon
            }
        }

        return if (plugin.companyManager.sponsorManager.isSponsored(this)) {
            buildUpdatingItemStack(plugin, type, 1,
                                   block = {
                                       addItemFlags(HIDE_ENCHANTS,
                                                    HIDE_ATTRIBUTES,
                                                    HIDE_UNBREAKABLE,
                                                    HIDE_DESTROYS,
                                                    HIDE_PLACED_ON,
                                                    HIDE_POTION_EFFECTS)

                                       displayName = "&f$name"
                                       lore = listOf("",
                                                     "",
                                                     "&7Employees: &a${staffer.size}",
                                                     "&7Items for sale: &a${product.size}")
                                   },
                                   update = {
                                       updateItemMeta(this) {
                                           addItemFlags(HIDE_ENCHANTS,
                                                        HIDE_ATTRIBUTES,
                                                        HIDE_UNBREAKABLE,
                                                        HIDE_DESTROYS,
                                                        HIDE_PLACED_ON,
                                                        HIDE_POTION_EFFECTS)

                                           displayName = "&e$name"
                                           lore = listOf("",
                                                         "",
                                                         "&7Employees: &a${staffer.size}",
                                                         "&7Items for sale: &a${product.size}",
                                                         "",
                                                         "&7${plugin.companyManager.sponsorManager.sponsorshipTimeLeft(this@Company)}")
                                       }
                                   })
        } else {
            buildItemStack(type) {
                addItemFlags(HIDE_ENCHANTS,
                             HIDE_ATTRIBUTES,
                             HIDE_UNBREAKABLE,
                             HIDE_DESTROYS,
                             HIDE_PLACED_ON,
                             HIDE_POTION_EFFECTS)

                displayName = "&f$name"
                lore = listOf("",
                              "",
                              "&7Employees: &a${staffer.size}",
                              "&7Items for sale: &a${product.size}")
            }
        }
    }

    override fun passes(query: Query): Boolean {
        return product.any { it.passes(query) }
    }


    fun hire(staffer: Staffer, hirings: HiringsManager): Result<Result.Status> = Result.of {
        if (staffer.uuid in this@Company.staffer) {
            Status.FAIL
        } else when (val result = hirings.attemptHire(this@Company, staffer)) {
            is Some -> {
                Status.PASS
            }
            is None -> {
                result.rethrow()
            }
        }
    }

    internal fun hire(staffer: Staffer) {
        this.staffer += staffer.uuid
        staffer.companyUUID = uuid
    }


    fun fire(staffer: Staffer): Result<Result.Status> = Result.of {
        if (staffer.uuid !in this@Company.staffer) {
            Status.FAIL
        } else {
            resetData(staffer)
            Status.PASS
        }
    }


    fun isOwner(uuid: UUID): Boolean {
        return staffer.firstOrNull() == uuid
    }

    operator fun contains(uuid: UUID): Boolean {
        return uuid in staffer
    }

    operator fun contains(data: Player): Boolean {
        return contains(data.uniqueId)
    }


    fun onlineStaffers(): List<Player> {
        return staffer.mapNotNull(Bukkit::getPlayer)
    }


    internal fun resetData(staffer: Staffer) {
        this.staffer -= staffer.uuid

        finance.account -= staffer.uuid

        product.removeIf {
            val result = it.stafferUUID == staffer.uuid

            if (result) {
                staffer.voidedItems += it.base
            }

            result
        }

        staffer.companyUUID = null
    }

    internal fun updateData(uuid: UUID, name: String, icon: Material, tariffs: Int, balance: Double, account: Map<UUID, Account>, staffer: List<UUID>, product: List<Product>): Company {
        this.uuid = uuid
        this.name = name
        this.icon = icon

        this.finance.updateData(tariffs, balance, account)

        this.staffer.addAll(staffer)
        this.product.addAll(product)

        this.product.forEach {
            it.companyUUID = this.uuid
        }

        return this
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Company

        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        return uuid?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "Company(name='$name', uuid=$uuid, staffer=$staffer)"
    }


    class Finance {

        @Transient
        internal lateinit var plugin: Companies


        var tariffs = -1
        var balance = 0.0
        val account = mutableMapOf<UUID, Account>()


        operator fun get(uuid: UUID): Account {
            return account.computeIfAbsent(uuid) { Account(0, 0.0, 0.0, plugin.configsManager.get(PAYOUTS_DEF_RATIO)) }
        }


        fun resetPayoutRatios() {
            account.values.forEach { it.payoutRatio = plugin.configsManager.get(PAYOUTS_DEF_RATIO) }
        }


        internal fun updateData(tariffs: Int, balance: Double, account: Map<UUID, Account>) {
            this.tariffs = tariffs
            this.balance = balance

            this.account.putAll(account)
        }


        data class Account(var itemsSold: Int, var playerProfit: Double, var playerPayout: Double, var payoutRatio: Int) {

            fun soldItem(profit: Double, payout: Double) {
                itemsSold += 1

                playerProfit = (playerProfit + profit).formatToTwoPlaces()
                playerPayout = (playerPayout + payout).formatToTwoPlaces()
            }

        }

    }

}