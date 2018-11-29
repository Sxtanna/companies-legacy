package com.sxtanna.aspirianmc.company

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.base.Displayable
import com.sxtanna.aspirianmc.base.Identified
import com.sxtanna.aspirianmc.base.Named
import com.sxtanna.aspirianmc.base.Result
import com.sxtanna.aspirianmc.base.Result.*
import com.sxtanna.aspirianmc.company.Company.Finance.Account
import com.sxtanna.aspirianmc.config.Configs.DISPLAY_DEF_ICON
import com.sxtanna.aspirianmc.config.Configs.PAYOUTS_DEF_RATIO
import com.sxtanna.aspirianmc.exts.buildItemStack
import com.sxtanna.aspirianmc.exts.formatToTwoPlaces
import com.sxtanna.aspirianmc.exts.korm
import com.sxtanna.aspirianmc.manager.HiringsManager
import com.sxtanna.aspirianmc.market.Product
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Material.AIR
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES
import org.bukkit.inventory.ItemStack
import java.util.*

class Company() : Named, Identified<UUID>, Displayable {
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


    override fun createDisplayIcon(): ItemStack {
        val type = when (icon) {
            AIR -> {
                plugin.configsManager.get(DISPLAY_DEF_ICON)
            }
            else -> icon
        }

        return buildItemStack(type) {
            addItemFlags(HIDE_ATTRIBUTES)

            displayName = "&f$name"
            lore = listOf(
                    "",
                    "",
                    "&7Employees: &a${staffer.size}",
                    "&7Items for sale: &a${product.size}"
            )
        }
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

        println(korm.push(this))

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
        return name
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