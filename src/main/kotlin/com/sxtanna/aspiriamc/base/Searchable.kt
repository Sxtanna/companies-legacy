package com.sxtanna.aspiriamc.base

import com.sxtanna.aspiriamc.base.Searchable.Query.CostQuery.CostType.ABOVE
import com.sxtanna.aspiriamc.exts.buildItemStack
import com.sxtanna.aspiriamc.exts.properName
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.enchantments.Enchantment.DAMAGE_ALL
import org.bukkit.inventory.ItemFlag.*
import org.bukkit.inventory.ItemStack

interface Searchable {

    fun passes(query: Query): Boolean


    sealed class Query : Iconable {

        var enabled = false


        abstract fun createLore(): List<String>


        data class NameQuery(var name: String)
            : Query() {

            override fun createLore(): List<String> {
                return listOf("&6Named",
                              "  &7\"&f$name&7\"")
            }

            override fun createIcon(): ItemStack {
                return buildItemStack(PAPER) {
                    displayName = "&eName"

                    addItemFlags(HIDE_ENCHANTS,
                                 HIDE_ATTRIBUTES,
                                 HIDE_UNBREAKABLE,
                                 HIDE_DESTROYS,
                                 HIDE_PLACED_ON,
                                 HIDE_POTION_EFFECTS)

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

        data class CostQuery(var cost: Double, var type: CostType)
            : Query() {

            override fun createLore(): List<String> {
                return listOf("&6Cost ${if (type == ABOVE) "More Than" else "Less Than"}",
                              "  &a$$cost")
            }

            override fun createIcon(): ItemStack {
                return buildItemStack(SUNFLOWER) {
                    displayName = "&eCost"

                    addItemFlags(HIDE_ENCHANTS,
                                 HIDE_ATTRIBUTES,
                                 HIDE_UNBREAKABLE,
                                 HIDE_DESTROYS,
                                 HIDE_PLACED_ON,
                                 HIDE_POTION_EFFECTS)

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

        data class TypeQuery(var type: Material)
            : Query() {

            override fun createLore(): List<String> {
                return listOf("&6Type",
                              "  &7${type.properName()}")
            }

            override fun createIcon(): ItemStack {
                return buildItemStack(GRASS_BLOCK) {
                    displayName = "&eType"

                    addItemFlags(HIDE_ENCHANTS,
                                 HIDE_ATTRIBUTES,
                                 HIDE_UNBREAKABLE,
                                 HIDE_DESTROYS,
                                 HIDE_PLACED_ON,
                                 HIDE_POTION_EFFECTS)

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


            data class Count(var count: Int)
                : DataQuery() {

                override fun createLore(): List<String> {
                    return listOf("&6Count",
                                  "  &7$count")
                }

                override fun createIcon(): ItemStack {
                    return buildItemStack(MAP) {
                        displayName = "&eCount"

                        addItemFlags(HIDE_ENCHANTS,
                                     HIDE_ATTRIBUTES,
                                     HIDE_UNBREAKABLE,
                                     HIDE_DESTROYS,
                                     HIDE_PLACED_ON,
                                     HIDE_POTION_EFFECTS)

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

            data class Color(var color: DyeColor?)
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
                        displayName = "&eColor"

                        addItemFlags(HIDE_ENCHANTS,
                                     HIDE_ATTRIBUTES,
                                     HIDE_UNBREAKABLE,
                                     HIDE_DESTROYS,
                                     HIDE_PLACED_ON,
                                     HIDE_POTION_EFFECTS)

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
                            displayName = "&eIs Block"

                            addItemFlags(HIDE_ENCHANTS,
                                         HIDE_ATTRIBUTES,
                                         HIDE_UNBREAKABLE,
                                         HIDE_DESTROYS,
                                         HIDE_PLACED_ON,
                                         HIDE_POTION_EFFECTS)

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
                            displayName = "&eIs Item"

                            addItemFlags(HIDE_ENCHANTS,
                                         HIDE_ATTRIBUTES,
                                         HIDE_UNBREAKABLE,
                                         HIDE_DESTROYS,
                                         HIDE_PLACED_ON,
                                         HIDE_POTION_EFFECTS)

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
                            displayName = "&eIs Edible"

                            addItemFlags(HIDE_ENCHANTS,
                                         HIDE_ATTRIBUTES,
                                         HIDE_UNBREAKABLE,
                                         HIDE_DESTROYS,
                                         HIDE_PLACED_ON,
                                         HIDE_POTION_EFFECTS)

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
                            displayName = "&eIs Fuel"

                            addItemFlags(HIDE_ENCHANTS,
                                         HIDE_ATTRIBUTES,
                                         HIDE_UNBREAKABLE,
                                         HIDE_DESTROYS,
                                         HIDE_PLACED_ON,
                                         HIDE_POTION_EFFECTS)

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
                            displayName = "&eIs Music Disc"

                            addItemFlags(HIDE_ENCHANTS,
                                         HIDE_ATTRIBUTES,
                                         HIDE_UNBREAKABLE,
                                         HIDE_DESTROYS,
                                         HIDE_PLACED_ON,
                                         HIDE_POTION_EFFECTS)

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