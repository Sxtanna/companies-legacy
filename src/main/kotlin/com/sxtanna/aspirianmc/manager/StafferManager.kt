package com.sxtanna.aspirianmc.manager

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.base.Result
import com.sxtanna.aspirianmc.company.Staffer
import com.sxtanna.aspirianmc.company.Staffer.AccountType
import com.sxtanna.aspirianmc.company.Staffer.AccountType.NEW
import com.sxtanna.aspirianmc.company.Staffer.AccountType.OLD
import com.sxtanna.aspirianmc.exts.ensureUsable
import com.sxtanna.aspirianmc.manager.base.Manager
import org.bukkit.entity.Player
import java.io.File
import java.util.*


class StafferManager(override val plugin: Companies) : Manager("Staffers") {

    internal val cache = mutableMapOf<UUID, Staffer>()
    internal val names = NameCacheCauseWhyNot()


    override fun enable() {
        plugin.listensManager.playerJoin {
            load(player.uniqueId) { _, _ -> }

            names.joinExec(player)
        }
        plugin.listensManager.playerQuit {
            save(player.uniqueId, true)
        }

        onlinePlayers.forEach {
            // load all relevant staffer data
            load(it.uniqueId)
        }
    }

    override fun disable() {
        cache.values.forEach { plugin.database.saveStaffer(it) }
        cache.clear()
    }


    fun get(uuid: UUID, whenLoaded: (data: Staffer, type: AccountType) -> Unit = { _, _ -> }): Result<Staffer> {
        return Result.of {
            val existed = cache[uuid]

            if (existed != null) {
                existed
            }
            else {
                load(uuid, whenLoaded)
                fail("loading staffer account")
            }
        }
    }


    private fun load(uuid: UUID, whenLoaded: (data: Staffer, type: AccountType) -> Unit = { _, _ -> }) {
        plugin.database.loadStaffer(uuid) {
            val data = it ?: Staffer(uuid)

            cache[data.uuid] = data

            // ensure their company still exists
            data.companyUUID?.let { companyUUID ->
                val company = plugin.companyManager.cache[companyUUID]

                if (company == null) {
                    data.companyUUID = null

                    findPlayerByUUID(uuid)?.let { player ->
                        reply(player, "&cyour company was closed while you were offline")
                    }
                }
            }

            whenLoaded.invoke(data, if (it == null) NEW else OLD)
        }
    }

    private fun save(uuid: UUID, remove: Boolean = false) {
        val data = if (remove) {
            cache.remove(uuid)
        }
        else {
            cache[uuid]
        }

        plugin.database.saveStaffer(data ?: return)

        val company = plugin.companyManager.cache[uuid] ?: return

        val players = company.onlineStaffers()
        if (players.size > 1) return
        if (players.getOrNull(0)?.uniqueId != uuid) return

        plugin.companyManager.save(company.uuid, false)
    }


    inner class NameCacheCauseWhyNot {

        internal val cache = mutableMapOf<UUID, String>()


        operator fun get(uuid: UUID): String {
            return plugin.server.getPlayer(uuid)?.name ?: cache[uuid] ?: load(uuid) ?: "null"
        }

        operator fun set(uuid: UUID, name: String)  {
            val file = ensureUsable(pluginFolder.resolve("name-cache").resolve("$uuid.dat"))
            file.writeText(name)

            cache[uuid] = name
        }


        internal fun joinExec(player: Player) {
            val name = load(player.uniqueId)

            if (name == null || name != player.name) {
                set(player.uniqueId, player.name)
            }
        }

        private fun load(uuid: UUID): String? {
            return pluginFolder.resolve("name-cache").resolve("$uuid.dat").takeIf(File::exists)?.readText().apply {
                if (this != null) cache[uuid] = this
            }
        }

    }

}