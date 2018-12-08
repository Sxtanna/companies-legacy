package com.sxtanna.aspiriamc.hooks

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.PluginDependant
import com.sxtanna.aspiriamc.base.Result
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.OfflinePlayer
import java.util.*

class VaultHook(override val plugin: Companies) : PluginDependant {

    private var economy: Economy? = null


    internal fun attemptHook(): Boolean {
        try {
            economy = server.servicesManager.getRegistration(Economy::class.java)?.provider
        } catch (ex: Exception) {
            logger.severe("Vault dependency not found: ${ex.message}")
            ex.printStackTrace()
        }

        return economy != null
    }


    fun attemptTake(player: OfflinePlayer, cost: Double): Result<EconomyResponse> = Result.of {
        checkNotNull(economy?.withdrawPlayer(player, cost)) {
            "economy unavailable"
        }
    }

    fun attemptGive(player: OfflinePlayer, cost: Double): Result<EconomyResponse> = Result.of {
        checkNotNull(economy?.depositPlayer(player, cost)) {
            "economy unavailable"
        }
    }


    fun attemptGive(uuid: UUID, cost: Double): Result<EconomyResponse> {
        val player = plugin.server.getPlayer(uuid) ?: plugin.server.getOfflinePlayer(uuid)
        return attemptGive(player, cost)
    }

}