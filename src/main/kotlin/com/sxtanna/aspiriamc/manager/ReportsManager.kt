package com.sxtanna.aspiriamc.manager

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.Result.Some
import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer
import com.sxtanna.aspiriamc.exts.formatToTwoPlaces
import com.sxtanna.aspiriamc.manager.base.Manager
import com.sxtanna.aspiriamc.market.Product

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
        plugin.economyHook.attemptGive(stafferUUID, revenue)

        revenue -= payouts

        if (company.staffer.size == 1) return

        revenue /= company.staffer.size - 1

        company.staffer.forEach {
            if (it == stafferUUID) return@forEach

            company.finance[it].playerPayout += revenue
            plugin.economyHook.attemptGive(it, revenue)
        }
    }

}