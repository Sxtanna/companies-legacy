package com.sxtanna.aspiriamc.exts

import com.sxtanna.aspiriamc.base.Result
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

typealias BukkitCommand = Command

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

fun Double.formatToTwoPlaces(): Double {
    val text = toBigDecimal().toPlainString()
    val left = text.substringAfter('.', "")

    return if (left.length < 2 || left.isBlank()) {
        text
    } else {
        text.dropLast(left.length - 2)
    }.toDouble()
}


// bukkit functions

fun color(text: String): String {
    return ChatColor.translateAlternateColorCodes('&', text)
}

fun strip(text: String): String {
    return ChatColor.stripColor(text)
}


fun itemStackName(item: ItemStack): String {
    val meta = (if (item.hasItemMeta()) item.itemMeta else null) ?: return item.type.properName()

    return if (meta is BookMeta && meta.hasTitle()) {
        meta.title
    }
    else if (meta.hasDisplayName()) {
        meta.displayName
    }
    else {
        item.type.properName()
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


fun buildItemStack(item: ItemStack, amount: Int = item.amount, block: ItemMeta.() -> Unit = { }): ItemStack {
    return item.apply {
        this.amount = amount
        val meta = this.itemMeta.apply(block)

        if (meta.hasDisplayName()) {
            meta.displayName = color(meta.displayName)
        }

        if (meta.hasLore()) {
            meta.lore = meta.lore.map(::color)
        }

        this.itemMeta = meta
    }
}

fun buildItemStack(type: Material, amount: Int = 1, block: ItemMeta.() -> Unit = { }): ItemStack {
    return buildItemStack(ItemStack(type, amount), amount, block)
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