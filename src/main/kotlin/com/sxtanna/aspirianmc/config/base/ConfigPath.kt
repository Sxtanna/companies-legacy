package com.sxtanna.aspirianmc.config.base

import org.bukkit.configuration.ConfigurationSection

interface ConfigPath<T : Any?> {

    val pathString: String


    fun value(yaml: ConfigurationSection): T

}