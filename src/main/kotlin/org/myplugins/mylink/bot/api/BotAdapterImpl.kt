package org.myplugins.mylink.api

import org.myplugins.mylink.MyLinkPlugin

class BotAdapterImpl(private val plugin: MyLinkPlugin) : BotAdapter {

    override fun sendToChannel(channelKey: String, message: String) {
        val channelId = when (channelKey) {
            "chat-sync" -> plugin.cfg.chatBridgeChannel
            "console" -> plugin.cfg.consoleChannel
            else -> return
        }
        plugin.botManager.sendMessage(channelId, message)
    }

    override fun isReady(): Boolean = plugin.botManager.isReady()
}