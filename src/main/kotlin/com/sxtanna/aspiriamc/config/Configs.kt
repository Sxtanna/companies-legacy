package com.sxtanna.aspiriamc.config

import com.sxtanna.aspiriamc.config.base.ConfigPath
import com.sxtanna.aspiriamc.database.base.DatabaseType
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

@Suppress("ClassName")
sealed class Configs<T : Any>(final override val path: String) : ConfigPath<T> {

    object COMPANY_MEMBER_MAX
        : Configs<Int>("company.member.max") {

        override fun value(yaml: ConfigurationSection): Int {
            return yaml.getInt(path, 100).coerceAtLeast(1)
        }

    }

    object COMPANY_CREATE_FEE
        : Configs<Double>("company.create-fee") {

        override fun value(yaml: ConfigurationSection): Double {
            return yaml.getDouble(path, 0.0).coerceAtLeast(0.0)
        }

    }

    object COMPANY_RENAME_FEE
        : Configs<Double>("company.rename-fee") {

        override fun value(yaml: ConfigurationSection): Double {
            return yaml.getDouble(path, 0.0).coerceAtLeast(0.0)
        }

    }


    object COMPANY_HISTORY_TIME
        : Configs<Long>("company.history-time") {

        override fun value(yaml: ConfigurationSection): Long {
            return yaml.getLong(path, 5L).coerceAtLeast(0L)
        }

    }

    object COMPANY_HISTORY_UNIT
        : Configs<TimeUnit>("company.history-unit") {

        override fun value(yaml: ConfigurationSection): TimeUnit {
            return yaml.getString(path, "DAYS").let { value ->
                TimeUnit.values().find { it.name.equals(value, true) } ?: TimeUnit.DAYS
            }
        }

    }


    object COMPANY_COMMAND_TOP_MAX
        : Configs<Int>("company.command.top-max") {

        override fun value(yaml: ConfigurationSection): Int {
            return yaml.getInt(path, 10).coerceAtLeast(0)
        }

    }

    object COMPANY_COMMAND_NAME_MAX
        : Configs<Int>("company.command.name-max") {

        override fun value(yaml: ConfigurationSection): Int {
            return yaml.getInt(path, 15).coerceAtLeast(1)
        }

    }


    object PAYOUTS_DEF_RATIO
        : Configs<Int>("company.payouts.def-ratio") {

        override fun value(yaml: ConfigurationSection): Int {
            return yaml.getInt(path, 75).coerceAtLeast(0)
        }

    }

    object PAYOUTS_DEF_TAXES
        : Configs<Int>("company.payouts.def-taxes") {

        override fun value(yaml: ConfigurationSection): Int {
            return yaml.getInt(path, 5).coerceAtLeast(0)
        }

    }

    object PAYOUTS_MAX_TAXES
        : Configs<Int>("company.payouts.max-taxes") {

        override fun value(yaml: ConfigurationSection): Int {
            return yaml.getInt(path, 75).coerceIn(0, 100)
        }

    }


    object HIRINGS_RESPOND_TIME
        : Configs<Long>("company.hirings.respond-time") {

        override fun value(yaml: ConfigurationSection): Long {
            return yaml.getLong(path, 120L).coerceAtLeast(0L)
        }

    }


    object SPONSOR_LIVE_TIME
        : Configs<Long>("company.sponsor.live-time") {

        override fun value(yaml: ConfigurationSection): Long {
            return yaml.getLong(path, 3600).coerceAtLeast(0)
        }

    }

    object SPONSOR_SLOT_COST
        : Configs<Double>("company.sponsor.slot-cost") {

        override fun value(yaml: ConfigurationSection): Double {
            return yaml.getDouble(path, 0.0).coerceAtLeast(0.0)
        }

    }


    object DISPLAY_DEF_ICON
        : Configs<Material>("company.icon.def-icon") {

        override fun value(yaml: ConfigurationSection): Material {
            val text = yaml.getString(path, "DIORITE")

            return try {
                Material.matchMaterial(text)
            } catch (ex: Exception) {
                Material.DIORITE
            }
        }

    }


    object MARKET_ITEM_MAX
        : Configs<Int>("markets.item-max") {

        override fun value(yaml: ConfigurationSection): Int {
            return yaml.getInt(path, -1)
        }

    }

    object MARKET_ICON_FEE
        : Configs<Double>("markets.icon-fee") {

        override fun value(yaml: ConfigurationSection): Double {
            return yaml.getDouble(path, 0.0).coerceAtLeast(0.0)
        }

    }


    object STORAGE_DATABASE_TYPE
        : Configs<DatabaseType>("storage.database-type") {

        override fun value(yaml: ConfigurationSection): DatabaseType {
            return DatabaseType.get(yaml.getString(path, "LOCAL").toUpperCase())
        }

    }


    companion object {

        fun values(): List<Configs<*>> {
            return listOf(COMPANY_MEMBER_MAX,
                          COMPANY_CREATE_FEE,
                          COMPANY_RENAME_FEE,

                          COMPANY_HISTORY_TIME,
                          COMPANY_HISTORY_UNIT,

                          COMPANY_COMMAND_TOP_MAX,
                          COMPANY_COMMAND_NAME_MAX,

                          PAYOUTS_DEF_RATIO,
                          PAYOUTS_DEF_TAXES,
                          PAYOUTS_MAX_TAXES,

                          HIRINGS_RESPOND_TIME,

                          SPONSOR_LIVE_TIME,
                          SPONSOR_SLOT_COST,

                          DISPLAY_DEF_ICON,

                          MARKET_ITEM_MAX,
                          MARKET_ICON_FEE)
        }

        fun valuesWithTypes(): Map<Configs<*>, KClass<*>> {
            return mapOf(
                COMPANY_MEMBER_MAX to Int::class,
                COMPANY_CREATE_FEE to Double::class,
                COMPANY_RENAME_FEE to Double::class,

                COMPANY_HISTORY_TIME to Long::class,
                COMPANY_HISTORY_UNIT to TimeUnit::class,

                COMPANY_COMMAND_TOP_MAX to Int::class,
                COMPANY_COMMAND_NAME_MAX to Int::class,

                PAYOUTS_DEF_RATIO to Int::class,
                PAYOUTS_DEF_TAXES to Int::class,
                PAYOUTS_MAX_TAXES to Int::class,

                HIRINGS_RESPOND_TIME to Long::class,

                SPONSOR_LIVE_TIME to Long::class,
                SPONSOR_SLOT_COST to Double::class,

                DISPLAY_DEF_ICON to Material::class,

                MARKET_ITEM_MAX to Int::class,
                MARKET_ICON_FEE to Double::class)
        }

        fun valueOf(name: String): Configs<*>? {
            return values().find { it::class.java.simpleName.equals(name, true) }
        }

    }

}