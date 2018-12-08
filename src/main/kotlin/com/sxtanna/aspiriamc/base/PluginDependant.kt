package com.sxtanna.aspiriamc.base

import com.sxtanna.aspiriamc.Companies
import org.bukkit.Server
import org.bukkit.command.CommandMap
import org.bukkit.entity.Player
import org.bukkit.plugin.PluginManager
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.util.*
import java.util.logging.Logger

interface PluginDependant {

    val plugin: Companies


    val logger: Logger
        get() = plugin.logger

    val server: Server
        get() = plugin.server


    val scheduler: BukkitScheduler
        get() = plugin.server.scheduler

    val commandMap: CommandMap
        get() = plugin.server.commandMap

    val pluginManager: PluginManager
        get() = plugin.server.pluginManager

    val onlinePlayers: List<Player>
        get() = plugin.server.onlinePlayers.toList()


    val pluginFolder: File
        get() = plugin.dataFolder

    val serverFolder: File
        get() = pluginFolder.parentFile.parentFile



    fun findPlayerByUUID(uuid: UUID): Player? {
        return server.getPlayer(uuid)
    }

    fun findPlayerByName(name: String): Player? {
        return server.getPlayer(name)
    }


    fun bukkitRunnable(block: BukkitRunnable.() -> Unit): BukkitRunnable = PluginRunnable(block)

    fun BukkitRunnable.runTaskLater(delay: Long): BukkitTask {
        return runTaskLater(plugin, delay)
    }

    fun BukkitRunnable.runTaskLaterAsync(delay: Long): BukkitTask {
        return runTaskLaterAsynchronously(plugin, delay)
    }

    fun BukkitRunnable.runTaskTimer(period: Long, delay: Long = 0L): BukkitTask {
        return runTaskTimer(plugin, delay, period)
    }

    fun BukkitRunnable.runTaskTimerAsync(period: Long, delay: Long = 0L): BukkitTask {
        return runTaskTimerAsynchronously(plugin, delay, period)
    }


    fun sync(block: () -> Unit): BukkitTask {
        return scheduler.runTask(plugin, block)
    }

    fun async(block: () -> Unit): BukkitTask {
        return scheduler.runTaskAsynchronously(plugin, block)
    }


    fun delay(delay: Long, block: BukkitRunnable.() -> Unit): BukkitTask {
        return bukkitRunnable(block).runTaskLater(delay)
    }

    fun delayAsync(delay: Long, block: BukkitRunnable.() -> Unit): BukkitTask {
        return bukkitRunnable(block).runTaskLaterAsync(delay)
    }

    fun repeat(period: Long, delay: Long = 0L, block: BukkitRunnable.() -> Unit): BukkitTask {
        return bukkitRunnable(block).runTaskTimer(period, delay)
    }

    fun repeatAsync(period: Long, delay: Long = 0L, block: BukkitRunnable.() -> Unit): BukkitTask {
        return bukkitRunnable(block).runTaskTimerAsync(period, delay)
    }


    private class PluginRunnable(val block: BukkitRunnable.() -> Unit) : BukkitRunnable() {
        override fun run() = block()
    }

}