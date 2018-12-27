package com.sxtanna.aspiriamc.manager

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.config.Garnish
import com.sxtanna.aspiriamc.config.Garnish.SoundGarnish
import com.sxtanna.aspiriamc.exts.ensureUsable
import com.sxtanna.aspiriamc.manager.base.Manager
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

class GarnishManager(override val plugin: Companies) : Manager("Garnish") {

    private val file = pluginFolder.resolve("garnish.yml")

    private val conf: FileConfiguration
        get() = YamlConfiguration.loadConfiguration(file.ensureUsable())


    override fun enable() {
        if (file.exists()) return

        file.ensureUsable().writeText(Garnish.defaults)
    }


    fun send(sender: CommandSender, garnish: Garnish<*>) {
        try {
            when (garnish) {
                is SoundGarnish -> {
                    if (sender !is Player) return

                    when (val sound = garnish.value(conf)) {
                        null -> {
                            // aww no sound ;(
                        }
                        else -> {
                            sender.playSound(sender.eyeLocation, sound, 1F, 1F)
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            plugin.logger.warning("failed to send garnish ${garnish::class.simpleName} to ${sender.name}: ${ex.message}")
            plugin.reportsManager.reportException(ex)
        }
    }

}