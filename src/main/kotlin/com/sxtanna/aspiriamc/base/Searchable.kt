package com.sxtanna.aspiriamc.base

import com.sxtanna.aspiriamc.base.Searchable.Query.CostQuery.CostType.ABOVE
import com.sxtanna.aspiriamc.base.Searchable.Query.CostQuery.CostType.BELOW
import com.sxtanna.aspiriamc.exts.buildItemStack
import com.sxtanna.aspiriamc.exts.hideEverything
import com.sxtanna.aspiriamc.exts.properName
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.Enchantment.DAMAGE_ALL
import org.bukkit.inventory.ItemStack

interface Searchable {

    fun passes(query: Query): Boolean


    sealed class Query : Iconable {

        var enabled = false


        abstract fun createLore(): List<String>


        data class NameQuery(var name: String = "")
            : Query() {

            override fun createLore(): List<String> {
                return listOf("&6Named",
                              "  &7\"&f$name&7\"")
            }

            override fun createIcon(): ItemStack {
                return buildItemStack(PAPER) {
                    hideEverything()

                    displayName = "&eName"

                    if (enabled) {
                        addEnchant(DAMAGE_ALL, 1, true)
                    }

                    if (name.isNotBlank()) {
                        lore = listOf("",
                                      "&7\"&f$name&7\"")
                    }
                }
            }

        }

        data class CostQuery(var cost: Double = -1.0, var type: CostType = BELOW)
            : Query() {

            override fun createLore(): List<String> {
                return listOf("&6Cost ${if (type == ABOVE) "More Than" else "Less Than"}",
                              "  &a$$cost")
            }

            override fun createIcon(): ItemStack {
                return buildItemStack(SUNFLOWER) {
                    hideEverything()

                    displayName = "&eCost"

                    if (enabled) {
                        addEnchant(DAMAGE_ALL, 1, true)
                    }

                    if (cost > -1.0) {
                        lore = listOf("",
                                      "&f${if (type == ABOVE) "More Than" else "Less Than"}",
                                      "  &a$$cost")
                    }
                }
            }

            enum class CostType {

                ABOVE,
                BELOW,

            }

        }

        data class TypeQuery(var type: Material = AIR)
            : Query() {

            override fun createLore(): List<String> {
                return listOf("&6Type",
                              "  &7${type.properName()}")
            }

            override fun createIcon(): ItemStack {
                return buildItemStack(GRASS_BLOCK) {
                    hideEverything()

                    displayName = "&eType"

                    if (enabled) {
                        addEnchant(DAMAGE_ALL, 1, true)
                    }

                    if (type != AIR) {
                        lore = listOf("",
                                      "&f${type.properName()}")
                    }
                }
            }

        }

