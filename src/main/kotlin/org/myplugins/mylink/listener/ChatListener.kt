package org.myplugins.mylink.listener

import io.papermc.paper.event.player.AsyncChatEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.myplugins.mylink.MyLinkPlugin

class ChatListener(private val plugin: MyLinkPlugin) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        if (!plugin.cfg.chatBridgeEnabled) return
        val channelId = plugin.cfg.chatBridgeChannel
        if (channelId.isBlank()) return

        val player = event.player
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())
        // MCHeads — uses UUID, never breaks on name changes
        val avatarUrl = "https://mc-heads.net/avatar/${player.uniqueId}/128"

        if (plugin.cfg.chatBridgeWebhooks) {
            plugin.botManager.sendWebhookMessage(
                channelId = channelId,
                username = player.name,
                avatarUrl = avatarUrl,
                message = message
            )
        } else {
            val formatted = plugin.cfg.chatMcToDcFormat
                .replace("%player%", player.name)
                .replace("%message%", message)
            plugin.botManager.sendMessage(channelId, formatted)
        }
    }
}

class DiscordChatListener(private val plugin: MyLinkPlugin) : ListenerAdapter() {

    private val mm = MiniMessage.miniMessage()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        if (!plugin.cfg.chatBridgeEnabled) return
        if (event.channel.id != plugin.cfg.chatBridgeChannel) return

        val username = event.author.name
        val message = event.message.contentDisplay

        val formatted = plugin.cfg.chatDcToMcFormat
            .replace("%user%", username)
            .replace("%message%", message)

        val component = mm.deserialize(formatted)

        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.broadcast(component)
        })
    }
}