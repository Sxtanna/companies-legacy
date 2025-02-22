package com.sxtanna.aspiriamc.hooks

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.PluginDependant
import com.sxtanna.aspiriamc.base.Result
import net.milkbowl.vault.economy.Economy
import org.bukkit.OfflinePlayer
import java.util.*

class EconomyHook(override val plugin: Companies) : PluginDependant {

    private var economy: Economy? = null


    internal fun attemptHook(): Boolean {
        try {
            economy = server.servicesManager.getRegistration(Economy::class.java)?.provider
        } catch (ex: Exception) {
            logger.severe("Vault dependency not found: ${ex.message}")
            ex.printStackTrace()

            plugin.reportsManager.reportException(ex)
        }

        return economy != null
    }


    fun attemptTake(player: OfflinePlayer, cost: Double): Result<Unit> = Result.of {
        val response = checkNotNull(economy?.withdrawPlayer(player, cost)) {
            "economy unavailable"
        }

        if (response.transactionSuccess().not()) {
            fail(response.errorMessage)
        }
    }

    fun attemptGive(player: OfflinePlayer, cost: Double): Result<Unit> = Result.of {
        val response = checkNotNull(economy?.depositPlayer(player, cost)) {
            "economy unavailable"
        }

        if (response.transactionSuccess().not()) {
            fail(response.errorMessage)
        }
    }


    fun attemptGive(uuid: UUID, cost: Double): Result<Unit> {
        val player = plugin.server.getPlayer(uuid) ?: plugin.server.getOfflinePlayer(uuid)
        return attemptGive(player, cost)
    }

}