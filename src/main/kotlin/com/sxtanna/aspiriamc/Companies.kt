package com.sxtanna.aspiriamc

import com.sxtanna.aspiriamc.company.Company
import com.sxtanna.aspiriamc.company.Staffer
import com.sxtanna.aspiriamc.config.Configs.STORAGE_DATABASE_TYPE
import com.sxtanna.aspiriamc.database.base.CompanyDatabase
import com.sxtanna.aspiriamc.exts.TimChatMenuAPI
import com.sxtanna.aspiriamc.hooks.EconomyHook
import com.sxtanna.aspiriamc.manager.*
import com.sxtanna.aspiriamc.manager.base.Manager
import com.sxtanna.aspiriamc.market.menu.GlobalCompanyMarketMenu
import com.sxtanna.aspiriamc.menu.Menu
import com.sxtanna.korm.Korm
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType.*
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType.SlotType.OUTSIDE
import org.bukkit.inventory.PlayerInventory
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class Companies : JavaPlugin() {

    internal val korm = Korm()
    internal val managers = mutableListOf<Manager>()


    lateinit var economyHook: EconomyHook
        private set
    lateinit var companyDatabase: CompanyDatabase
        private set

    lateinit var configsManager: ConfigsManager
        private set
    lateinit var garnishManager: GarnishManager
        private set

    lateinit var messageManager: MessageManager
        private set
    lateinit var listensManager: ListensManager
        private set
    lateinit var commandManager: CommandManager
        private set

    lateinit var marketsManager: MarketsManager
        private set

    lateinit var stafferManager: StafferManager
        private set
    lateinit var companyManager: CompanyManager
        private set
    lateinit var hiringsManager: HiringsManager
        private set
    lateinit var reportsManager: ReportsManager
        private set


    override fun onLoad() {
        saveDefaultConfig()

        economyHook = EconomyHook(this)

        configsManager = ConfigsManager(this)
        garnishManager = GarnishManager(this)

        messageManager = MessageManager(this)
        listensManager = ListensManager(this)
        commandManager = CommandManager(this)

        marketsManager = MarketsManager(this)

        stafferManager = StafferManager(this)
        hiringsManager = HiringsManager(this)
        companyManager = CompanyManager(this)
        reportsManager = ReportsManager(this)

        companyDatabase = configsManager.get(STORAGE_DATABASE_TYPE).createDatabase(this)

        managers += listOf(configsManager, garnishManager, messageManager, listensManager, commandManager, marketsManager, stafferManager, hiringsManager, companyManager, reportsManager)
    }

    override fun onEnable() {
        if (economyHook.attemptHook().not()) {
            logger.severe("Failed to hook into vault, disabling")
            server.pluginManager.disablePlugin(this)
            return
        }

        MenuListener.load(this)

        try {
            companyDatabase.load()
        } catch (ex: Exception) {
            logger.severe("Failed to load the database: ${ex.message}")
            ex.printStackTrace()
            reportsManager.reportException(ex)

            server.pluginManager.disablePlugin(this)
            return
        }

        managers.forEach {
            try {
                it.enable()
                logger.info("Enabled Manager:${it.name}")

                it.enabled = true
            } catch (ex: Exception) {
                logger.severe("Failed to enable Manager:${it.name} =S=")
                ex.printStackTrace()
                logger.severe("Failed to enable Manager:${it.name} =E=")

                reportsManager.reportException(ex)
            }
        }
    }

    override fun onDisable() {
        MenuListener.kill()

        managers.forEach {
            try {
                it.disable()
                logger.info("Disabled Manager:${it.name}")

                it.enabled = false
            } catch (ex: Exception) {
                logger.severe("Failed to disable Manager:${it.name} =S=")
                ex.printStackTrace()
                logger.severe("Failed to disable Manager:${it.name} =E=")

                reportsManager.reportException(ex)
            }
        }
    }


    internal fun quickAccessStaffer(stafferUUID: UUID): Staffer? {
        return stafferManager.cache[stafferUUID]
    }

    internal fun quickAccessCompanyByStafferUUID(stafferUUID: UUID): Company? {
        return companyManager.cache[quickAccessStaffer(stafferUUID)?.companyUUID ?: return null]
    }

    internal fun quickAccessCompanyByCompanyUUID(companyUUID: UUID?): Company? {
        return companyManager.cache[companyUUID ?: return null]
    }


    private object MenuListener : Listener {

        private val invalid = EnumSet.of(NUMBER_KEY, DOUBLE_CLICK, DROP, CONTROL_DROP, CREATIVE, UNKNOWN)


        @EventHandler
        private fun InventoryClickEvent.onMenuClick() {
            val menu = (inventory?.holder as? Menu) ?: return

            isCancelled = true

            if (clickedInventory is PlayerInventory || click in invalid) {
                return
            }

            val player = whoClicked as Player

            if (slotType == OUTSIDE) {
                menu.onEmptyClick(player, click)
            } else {
                menu.onSlotsClick(player, click, slot)
                menu.push(player, click, slot)
            }
        }

        @EventHandler
        private fun InventoryCloseEvent.onMenuClose() {
            val menu = (inventory?.holder as? Menu) ?: return

            menu.onClose(player as Player)
        }


        internal fun load(plugin: Companies) {
            plugin.server.pluginManager.registerEvents(MenuListener, plugin)
            GlobalCompanyMarketMenu.loadRefresher(plugin)
            TimChatMenuAPI.init(plugin)
        }

        internal fun kill() {
            HandlerList.unregisterAll(MenuListener)
            GlobalCompanyMarketMenu.killRefresher()
            TimChatMenuAPI.disable()
        }

    }

}