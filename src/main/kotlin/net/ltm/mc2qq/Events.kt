package net.ltm.mc2qq

import net.md_5.bungee.api.plugin.Event

class MatrixMessageEvent(val user: String, val message: String) : Event()

class MinecraftMessageEvent(val message: String) : Event()