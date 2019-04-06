package com.sxtanna.aspiriamc.exts

import com.sxtanna.aspiriamc.Companies
import com.sxtanna.aspiriamc.base.Result
import com.sxtanna.aspiriamc.menu.item.UpdatingItemStack
import me.tom.sparse.spigot.chat.menu.ChatMenu
import me.tom.sparse.spigot.chat.menu.ChatMenuAPI
import org.bukkit.ChatColor
import org.bukkit.DyeColor
import org.bukkit.DyeColor.BROWN
import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.command.Command
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.*
import java.util.Base64

typealias BukkitCommand = Command

typealias TimChatMenu = ChatMenu
typealias TimChatMenuAPI = ChatMenuAPI

// general extensions

fun File.ensureUsable(): File {
    if (exists()) return this

    parentFile.mkdirs()
    createNewFile()

    return this
}

fun Enum<*>.properName(): String {
    return name.split("_").joinToString(" ") { it.toLowerCase().capitalize() }
}

fun String.ownership(): String {
    return "$this${if (this.endsWith('s', true)) "'" else "'s"}"
}

fun Double.toReadableString(): String {
    return CURRENCIES_FORMAT.format(this)
}

fun Double.formatToTwoPlaces(): Double {
    val text = toReadableString()
    val left = text.substringAfter('.', "")

    return if (left.length < 2 || left.isBlank()) {
        text
    } else {
        text.dropLast(left.length - 2)
    }.toDouble()
}

fun mapToWithin(value: Int, originMin: Int, originMax: Int, targetMin: Int, targetMax: Int): Int {
    return (value - originMin) * (targetMax - targetMin) / (originMax - originMin) + targetMin
}


fun Throwable.consume(): String {
    return StringWriter().apply { printStackTrace(PrintWriter(this)) }.toString()
}


// bukkit functions

fun color(text: String): String {
    return ChatColor.translateAlternateColorCodes('&', text)
}

fun strip(text: String): String {
    return ChatColor.stripColor(color(text)) ?: text
}


fun itemStackName(item: ItemStack): String {
    val meta = (if (item.hasItemMeta()) item.itemMeta else null) ?: return item.type.properName()

    return if (meta is BookMeta && meta.hasTitle()) {
        meta.title ?: ""
    } else if (meta.hasDisplayName()) {
        meta.displayName
    } else {
        item.type.properName()
    }
}

fun itemStackIsColor(item: ItemStack, color: DyeColor): Boolean {
    return when (item.type) {
        LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS -> {

            if (item.hasItemMeta().not()) {
                color == BROWN // quick return
            } else {

                val pass = color.color
                val meta = (item.itemMeta as LeatherArmorMeta).color

                val maxDist = 50

                val rRange = (pass.red   - maxDist).coerceAtLeast(0)..(pass.red   + maxDist).coerceAtMost(255)
                val gRange = (pass.green - maxDist).coerceAtLeast(0)..(pass.green + maxDist).coerceAtMost(255)
                val bRange = (pass.blue  - maxDist).coerceAtLeast(0)..(pass.blue  + maxDist).coerceAtMost(255)

                val rPass = meta.red   in rRange
                val gPass = meta.green in gRange
                val bPass = meta.blue  in bRange

                return rPass && gPass && bPass
            }
        }
        else                                                                -> {
            COLOR_MATERIAL_ASSOCIATIONS[color]?.contains(item.type) ?: false
        }
    }
}


fun itemStackToBase64(item: ItemStack): Result<String> = Result.of {
    val oStream = ByteArrayOutputStream()
    val bObject = BukkitObjectOutputStream(oStream)

    bObject.writeObject(item)
    bObject.close()

    Base64.getEncoder().encodeToString(oStream.toByteArray())
}

fun base64ToItemStack(text: String): Result<ItemStack> = Result.of {
    val iStream = ByteArrayInputStream(Base64.getDecoder().decode(text))
    val bObject = BukkitObjectInputStream(iStream)

    val item = bObject.readObject() as? ItemStack
    bObject.close()

    item
}

fun updateItemMeta(item: ItemStack, block: ItemMeta.() -> Unit): ItemStack {
    val meta = item.itemMeta?.apply(block) ?: return item

    if (meta.hasDisplayName()) {
        meta.setDisplayName(color(meta.displayName))
    }

    if (meta.hasLore()) {
        meta.lore = meta.lore?.map(::color)
    }

    item.itemMeta = meta

    return item
}

fun buildItemStack(type: Material, amount: Int = 1, block: ItemMeta.() -> Unit = { }): ItemStack {
    return buildItemStack(ItemStack(type, amount), amount, block)
}

fun buildItemStack(item: ItemStack, amount: Int = item.amount, block: ItemMeta.() -> Unit = { }): ItemStack {
    return item.apply {
        this.amount = amount

        updateItemMeta(item, block)
    }
}

fun buildUpdatingItemStack(plugin: Companies, type: Material, amount: Int = 1, block: ItemMeta.() -> Unit = { }, update: UpdatingItemStack.() -> Unit): UpdatingItemStack {
    val item = UpdatingItemStack(plugin, type, amount, update)

    buildItemStack(item, amount, block)

    return item
}


fun buildTippedArrow(amount: Int = 1, block: PotionMeta.() -> Unit = {}): ItemStack {
    return buildItemStack(TIPPED_ARROW, amount) {
        (this as PotionMeta).block()
    }
}


// bukkit extensions

fun Player.inventoryCanHold(item: ItemStack): Boolean {

    inventory.storageContents.forEach { held: ItemStack? ->
        if (held == null) return true
        if (held.isSimilar(item).not()) return@forEach

        if (held.amount + item.amount <= held.maxStackSize) return true
    }

    return false
}

fun ItemMeta.hideEverything() {
    addItemFlags(HIDE_ENCHANTS,
                 HIDE_ATTRIBUTES,
                 HIDE_UNBREAKABLE,
                 HIDE_DESTROYS,
                 HIDE_PLACED_ON,
                 HIDE_POTION_EFFECTS)
}

fun Enchantment.properName(): String {
    return when(this) {
        Enchantment.BINDING_CURSE -> {
            "Curse Of Binding"
        }
        Enchantment.VANISHING_CURSE -> {
            "Curse Of Vanishing"
        }
        Enchantment.SWEEPING_EDGE -> {
            "Sweeping Edge"
        }
        else -> {
            key.key.split('_').joinToString(" ") { it.toLowerCase().capitalize() }
        }
    }
}