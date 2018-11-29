package com.sxtanna.aspirianmc.manager

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.base.Result.Some
import com.sxtanna.aspirianmc.company.Company
import com.sxtanna.aspirianmc.company.Staffer
import com.sxtanna.aspirianmc.exts.formatToTwoPlaces
import com.sxtanna.aspirianmc.manager.base.Manager
import com.sxtanna.aspirianmc.market.Product
import java.util.*

class ReportsManager(override val plugin: Companies) : Manager("Reports") {

    fun reportPurchase(product: Product) {

        fun retrieveCompany(staffer: Staffer) {
            val result = plugin.companyManager.get(staffer.companyUUID ?: return) {
                reportPurchase(it ?: return@get, product)
            }

            when (result) {
                is Some -> {
                    reportPurchase(result.data, product)
                }
            }
        }

        val result = plugin.stafferManager.get(product.stafferUUID ?: return) { data, _ ->
            retrieveCompany(data)
        }

        when (result) {
            is Some -> {
                retrieveCompany(result.data)
            }
        }
    }


    private fun reportPurchase(company: Company, product: Product) {
        val stafferUUID = product.stafferUUID ?: return // should be impossible

        var revenue = product.cost
        val tariffs = ((revenue / 100.0) * company.finance.tariffs)

        // tax product
        revenue -= tariffs

        company.finance.balance = (company.finance.balance + tariffs).formatToTwoPlaces()

        val account = company.finance[stafferUUID]
        val payouts = if (company.staffer.size == 1) revenue else ((revenue / 100.0) * account.payoutRatio)

        account.soldItem(product.cost, payouts)
        plugin.vaultHook.attemptGive(stafferUUID, revenue)

        revenue -= payouts

        if (company.staffer.size == 1) return

        revenue /= company.staffer.size - 1

        company.staffer.forEach {
            if (it == stafferUUID) return@forEach

            company.finance[it].playerPayout += revenue
            plugin.vaultHook.attemptGive(it, revenue)
        }
    }


    private fun calculatePayout(company: Company, revenue: Double): Map<UUID, Double> {
        val outputs = mutableMapOf<UUID, Double>()

        // legacy shit
        /*when (company.staffer.size) {
            1 -> {
                outputs[company.staffer.first()] = revenue
            }
            2 -> {
                outputs[company.staffer[0]] = revenue / 2
                outputs[company.staffer[1]] = revenue / 2
            }
            else -> when (CompanyPayoutMode.determine(company)) {
                EVEN -> {
                    val owner = ((revenue / company.staffer.size) * 2).formatToTwoPlaces()
                    val staff = ((revenue - owner) / company.staffer.size).formatToTwoPlaces()

                    outputs[company.staffer[0]] = owner

                    company.staffer.drop(1).forEach {
                        outputs[it] = staff
                    }
                }
                USER -> {
                    var visited = 0
                    var revenue = revenue
                    val ordered = company.staffer.sortedByDescending { company.finance[it].payoutRatio }

                    ordered.forEach {
                        val percent = company.finance[it].payoutRatio
                        val payouts = ((revenue / (company.staffer.size - visited)) * percent).formatToTwoPlaces()

                        if (percent != 1) {
                            visited += 1
                            revenue -= payouts
                        }

                        outputs[it] = payouts
                    }
                }
            }
        }*/

        return outputs
    }


    enum class CompanyPayoutMode {

        EVEN,
        USER;


        companion object {

            fun determine(company: Company): CompanyPayoutMode {
                if (company.finance.account.values.all { it.payoutRatio == 1 }) {
                    return EVEN
                }

                return USER
            }

        }

    }

}