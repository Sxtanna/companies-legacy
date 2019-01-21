package com.sxtanna.aspiriamc.manager

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.config.Configs.MARKET_ICON_FEE
import com.sxtanna.aspiriamc.config.Configs.MARKET_ITEM_MAX
import com.sxtanna.aspiriamc.manager.base.Manager

class MarketsManager(override val plugin: Companies) : Manager("Markets") {

    val itemMax: Int
        get() = plugin.configsManager.get(MARKET_ITEM_MAX)
    val iconFee: Double
        get() = plugin.configsManager.get(MARKET_ICON_FEE)

}