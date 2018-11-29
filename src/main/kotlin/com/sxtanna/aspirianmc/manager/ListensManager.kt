package com.sxtanna.aspirianmc.manager

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.manager.base.Manager
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ListensManager(override val plugin: Companies) : Manager("Listeners"), Listener {

    private val onPlayerJoin = mutableListOf<PlayerJoinEvent.() -> Unit>()
    private val onPlayerQuit = mutableListOf<PlayerQuitEvent.() -> Unit>()


    override fun enable() {
        pluginManager.registerEvents(this, plugin)
    }

    override fun disable() {
        HandlerList.unregisterAll(this)
    }


    fun playerJoin(block: PlayerJoinEvent.() -> Unit) {
        onPlayerJoin += block
    }

    fun playerQuit(block: PlayerQuitEvent.() -> Unit) {
        onPlayerQuit += block
    }


    @EventHandler
    private fun PlayerJoinEvent.onJoin() {
        onPlayerJoin.forEach { it.invoke(this) }
    }

    @EventHandler
    private fun PlayerQuitEvent.onQuit() {
        onPlayerQuit.forEach { it.invoke(this) }
    }

}