        sealed class DataQuery
            : Query() {


            data class Count(var count: Int = -1)
                : DataQuery() {

                override fun createLore(): List<String> {
                    return listOf("&6Count",
                                  "  &7$count")
                }

                override fun createIcon(): ItemStack {
                    return buildItemStack(MAP) {
                        hideEverything()

                        displayName = "&eCount"

                        if (enabled) {
                            addEnchant(DAMAGE_ALL, 1, true)
                        }

                        if (count != -1) {
                            lore = listOf("",
                                          "&f$count")
                        }
                    }
                }

            }

            data class Color(var color: DyeColor? = null)
                : DataQuery() {

                override fun createLore(): List<String> {
                    return listOf("&6Color",
                                  "  &7${color?.properName() ?: "???"}")
                }

                override fun createIcon(): ItemStack {
                    val type = when (val col = color) {
                        null -> {
                            FIREWORK_STAR
                        }
                        else -> {
                            Material.getMaterial("${col.name}_WOOL")
                        }
                    }

                    return buildItemStack(type) {
                        hideEverything()

                        displayName = "&eColor"

                        if (enabled) {
                            addEnchant(DAMAGE_ALL, 1, true)
                        }

                        if (type != FIREWORK_STAR) {
                            lore = listOf("",
                                          "&f${color?.properName() ?: "Unknown"}")
                        }
                    }
                }

            }

            data class Chant(val chant: MutableMap<Enchantment, Int> = mutableMapOf())
                : DataQuery() {

                override fun createLore(): List<String> {
                    return listOf("&6Enchantments",
                                  *enchantmentLines().drop(1).map { "  $it" }.toTypedArray())
                }

                override fun createIcon(): ItemStack {
                    return buildItemStack(ENCHANTING_TABLE) {
                        hideEverything()

                        displayName = "&eEnchantments"

                        if (enabled) {
                            addEnchant(DAMAGE_ALL, 1, true)

                            lore = listOf(*enchantmentLines().toTypedArray())
                        }
                    }
                }


                private fun enchantmentLines(): List<String> {
                    val lines = mutableListOf("")

                    if (chant.isEmpty()) return lines

                    chant.forEach { chant, level ->
                        lines += "&7${chant.properName()}: &a$level"
                    }

                    return lines
                }


                fun createIconFor(enchantment: Enchantment): ItemStack {
                    return buildItemStack(ENCHANTED_BOOK) {
                        hideEverything()

                        displayName = "&e${enchantment.properName()}"

                        val level = chant[enchantment] ?: 0


                        if (enchantment.maxLevel == 1) {
                            lore = listOf("",
                                          if (level == 1) "  &aEnabled" else "  &cDisabled")
                        } else if (level > 0) {
                            lore = listOf("",
                                          "  &a$level")
                        }
                    }
                }

            }

            sealed class Kinds : DataQuery(), Named {

                abstract val predicate: (Material) -> Boolean


                override fun createLore(): List<String> {
                    return listOf("&7$name")
                }

                class IsBlock : Kinds() {

                    override val name = "Is Block"
                    override val predicate = Material::isBlock


                    override fun createIcon(): ItemStack {
                        return buildItemStack(STONE) {
                            hideEverything()

                            displayName = "&eIs Block"

                            if (enabled) {
                                addEnchant(DAMAGE_ALL, 1, true)
                            }
                        }
                    }

                }

                class IsItems : Kinds() {

                    override val name = "Is Item"
                    override val predicate = { type: Material ->
                        type.isBlock.not()
                    }


                    override fun createIcon(): ItemStack {
                        return buildItemStack(STICK) {
                            hideEverything()

                            displayName = "&eIs Item"

                            if (enabled) {
                                addEnchant(DAMAGE_ALL, 1, true)
                            }
                        }
                    }

                }

                class IsFoods : Kinds() {

                    override val name = "Is Edible"
                    override val predicate = Material::isEdible


                    override fun createIcon(): ItemStack {
                        return buildItemStack(COOKED_BEEF) {
                            hideEverything()

                            displayName = "&eIs Edible"

                            if (enabled) {
                                addEnchant(DAMAGE_ALL, 1, true)
                            }
                        }
                    }

                }

                class IsFuels : Kinds() {

                    override val name = "Is Fuel"
                    override val predicate = Material::isFuel


                    override fun createIcon(): ItemStack {
                        return buildItemStack(COAL) {
                            hideEverything()

                            displayName = "&eIs Fuel"

                            if (enabled) {
                                addEnchant(DAMAGE_ALL, 1, true)
                            }
                        }
                    }

                }

                class IsDiscs : Kinds() {

                    override val name = "Is Music Disc"
                    override val predicate = Material::isRecord


                    override fun createIcon(): ItemStack {
                        return buildItemStack(MUSIC_DISC_STAL) {
                            hideEverything()

                            displayName = "&eIs Music Disc"

                            if (enabled) {
                                addEnchant(DAMAGE_ALL, 1, true)
                            }
                        }
                    }

                }

            }

        }

    }

}