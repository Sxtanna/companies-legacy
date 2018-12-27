package com.sxtanna.aspiriamc.menu.chat

import com.sxtanna.aspiriamc.exts.TimChatMenu
import me.tom.sparse.spigot.chat.menu.element.Element
import org.bukkit.entity.Player

abstract class ChatMenu {

    private var built = false
    protected val menu = TimChatMenu().pauseChat()


    internal open fun build() {

    }


    open fun open(player: Player) {
        if (built.not()) {
            build()
            built = true
        }

        menu.openFor(player)
    }

    open fun kill(player: Player) {
        menu.close(player)
    }


    operator fun <T : Element> T.unaryPlus(): T {
        menu.add(this)
        return this
    }

}