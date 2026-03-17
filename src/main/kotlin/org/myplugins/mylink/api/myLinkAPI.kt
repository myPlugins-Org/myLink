package org.myplugins.mylink.api

import org.myplugins.mylink.mongo.LinkedAccount
import java.util.UUID

interface MyLinkAPI {
    suspend fun getDiscordId(uuid: UUID): String?
    suspend fun getLinkedAccount(uuid: UUID): LinkedAccount?
    suspend fun isLinked(uuid: UUID): Boolean
    fun sendToChannel(channelKey: String, message: String)
    fun isBotReady(): Boolean
}