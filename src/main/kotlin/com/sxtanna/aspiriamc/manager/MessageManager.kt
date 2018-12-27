package com.sxtanna.aspiriamc.manager

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.exts.color
import com.sxtanna.aspiriamc.manager.MessageManager.Consts.companyText
import com.sxtanna.aspiriamc.manager.MessageManager.Consts.stafferText
import com.sxtanna.aspiriamc.manager.base.Manager
import com.sxtanna.aspiriamc.market.Product
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

class MessageManager(override val plugin: Companies) : Manager("Message") {


    fun sendSoldMessageFor(product: Product, company: Company, buyer: String, payout: Double, taxes: Double, itemStack: ItemStack) {

        val staff = product.stafferUUID
        val owner = company.staffer.first()

        fun itemName(): String {
            return if (itemStack.type.maxStackSize == 1) {
                product.name
            } else {
                "&a${itemStack.amount} &f${product.name}${if (product.name.endsWith('s') || itemStack.amount == 1) "" else "s"}"
            }
        }

        fun sendStaff() = sendMessageTo(staff) {
            stafferText[
                    "{buyer_name}",
                    buyer
            ][
                    "{product_name}",
                    itemName()
            ][
                    "{payout}",
                    "$$payout"
            ]
        }

        fun sendOwner() = sendMessageTo(owner) { player ->
            companyText[
                    "{taxes}",
                    "+$$taxes"
            ][
                    "{payout}",
                    "$$payout"
            ][
                    "{buyer_name}",
                    if (buyer == player.name) "You" else buyer
            ][
                    "{product_name}",
                    itemName()
            ][
                    "{seller_name}",
                    staff?.let(plugin.stafferManager.names::get)?.let { if (it == player.name) "You" else it } ?: "Unknown"
            ]
        }

        if (staff == owner) { // same player
            sendOwner()
            return
        }

        sendStaff()
        sendOwner()
    }


    private fun sendMessageTo(receiver: UUID?, message: (Player) -> String) {
        receiver?.let(::findPlayerByUUID)?.apply {
            sendMessage(color(message.invoke(this)))
        }
    }

    private operator fun String.get(old: String, new: String): String {
        return replace(old, new)
    }


    private object Consts {

        val stafferText = message("&8&m                                               ",
                                  "&e{buyer_name} &7bought your &b{product_name}",
                                  "&fPayout: &a{payout}",
                                  "&8&m                                               ")

        val companyText = message("&8&m                                               ",
                                  "&e{buyer_name} &7bought &b{product_name} &7listed by &e{seller_name}",
                                  "&fCompany Vault: &a{taxes}",
                                  "&fPayout for &e{seller_name}&f: &a{payout}",
                                  "&8&m                                               ")


        private fun message(vararg lines: String): String {
            return lines.joinToString("&r\n")
        }

    }


}