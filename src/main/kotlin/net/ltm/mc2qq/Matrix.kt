package net.ltm.mc2qq

import com.ejlchina.okhttps.HTTP
import com.ejlchina.okhttps.gson.GsonMsgConvertor
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import net.md_5.bungee.api.ProxyServer
import java.net.URLEncoder
import kotlin.concurrent.thread


class Matrix {
    private val mainLink: String = a
    private val accessToken: String = b
    private val userID: String = c
    private val roomID: String = d
    private val matrixIDToName = mutableMapOf<String, String>()
    private var roomHistoryToken: String = ""
    private var roomFilters: String = ""
    private val cli = HTTP.builder()
        .baseUrl(mainLink)
        .addMsgConvertor(GsonMsgConvertor())
        .bodyType("application/json")
        .build()

    fun init() {
        roomFilters = URLEncoder.encode(
            "{\"room\": {\"rooms\": [\"" + roomID + "\"], "
                    + "\"timeline\": {\"types\": [\"m.room.message\"], "
                    + "\"not_senders\": [\"" + userID + "\"]}}}", "UTF-8"
        )
        try {
            getLastMessages()
            sendMessages("服务器已连接")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLastMessages(): JsonArray {
        var result = JsonArray()
        try {
            val res = cli.sync(
                "/_matrix/client/r0/sync?filter=$roomFilters"
                        + (if (roomHistoryToken == "") "" else "&since=$roomHistoryToken")
                        + "&access_token=$accessToken"
            ).get()
            val json = JsonParser.parseString(res.body.toString()).asJsonObject
            res.body.close()
            roomHistoryToken = json.get("next_batch").asString
            if (json.has("rooms")) {
                val roomData = json.getAsJsonObject("rooms").getAsJsonObject("join")
                if (roomData.has(roomID)) {
                    result = roomData.getAsJsonObject(roomID).getAsJsonObject("timeline").getAsJsonArray("events")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun sendMessages(messsage: String): Boolean {
        val res = cli.sync("/_matrix/client/api/v1/rooms/$roomID/send/m.room.message")
            .addHeader("Authorization", "Bearer $accessToken")
            .addBodyPara("msgtype", "m.text")
            .addBodyPara("body", messsage)
            .post()
        res.body.close()
        if (res.status != 200) {
            throw Exception("[MC2QQMatrix] 发送消息失败")
        }
        return true
    }

    fun listenMessage() {
        val res = getLastMessages()
        if (!res.isEmpty) {
            for (i in res) {
                val user = getDisplayName(i.asJsonObject.get("sender").asString)
                val message = i.asJsonObject.get("content").asJsonObject.get("body").asString
                ProxyServer.getInstance().pluginManager.callEvent(MatrixMessageEvent(user, message))
            }
        }
    }

    private fun getDisplayName(matrixID: String): String {
        var res = matrixID
        if (matrixIDToName.containsKey(matrixID)) {
            res = matrixIDToName[matrixID]!!
        } else {
            try {
                val res2 = cli.sync("/_matrix/client/r0/profile/$matrixID/displayname")
                    .addHeader("Authorization", "Bearer $accessToken").get().body
                val tmp = JsonParser.parseString(res2.toString())
                res2.close()
                try {
                    if (tmp.isJsonObject) {
                        val name = tmp.asJsonObject.get("displayname").asString
                        matrixIDToName[matrixID] = name
                        res = name
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return res
    }
}

class MessageSendManager {
    private val sendMessageQueue = mutableListOf<String>()
    var threads: Thread? = null

    fun addMessage(message: String) {
        synchronized(this.sendMessageQueue) {
            sendMessageQueue.add(message)
            return
        }
    }

    fun sendMessage() {
        threads = thread(start = false, isDaemon = true) {
            while (true) {
                if (sendMessageQueue.isEmpty()) {
                    Thread.sleep(1000)
                    continue
                }
                val message = sendMessageQueue.removeAt(0)
                try {
                    matrix.sendMessages(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        threads!!.start()
    }
}