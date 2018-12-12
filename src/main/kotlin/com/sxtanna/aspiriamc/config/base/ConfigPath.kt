package com.sxtanna.aspiriamc.config.base

import org.bukkit.configuration.ConfigurationSection

interface ConfigPath<T : Any?> {

    val path: String


    fun value(yaml: ConfigurationSection): T

}