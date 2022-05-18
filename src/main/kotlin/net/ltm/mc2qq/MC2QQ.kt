package net.ltm.mc2qq

import net.md_5.bungee.api.plugin.Plugin

class MC2QQ : Plugin() {
    override fun onEnable() {
        // Plugin startup logic
        logger.info("MC2QQ Now Loading")
        proxy.scheduler.runAsync(this) { KeepAliveInWebSocket() }
    }

    override fun onDisable() {
        // Plugin shutdown logic
        logger.info("MC2QQ Now Unloading")
    }
}