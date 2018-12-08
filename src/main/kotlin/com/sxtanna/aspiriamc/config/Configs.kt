package com.sxtanna.aspiriamc.config

import com.sxtanna.aspiriamc.config.base.ConfigPath
import com.sxtanna.aspiriamc.database.base.DatabaseType
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection

@Suppress("ClassName")
sealed class Configs<T : Any>(final override val pathString: String) : ConfigPath<T> {

    object COMPANY_MEMBER_MAX
        : Configs<Int>("company.member.max") {

        override fun value(yaml: ConfigurationSection): Int {
            return yaml.getInt(pathString, 100).coerceAtLeast(1)
        }

    }

    object COMPANY_CREATE_FEE
        : Configs<Double>("company.create-fee") {

        override fun value(yaml: ConfigurationSection): Double {
            return yaml.getDouble(pathString, 0.0).coerceAtLeast(0.0)
        }

    }

    object COMPANY_RENAME_FEE
        : Configs<Double>("company.rename-fee") {

        override fun value(yaml: ConfigurationSection): Double {
            return yaml.getDouble(pathString, 0.0).coerceAtLeast(0.0)
        }

    }


    object COMPANY_COMMAND_TOP_MAX
        : Configs<Long>("company.command.top-max") {

        override fun value(yaml: ConfigurationSection): Long {
            return yaml.getLong(pathString, 10L).coerceAtLeast(0)
        }

    }

    object COMPANY_COMMAND_NAME_MAX
        : Configs<Int>("company.command.name-max") {

        override fun value(yaml: ConfigurationSection): Int {
            return yaml.getInt(pathString, 15).coerceAtLeast(1)
        }

    }


    object PAYOUTS_DEF_RATIO
        : Configs<Int>("company.payouts.def-ratio") {

        override fun value(yaml: ConfigurationSection): Int {
            return yaml.getInt(pathString, 75).coerceAtLeast(0)
        }

    }

    object PAYOUTS_DEF_TAXES
        : Configs<Int>("company.payouts.def-taxes") {

        override fun value(yaml: ConfigurationSection): Int {
            return yaml.getInt(pathString, 5).coerceAtLeast(0)
        }

    }

    object PAYOUTS_MAX_TAXES
        : Configs<Int>("company.payouts.max-taxes") {

        override fun value(yaml: ConfigurationSection): Int {
            return yaml.getInt(pathString, 75).coerceIn(0, 100)
        }

    }


    object HIRINGS_RESPOND_TIME
        : Configs<Long>("company.hirings.respond-time") {

        override fun value(yaml: ConfigurationSection): Long {
            return yaml.getLong(pathString, 120L).coerceAtLeast(0L)
        }

    }

    object SPONSOR_LIVE_TIME
        : Configs<Long>("company.sponsor.live-time") {

        override fun value(yaml: ConfigurationSection): Long {
            return yaml.getLong(pathString, 3600).coerceAtLeast(0)
        }

    }

    object SPONSOR_SLOT_COST
        : Configs<Double>("company.sponsor.slot-cost") {

        override fun value(yaml: ConfigurationSection): Double {
            return yaml.getDouble(pathString, 0.0).coerceAtLeast(0.0)
        }

    }


    object DISPLAY_DEF_ICON
        : Configs<Material>("company.icon.def-icon") {
        
        override fun value(yaml: ConfigurationSection): Material {
            val text = yaml.getString(pathString, "DIORITE")

            return try {
                Material.matchMaterial(text)
            }
            catch (ex: Exception) {
                Material.DIORITE
            }
        }

    }


    object MARKET_ITEM_MAX
        : Configs<Int>("markets.item-max") {

        override fun value(yaml: ConfigurationSection): Int {
            return yaml.getInt(pathString, -1)
        }

    }

    object MARKET_ICON_FEE
        : Configs<Double>("markets.icon-fee") {

        override fun value(yaml: ConfigurationSection): Double {
            return yaml.getDouble(pathString, 0.0).coerceAtLeast(0.0)
        }

    }


    object STORAGE_DATABASE_TYPE
        : Configs<DatabaseType>("storage.database-type") {

        override fun value(yaml: ConfigurationSection): DatabaseType {
            return DatabaseType.get(yaml.getString(pathString, "LOCAL").toUpperCase())
        }

    }

}