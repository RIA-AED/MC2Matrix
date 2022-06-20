package net.ltm.mc2qq

import com.ejlchina.okhttps.WebSocket
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.File

//DataClass
data class FrameIn(val pretext: String, val content: String)

data class FrameOut(val name: String, val pretext: String, val content: String)

val links = mutableListOf<WebSocket>()
var name: String = ""
var server: String = ""
var token: String = ""

class MC2QQ : Plugin() {
    override fun onEnable() {
        logger.info("MC2QQ Now Loading")
        proxy.pluginManager.registerListener(this, IngameChat())
        proxy.pluginManager.registerListener(this, QQMessage())
        proxy.pluginManager.registerCommand(this, ConnectCommand())
        proxy.pluginManager.registerCommand(this, ForceReconnect())
        try {
            val configuration: Configuration = ConfigurationProvider.getProvider(YamlConfiguration::class.java).load(
                File(
                    dataFolder, "config.yml"
                )
            )
            name = configuration["name"].toString()
            server = configuration["server"].toString()
            token = configuration["token"].toString()
            proxy.scheduler.runAsync(this) {
                client()
            }
        } catch (e: java.lang.Exception) {
            logger.info("Exception: ${e.message}\n${e.cause}\n${e.stackTrace}")
        }
        proxy.scheduler.runAsync(this) {
            daemon()
        }//Dameon
    }

    override fun onDisable() {
        proxy.pluginManager.unregisterListeners(this)
        proxy.pluginManager.unregisterCommands(this)
        logger.info("MC2QQ Now Unloading")
    }
}
