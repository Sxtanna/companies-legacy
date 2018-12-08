package com.sxtanna.aspiriamc.manager

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.config.Configs
import com.sxtanna.aspiriamc.manager.base.Manager

class ConfigsManager(override val plugin: Companies) : Manager("Configs") {

    fun <T : Any> get(config: Configs<T>): T {
        return config.value(plugin.config)
    }

}