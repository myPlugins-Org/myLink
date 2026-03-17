package org.myplugins.mylink.api

import org.myplugins.mylink.link.LinkService
import org.myplugins.mylink.mongo.LinkedAccount
import java.util.UUID

class MyLinkAPIImpl(private val service: LinkService) : MyLinkAPI {

    var botAdapter: BotAdapter? = null

    override suspend fun getDiscordId(uuid: UUID): String? =
        service.getLinkedAccount(uuid)?.discordId

    override suspend fun getLinkedAccount(uuid: UUID): LinkedAccount? =
        service.getLinkedAccount(uuid)

    override suspend fun isLinked(uuid: UUID): Boolean =
        service.isLinked(uuid)

    override fun sendToChannel(channelKey: String, message: String) {
        botAdapter?.sendToChannel(channelKey, message)
    }

    override fun isBotReady(): Boolean = botAdapter?.isReady() == true
}

interface BotAdapter {
    fun sendToChannel(channelKey: String, message: String)
    fun isReady(): Boolean
}