package com.github.andreyasadchy.xtra.util.chat

import android.util.Log
import com.github.andreyasadchy.xtra.util.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.net.ssl.X509TrustManager

class KickPusherChatWebSocket(
    private val chatroomId: String,
    private val channelId: String?,
    private val trustManager: Lazy<X509TrustManager>,
    private val listener: Listener,
) {
    private val tag = "KickRealtimeChat"
    private val channelNames = buildChannelNames(chatroomId, channelId)
    private var webSocket: WebSocket? = null
    private var hasEmittedConnect = false

    companion object {
        private const val APP_KEY = "32cbd69e4b950bf97679"
        private const val HOST = "ws-us2.pusher.com"

        fun buildChannelNames(chatroomId: String, channelId: String?): List<String> = buildList {
            add("chatrooms.$chatroomId.v2")
            channelId?.takeIf { it.isNotBlank() }?.let {
                add("channel.$it")
                add("predictions-channel-$it")
            }
        }

        fun buildSocketUrl(): String {
            return "wss://$HOST/app/$APP_KEY?protocol=7&client=android&version=1.0&flash=false"
        }
    }

    interface Listener {
        suspend fun onConnect() {}
        suspend fun onChatEvent(eventName: String, channelName: String?, messageJson: String) {}
        suspend fun onDisconnect(message: String, fullMsg: String?) {}
    }

    fun connect(coroutineScope: CoroutineScope): Job {
        webSocket = WebSocket(buildSocketUrl(), trustManager, WebSocketListener())
        return coroutineScope.launch(Dispatchers.IO) {
            webSocket?.start()
        }
    }

    suspend fun disconnect(job: Job?) = withContext(Dispatchers.IO) {
        job?.cancel()
        webSocket?.disconnect()
    }

    private suspend fun subscribe() = withContext(Dispatchers.IO) {
        channelNames.forEach { channelName ->
            val payload = JSONObject().apply {
                put("event", "pusher:subscribe")
                put(
                    "data",
                    JSONObject().apply {
                        put("auth", "")
                        put("channel", channelName)
                    }
                )
            }
            webSocket?.write(payload.toString())
        }
    }

    private fun parseJsonOrNull(raw: Any?): JSONObject? {
        return when (raw) {
            is JSONObject -> raw
            is String -> runCatching { JSONObject(raw) }.getOrNull()
            else -> null
        }
    }

    private inner class WebSocketListener : WebSocket.Listener {
        override suspend fun onConnect(webSocket: WebSocket) {
            // Wait for pusher:connection_established before subscribing.
        }

        override suspend fun onMessage(webSocket: WebSocket, message: String) {
            runCatching {
                val root = JSONObject(message)
                val event = root.optString("event")
                val channel = root.optString("channel")
                val rawData = root.opt("data")
                val payload = when (rawData) {
                    is JSONObject -> rawData.toString()
                    is String -> rawData
                    else -> null
                }
                when (event) {
                    "pusher:connection_established" -> subscribe()
                    "pusher_internal:subscription_succeeded" -> {
                        if (!hasEmittedConnect) {
                            hasEmittedConnect = true
                            listener.onConnect()
                        }
                    }
                    "pusher:ping" -> webSocket.write("""{"event":"pusher:pong","data":{}}""")
                    "pusher:error" -> Log.w(tag, "pusher_error channel=$channel")
                    else -> {
                        if (!event.startsWith("pusher:") && !event.startsWith("pusher_internal:")) {
                            payload?.takeIf { it.isNotBlank() }?.let {
                                listener.onChatEvent(event, channel.takeIf { value -> value.isNotBlank() }, it)
                            }
                        }
                    }
                }
            }.onFailure {
                Log.w(tag, "parse_error: ${it.message}")
            }
        }

        override suspend fun onDisconnect(webSocket: WebSocket, message: String, fullMsg: String?) {
            hasEmittedConnect = false
            listener.onDisconnect(message, fullMsg)
        }
    }
}
