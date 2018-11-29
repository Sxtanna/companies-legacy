package com.sxtanna.aspirianmc.config

import com.sxtanna.aspirianmc.config.base.ConfigPath
import org.bukkit.Sound
import org.bukkit.Sound.*
import org.bukkit.configuration.ConfigurationSection
import org.intellij.lang.annotations.Language

@Suppress("ClassName")
sealed class Garnish<T : Any?> : ConfigPath<T> {

    abstract val default: T


    abstract class SoundGarnish(final override val pathString: String) : Garnish<Sound?>() {

        abstract override val default: Sound

        override fun value(yaml: ConfigurationSection): Sound? {
            val value = yaml.getString(pathString, default.name).toUpperCase()

            return if (value == "NONE") {
                null
            }
            else {
                Sound.valueOf(value)
            }
        }

    }


    object MENU_BUTTON_BACK : SoundGarnish("menu.button-back") {

        override val default = UI_BUTTON_CLICK

    }

    object MENU_BUTTON_CLICK : SoundGarnish("menu.button-click") {

        override val default = UI_BUTTON_CLICK

    }


    object COMPANY_PAYOUTS_RESET : SoundGarnish("company.payouts-reset") {

        override val default = ENTITY_BAT_LOOP

    }

    object COMPANY_PAYOUTS_CHANGE : SoundGarnish("company.payouts-change") {

        override val default = BLOCK_NOTE_BLOCK_PLING

    }


    object COMPANY_ICON_PURCHASE_PASS : SoundGarnish("company.icon-purchase.pass") {

        override val default = ENTITY_PLAYER_LEVELUP

    }

    object COMPANY_ICON_PURCHASE_FAIL : SoundGarnish("company.icon-purchase.fail") {

        override val default = BLOCK_ANVIL_LAND

    }


    object COMPANY_CREATE_PURCHASE_PASS : SoundGarnish("company.create-purchase.pass") {

        override val default = ENTITY_PLAYER_LEVELUP

    }

    object COMPANY_CREATE_PURCHASE_FAIL : SoundGarnish("company.create-purchase.fail") {

        override val default = BLOCK_ANVIL_LAND

    }


    object COMPANY_SPONSOR_PURCHASE_PASS : SoundGarnish("company.sponsor-purchase.pass") {

        override val default = ENTITY_PLAYER_LEVELUP

    }

    object COMPANY_SPONSOR_PURCHASE_FAIL : SoundGarnish("company.sponsor-purchase.fail") {

        override val default = BLOCK_ANVIL_LAND

    }


    object MARKET_PRODUCT_PURCHASE_PASS : SoundGarnish("markets.product-purchase.pass") {

        override val default = ENTITY_ITEM_PICKUP

    }

    object MARKET_PRODUCT_PURCHASE_FAIL : SoundGarnish("markets.product-purchase.fail") {

        override val default = BLOCK_ANVIL_LAND

    }


    internal companion object {

        @Language("YAML")
        val defaults =
                """
                  menu:
                    button-back: UI_BUTTON_CLICK
                    button-click: UI_BUTTON_CLICK
                  markets:
                    product-purchase:
                      pass: ENTITY_ITEM_PICKUP
                      fail: BLOCK_ANVIL_LAND
                  company:
                    payouts-reset: ENTITY_BAT_LOOP
                    payouts-change: BLOCK_NOTE_BLOCK_PLING
                    icon-purchase:
                      pass: ENTITY_PLAYER_LEVELUP
                      fail: BLOCK_ANVIL_LAND
                    create-purchase:
                      pass: ENTITY_PLAYER_LEVELUP
                      fail: BLOCK_ANVIL_LAND
                    sponsor-purchase:
                      pass: ENTITY_PLAYER_LEVELUP
                      fail: BLOCK_ANVIL_LAND
                """.trimIndent()

    }



}