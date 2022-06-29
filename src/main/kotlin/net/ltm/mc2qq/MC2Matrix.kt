package net.ltm.mc2qq

import net.md_5.bungee.api.plugin.Plugin
import java.util.concurrent.TimeUnit

val matrix = Matrix()
val threadManager = MessageSendManager()

class MC2Matrix : Plugin() {
    override fun onEnable() {
        logger.info("MC2Matrix Now Loading")
        proxy.pluginManager.registerListener(this, MessagesFromMinecraft())
        proxy.pluginManager.registerListener(this, MessagesFromMatrix())
        proxy.pluginManager.registerListener(this, MessageToMatrix())
        try {
            matrix.init()
            threadManager.sendMessage()
            proxy.scheduler.schedule(this, {
                proxy.scheduler.runAsync(this) {
                    matrix.listenMessage()
                }
            }, 0, 3, TimeUnit.SECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDisable() {
        threadManager.threads?.interrupt()
        proxy.pluginManager.unregisterListeners(this)
        logger.info("MC2Matrix Now Unloading")
    }
}
