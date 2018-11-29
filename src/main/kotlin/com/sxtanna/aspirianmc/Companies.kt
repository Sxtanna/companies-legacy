package com.sxtanna.aspirianmc

import com.sxtanna.aspirianmc.company.Company
import com.sxtanna.aspirianmc.company.Staffer
import com.sxtanna.aspirianmc.config.Configs.STORAGE_DATABASE_TYPE
import com.sxtanna.aspirianmc.database.base.CompanyDatabase
import com.sxtanna.aspirianmc.hooks.VaultHook
import com.sxtanna.aspirianmc.manager.*
import com.sxtanna.aspirianmc.manager.base.Manager
import com.sxtanna.aspirianmc.menu.Menu
import com.sxtanna.korm.Korm
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType.SlotType.OUTSIDE
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class Companies : JavaPlugin() {

    private val korm = Korm()
    private val managers = mutableListOf<Manager>()


    lateinit var vaultHook: VaultHook

    lateinit var database: CompanyDatabase


    lateinit var configsManager: ConfigsManager
    lateinit var garnishManager: GarnishManager

    lateinit var listensManager: ListensManager
    lateinit var commandManager: CommandManager

    lateinit var marketsManager: MarketsManager

    lateinit var stafferManager: StafferManager
    lateinit var hiringsManager: HiringsManager
    lateinit var companyManager: CompanyManager
    lateinit var reportsManager: ReportsManager


    override fun onLoad() {
        saveDefaultConfig()

        vaultHook = VaultHook(this)

        configsManager = ConfigsManager(this)
        garnishManager = GarnishManager(this)

        listensManager = ListensManager(this)
        commandManager = CommandManager(this)

        marketsManager = MarketsManager(this)

        stafferManager = StafferManager(this)
        hiringsManager = HiringsManager(this)
        companyManager = CompanyManager(this)
        reportsManager = ReportsManager(this)

        database = configsManager.get(STORAGE_DATABASE_TYPE).createDatabase(this)

        managers += listOf(configsManager, garnishManager, listensManager, commandManager, marketsManager, stafferManager, hiringsManager, companyManager, reportsManager)
    }

    override fun onEnable() {
        if (vaultHook.attemptHook().not()) {
            logger.severe("Failed to hook into vault, disabling")
            server.pluginManager.disablePlugin(this)
            return
        }

        try {
            database.load()
        }
        catch (ex: Exception) {
            logger.severe("Failed to load the database: ${ex.message}")
            ex.printStackTrace()

            server.pluginManager.disablePlugin(this)
            return
        }

        managers.forEach {
            try {
                it.enable()
                logger.info("Enabled Manager:${it.name}")

                it.enabled = true
            }
            catch (ex: Exception) {
                logger.severe("Failed to enable Manager:${it.name} =S=")
                ex.printStackTrace()
                logger.severe("Failed to enable Manager:${it.name} =E=")
            }
        }

        server.pluginManager.registerEvents(MenuListener, this)
    }

    override fun onDisable() {
        managers.forEach {
            try {
                it.disable()
                logger.info("Disabled Manager:${it.name}")

                it.enabled = false
            }
            catch (ex: Exception) {
                logger.severe("Failed to disable Manager:${it.name} =S=")
                ex.printStackTrace()
                logger.severe("Failed to disable Manager:${it.name} =E=")
            }
        }

        HandlerList.unregisterAll(MenuListener)
    }


    internal fun korm() = korm


    internal fun quickAccessStaffer(stafferUUID: UUID): Staffer? {
        return stafferManager.cache[stafferUUID]
    }

    internal fun quickAccessCompany(stafferUUID: UUID): Company? {
        return companyManager.cache[quickAccessStaffer(stafferUUID)?.companyUUID ?: return null]
    }


    private object MenuListener : Listener {

        @EventHandler
        private fun InventoryClickEvent.onMenuClick() {
            val menu = (inventory?.holder as? Menu) ?: return

            isCancelled = true

            val player = whoClicked as Player

            if (slotType == OUTSIDE) {
                menu.onEmptyClick(player, click)
            }
            else {
                menu.onSlotsClick(player, click, slot)
                menu.push(player, click, slot)
            }
        }

        @EventHandler
        private fun InventoryCloseEvent.onMenuClose() {
            val menu = (inventory?.holder as? Menu) ?: return

            menu.onClose(player as Player)
        }

    }

}