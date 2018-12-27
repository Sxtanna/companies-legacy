package com.sxtanna.aspiriamc.base

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.PluginDependant.PluginTask.Cont
import org.bukkit.Bukkit
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


    class PluginRunnable(val block: BukkitRunnable.() -> Unit) : BukkitRunnable() {
        override fun run() = block()
    }

    sealed class PluginTask<I : Any, O : Any> : PluginDependant {

        abstract val cont: Cont
        abstract val task: (I) -> O

        protected lateinit var dataI: I
        protected lateinit var dataO: O

        protected lateinit var head: PluginTask<*, I>
        protected lateinit var tail: PluginTask<O, *>

        private var done = false


        fun exec() {
            if (::head.isInitialized && head.done.not()) {
                return head.exec()
            }

            cont.exec(plugin, { task.invoke(dataI) }) {
                dataO = it
                done = true

                if (::tail.isInitialized) {
                    tail.dataI = it
                    tail.exec()
                }
            }
        }

        fun <NO : Any> then(cont: Cont, task: (O) -> NO): PluginTask<O, NO> {
            val next = Next(plugin, cont, task)

            this.tail = next
            next.head = this

            return next
        }


        fun data(): Result<O> {
            return Result.of {
                dataO
            }
        }


        class Head<O : Any>(override val plugin: Companies, override val cont: Cont, override val task: (Unit) -> O)
            : PluginTask<Unit, O>() {

            init {
                dataI = Unit
            }

        }

        class Next<I : Any, O : Any>(override val plugin: Companies, override val cont: Cont, override val task: (I) -> O)
            : PluginTask<I, O>()


        sealed class Cont {

            abstract fun <O : Any> exec(plugin: Companies, function: () -> O, callback: (O) -> Unit)


            object Sync : Cont() {

                override fun <O : Any> exec(plugin: Companies, function: () -> O, callback: (O) -> Unit) {
                    if (Bukkit.isPrimaryThread()) {
                        callback.invoke(function.invoke())
                    } else plugin.companyManager.sync {
                        callback.invoke(function.invoke())
                    }
                }

            }

            object ASync : Cont() {

                override fun <O : Any> exec(plugin: Companies, function: () -> O, callback: (O) -> Unit) {
                    if (Bukkit.isPrimaryThread()) plugin.companyManager.async {
                        callback.invoke(function.invoke())
                    }
                    else {
                        callback.invoke(function.invoke())
                    }
                }

            }

        }

    }


    // task crap

    fun <T : Any> task(cont: Cont, block: () -> T) = PluginTask.Head(plugin, cont) {
        block.invoke()
    }


}