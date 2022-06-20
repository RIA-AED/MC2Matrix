package net.ltm.mc2qq

import com.ejlchina.okhttps.HTTP
import com.ejlchina.okhttps.WebSocket
import com.ejlchina.okhttps.gson.GsonMsgConvertor
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer.getInstance
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.Event
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import xyz.olivermartin.multichat.bungee.events.PostGlobalChatEvent
import java.lang.Thread.sleep
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

val chathistory = arrayListOf<String>()
var count:Int=0
//Third-party Class
class AESCrypt {

    /**
     * aes加密
     */
    fun encrypt(input: String, password: String): String {
        //1. 创建cipher对象
        val cipher = Cipher.getInstance("AES")
        //2. 初始化cipher
        //自己指定的秘钥
        val keySpec = SecretKeySpec(password.toByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        //3. 加密和解密
        val encrypt = cipher.doFinal(input.toByteArray());
        return Base64.getEncoder().encodeToString(encrypt)
    }

    /**
     * aes解密
     */
    fun decrypt(input: String, password: String): String {
        //1. 创建cipher对象
        val cipher = Cipher.getInstance("AES")
        //2. 初始化cipher
        //自己指定的秘钥
        val keySpec = SecretKeySpec(password.toByteArray(), "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        //3. 加密和解密
        val decrypt = cipher.doFinal(Base64.getDecoder().decode(input))
        return String(decrypt)
    }

}//https://blog.csdn.net/hongxue8888/article/details/103846628

class ReceiveMessageFromQQ(val pretext: String, val message: String) : Event()

val cli = HTTP.builder().addMsgConvertor(GsonMsgConvertor()).build()
val link = cli.webSocket(server).bodyType("json")
    .setOnOpen { ws, data ->
        val key = AESCrypt().encrypt(name, token)
        ws.send(FrameOut(name, key, key))
        getInstance().logger.info("WS Channel Open")
        ws.send(FrameOut(name, "", AESCrypt().encrypt("来自服务器$name:已连接上MiraiBot", token)))
        getInstance().logger.info("发送完毕,等待接受")

    }.setOnMessage { ws: WebSocket, msg: WebSocket.Message ->
        if (msg.isText) {
            try {
                val rawText = AESCrypt().decrypt(msg.toBean(FrameIn::class.java).content, token)
                val type = msg.toBean(FrameIn::class.java).pretext
                getInstance().pluginManager.callEvent(ReceiveMessageFromQQ(type, rawText))
            } catch (e: Exception) {
                getInstance().logger.info("发生错误：$e")
            }
        }
    }

fun client() {
    val tmp = link.listen()
    links.add(tmp)
}

fun daemon() {
    val sec = 1000L
    while (true) {
        sleep(60 * sec)
        if (links.isNotEmpty()) {
            if (links.single().status() !in arrayOf(1, 2)) {
                links.single().close(1000, "自动守护进程清理")
                links.clear()
                val tmp = link.listen()
                links.add(tmp)
            }
        } else {
            links.clear()
            links.add(link.listen())
        }
    }
}

class QQMessage : Listener {
    @EventHandler
    fun onReceiveMessage(event: ReceiveMessageFromQQ) {
        getInstance().logger.info("接收到QQ群消息:" + event.message)
        if (event.pretext.contains("bind")) {
            val id = event.pretext.replace("bind", "")
            for (i in getInstance().players) {
                if (i.name == id) {
                    i.sendMessage(TextComponent("你的验证码是:${event.message}"))
                }
            }
            return
        }
        if (event.pretext.contains("chat")) {
            var pre = event.message.substring(0, event.message.indexOf(':'))
            var text = event.message.substring(event.message.indexOf(':') + 1)
            chathistory.add("群 <$pre>: $text\n")
            count++
            val base = TextComponent("群 ")
            base.color = ChatColor.GRAY
            val first = TextComponent("<")
            first.color = ChatColor.DARK_GRAY
            val names = TextComponent(pre)
            names.color = ChatColor.GRAY
            val second = TextComponent(">: ")
            second.color = ChatColor.DARK_GRAY
            val content = TextComponent(text)
            content.color = ChatColor.WHITE
            for (i in getInstance().players)
                i.sendMessage(base, first, names, second, content)
            if (links.isNotEmpty()) {
                links.single().send(
                    FrameOut(
                        name,
                        "",
                        AESCrypt().encrypt("发送成功", token)
                    )
                )
            }
            return
        }
        if (event.pretext.contains("history")) {
            if (links.isNotEmpty()) {
                var context = "以下是历史记录:\n"
                for (i in chathistory) {
                    context += i.toString()
                }
                links.single().send(
                    FrameOut(
                        name,
                        "",
                        AESCrypt().encrypt(context, token)
                    )
                )
            }
        }
    }
}

class IngameChat : Listener {
    @EventHandler
    fun onReceiveMessage(event: PostGlobalChatEvent) {
        if(count >=15){
            count=0
            chathistory.clear()
        }
        chathistory.add("<${event.rawSenderNickname}>: ${event.rawMessage}\n")
        count++
        if (event.rawMessage.contains("#喊话")) {
            if (links.isNotEmpty()) {
                links.single().send(
                    FrameOut(
                        name,
                        "",
                        AESCrypt().encrypt(
                            "$name <${event.rawSenderNickname}>: " + event.rawMessage.replace("#喊话", ""),
                            token
                        )
                    )
                )
            }
        }
    }
}

class ConnectCommand : Command("connectws") {
    override fun execute(sender: CommandSender?, args: Array<out String>?) {
        if (links.isNotEmpty()) {
            if (links.single().status() !in arrayOf(1, 2)) {
                links.single().close(1000, "自动守护进程清理")
                links.clear()
                links.add(link.listen())
            } else {
                sender?.sendMessage(TextComponent("WebSocket已链接,无需再次链接!"))
                return
            }
        } else {
            links.clear()
            links.add(link.listen())
        }
        sender?.sendMessage(TextComponent("已尝试链接!"))
    }
}

class ForceReconnect : Command("reconnectws") {
    override fun execute(sender: CommandSender?, args: Array<out String>?) {
        if (links.isNotEmpty()) {
            links.single().close(1000, "强制重连")
        }
        links.add(link.listen())
        sender?.sendMessage(TextComponent("已强制重连!"))
    }
}