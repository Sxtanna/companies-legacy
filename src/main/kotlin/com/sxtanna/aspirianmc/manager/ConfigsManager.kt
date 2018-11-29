package com.sxtanna.aspirianmc.manager

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.config.Configs
import com.sxtanna.aspirianmc.manager.base.Manager

class ConfigsManager(override val plugin: Companies) : Manager("Configs") {

    fun <T : Any> get(config: Configs<T>): T {
        return config.value(plugin.config)
    }

}