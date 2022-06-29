package net.ltm.mc2qq

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import xyz.olivermartin.multichat.bungee.events.PostGlobalChatEvent

class MessagesFromMinecraft : Listener {
    @EventHandler
    fun onReceiveMessage(event: PostGlobalChatEvent) {
        val message = "[${event.senderServer}]<${event.rawSenderNickname}>:${event.message}"
        ProxyServer.getInstance().pluginManager.callEvent(MinecraftMessageEvent(message))
    }
}

class MessageToMatrix : Listener {
    @EventHandler
    fun onGetMessage(event: MinecraftMessageEvent) {
        val message = event.message
        threadManager.addMessage(message)
    }
}

class MessagesFromMatrix : Listener {
    @EventHandler
    fun onProcessMessage(event: MatrixMessageEvent) {
        when (event.message) {
            "!tab" -> sendTab(event)
            else -> sendMessages(event)
        }
    }

    private fun sendTab(event: MatrixMessageEvent) {
        var message = ""
        ProxyServer.getInstance().serversCopy.forEach {
            message += ("[${it.key}]:")
            ProxyServer.getInstance().getServerInfo(it.key).players.forEach { player ->
                message += ("${player.name},")
            }
            message += "该子服总玩家数${ProxyServer.getInstance().getServerInfo(it.key).players.count()}\n"
        }
        message += "该主服总玩家数${ProxyServer.getInstance().players.count()}"
        ProxyServer.getInstance().pluginManager.callEvent(MinecraftMessageEvent(message))
        ProxyServer.getInstance().logger.info("用户:${event.user}发送了Tab请求")
    }

    private fun sendMessages(event: MatrixMessageEvent) {
        val base = TextComponent("[Matrix]")
        base.color = ChatColor.GRAY
        val first = TextComponent("<")
        first.color = ChatColor.DARK_GRAY
        val name = TextComponent(event.user)
        name.color = ChatColor.GRAY
        val second = TextComponent(">: ")
        second.color = ChatColor.DARK_GRAY
        val content = TextComponent(event.message)
        content.color = ChatColor.WHITE
        ProxyServer.getInstance().players.forEach { it.sendMessage(base, first, name, second, content) }
        ProxyServer.getInstance().logger.info("用户:${event.user}发送了消息:${event.message}")
    }
}
