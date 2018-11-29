package com.sxtanna.aspirianmc.manager

import com.sxtanna.aspirianmc.Companies
import com.sxtanna.aspirianmc.config.Configs.MARKET_ICON_FEE
import com.sxtanna.aspirianmc.config.Configs.MARKET_ITEM_MAX
import com.sxtanna.aspirianmc.manager.base.Manager

class MarketsManager(override val plugin: Companies) : Manager("Markets") {

    var itemMax = -1
        private set
    var iconFee = 0.0
        private set


    override fun enable() {
        itemMax = plugin.configsManager.get(MARKET_ITEM_MAX)
        iconFee = plugin.configsManager.get(MARKET_ICON_FEE)
    }

}