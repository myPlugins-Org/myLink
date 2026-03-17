package org.myplugins.mylink.bot

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import org.myplugins.mylink.MyLinkPlugin
import org.myplugins.mylink.listener.DiscordChatListener

class BotManager(private val plugin: MyLinkPlugin) {

    var jda: JDA? = null
        private set

    val guild: Guild?
        get() = jda?.getGuildById(plugin.cfg.guildId)

    fun start() {
        jda = JDABuilder.createDefault(plugin.cfg.botToken)
            .enableIntents(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MODERATION,
            )
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .setChunkingFilter(net.dv8tion.jda.api.utils.ChunkingFilter.ALL)
            .setActivity(Activity.watching("myplugins.org"))
            .addEventListeners(
                DiscordListener(plugin),
                DiscordChatListener(plugin),
            )
            .build()
            .awaitReady()

        registerSlashCommands()
        plugin.setBotReady(true)
        plugin.logger.info("Discord bot connected as ${jda!!.selfUser.asTag}")
    }

    fun setPresence(activity: Activity) {
        jda?.presence?.activity = activity
    }

    private fun registerSlashCommands() {
        guild?.updateCommands()
            ?.addCommands(
                Commands.slash("link", "Link your Minecraft account to Discord")
                    .addOption(OptionType.STRING, "code", "The code generated in-game with /link", true),
                Commands.slash("unlink", "Unlink your Minecraft account from Discord"),
                Commands.slash("profile", "View your linked Minecraft account"),
            )
            ?.queue {
                plugin.logger.info("Slash commands registered successfully.")
            }
    }

    fun shutdown() {
        jda?.shutdown()
        plugin.setBotReady(false)
    }

    fun isReady(): Boolean = jda?.status == JDA.Status.CONNECTED

    fun sendMessage(channelId: String, message: String) {
        if (channelId.isBlank()) return
        jda?.getTextChannelById(channelId)?.sendMessage(message)?.queue()
    }

    fun sendWebhookMessage(channelId: String, username: String, avatarUrl: String, message: String) {
        if (channelId.isBlank()) return
        val channel = jda?.getTextChannelById(channelId) ?: return
        channel.retrieveWebhooks().queue { webhooks ->
            val webhook = webhooks.firstOrNull { it.name == "myLink" }
            if (webhook != null) {
                net.dv8tion.jda.api.entities.WebhookClient.createClient(jda!!, webhook.url)
                    .sendMessage(message)
                    .setUsername(username)
                    .setAvatarUrl(avatarUrl)
                    .queue()
            } else {
                channel.createWebhook("myLink").queue { created ->
                    net.dv8tion.jda.api.entities.WebhookClient.createClient(jda!!, created.url)
                        .sendMessage(message)
                        .setUsername(username)
                        .setAvatarUrl(avatarUrl)
                        .queue()
                }
            }
        }
    }
}