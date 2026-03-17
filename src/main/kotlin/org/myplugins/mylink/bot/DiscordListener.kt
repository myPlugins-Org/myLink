package org.myplugins.mylink.bot

import kotlinx.coroutines.launch
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.bukkit.Bukkit
import org.myplugins.mylink.MyLinkPlugin
import org.myplugins.mylink.link.ConsumeCodeResult
import org.myplugins.mylink.link.UnlinkResult
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.Date

class DiscordListener(private val plugin: MyLinkPlugin) : ListenerAdapter() {

    private val purple = Color(171, 76, 219)
    private val green = Color(87, 242, 135)
    private val red = Color(237, 66, 69)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "link" -> handleLink(event)
            "unlink" -> handleUnlink(event)
            "profile" -> handleProfile(event)
        }
    }

    private fun handleLink(event: SlashCommandInteractionEvent) {
        if (plugin.cfg.verifyChannel.isNotBlank() && event.channel.id != plugin.cfg.verifyChannel) {
            event.reply("❌ Please use this command in <#${plugin.cfg.verifyChannel}>.")
                .setEphemeral(true)
                .queue()
            return
        }

        val code = event.getOption("code")?.asString?.trim()?.uppercase() ?: run {
            event.reply("❌ Please provide a code.").setEphemeral(true).queue()
            return
        }

        val discordId = event.user.id
        val discordTag = event.user.name
        event.deferReply(true).queue()

        plugin.pluginScope.scope.launch {
            when (val result = plugin.linkService.consumeCode(code, discordId, discordTag)) {
                is ConsumeCodeResult.Success -> {
                    val nick = plugin.linkService.getLinkedAccount(result.uuid)?.lastNick ?: discordTag
                    val avatarUrl = "https://mc-heads.net/avatar/${result.uuid}/128"

                    val embed = EmbedBuilder()
                        .setTitle("✅ Account Linked!")
                        .setDescription("Welcome, **$discordTag**!\nYour Discord account is now linked to **$nick** on Minecraft.")
                        .setThumbnail(avatarUrl)
                        .setColor(green)
                        .setFooter("myPlugins • myplugins.org", "https://mc-heads.net/avatar/${result.uuid}/32")
                        .setTimestamp(java.time.Instant.now())
                        .build()

                    event.hook.sendMessageEmbeds(embed).queue()

                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        Bukkit.getPlayer(result.uuid)?.sendMessage(
                            plugin.cfg.msg("link-success", "player" to discordTag)
                        )
                    })

                    plugin.groupSyncService.syncToDiscord(result.uuid)
                }
                is ConsumeCodeResult.InvalidCode -> {
                    val embed = EmbedBuilder()
                        .setTitle("❌ Invalid Code")
                        .setDescription("The code you provided is invalid or has expired.\nUse `/link` in-game to generate a new one.")
                        .setColor(red)
                        .setFooter("myPlugins • myplugins.org")
                        .build()
                    event.hook.sendMessageEmbeds(embed).queue()
                }
                is ConsumeCodeResult.DatabaseError -> {
                    val embed = EmbedBuilder()
                        .setTitle("❌ Database Error")
                        .setDescription("A database error occurred. Please try again later.")
                        .setColor(red)
                        .setFooter("myPlugins • myplugins.org")
                        .build()
                    event.hook.sendMessageEmbeds(embed).queue()
                }
            }
        }
    }

    private fun handleUnlink(event: SlashCommandInteractionEvent) {
        val discordId = event.user.id
        event.deferReply(true).queue()

        plugin.pluginScope.scope.launch {
            val account = plugin.linkService.findByDiscordId(discordId) ?: run {
                val embed = EmbedBuilder()
                    .setTitle("❌ Not Linked")
                    .setDescription("You don't have a linked Minecraft account.")
                    .setColor(red)
                    .setFooter("myPlugins • myplugins.org")
                    .build()
                event.hook.sendMessageEmbeds(embed).queue()
                return@launch
            }
            val uuid = java.util.UUID.fromString(account.uuid)
            when (plugin.linkService.unlink(uuid)) {
                is UnlinkResult.Success -> {
                    val embed = EmbedBuilder()
                        .setTitle("✅ Account Unlinked")
                        .setDescription("Your Minecraft account **${account.lastNick}** has been unlinked.")
                        .setThumbnail("https://mc-heads.net/avatar/${account.uuid}/128")
                        .setColor(purple)
                        .setFooter("myPlugins • myplugins.org")
                        .setTimestamp(java.time.Instant.now())
                        .build()
                    event.hook.sendMessageEmbeds(embed).queue()
                }
                is UnlinkResult.NotLinked -> {
                    val embed = EmbedBuilder()
                        .setTitle("❌ Not Linked")
                        .setDescription("You don't have a linked Minecraft account.")
                        .setColor(red)
                        .setFooter("myPlugins • myplugins.org")
                        .build()
                    event.hook.sendMessageEmbeds(embed).queue()
                }
                is UnlinkResult.DatabaseError -> {
                    val embed = EmbedBuilder()
                        .setTitle("❌ Database Error")
                        .setDescription("A database error occurred. Please try again later.")
                        .setColor(red)
                        .setFooter("myPlugins • myplugins.org")
                        .build()
                    event.hook.sendMessageEmbeds(embed).queue()
                }
            }
        }
    }

    private fun handleProfile(event: SlashCommandInteractionEvent) {
        val discordId = event.user.id
        event.deferReply(true).queue()

        plugin.pluginScope.scope.launch {
            val account = plugin.linkService.findByDiscordId(discordId) ?: run {
                val embed = EmbedBuilder()
                    .setTitle("❌ Not Linked")
                    .setDescription("You don't have a linked Minecraft account.\nUse `/link` to get started.")
                    .setColor(red)
                    .setFooter("myPlugins • myplugins.org")
                    .build()
                event.hook.sendMessageEmbeds(embed).queue()
                return@launch
            }

            val linkedAt = SimpleDateFormat("dd/MM/yyyy 'at' HH:mm").format(Date(account.linkedAt))
            val uuid = account.uuid.let {
                if (it.contains("-")) it
                else "${it.substring(0,8)}-${it.substring(8,12)}-${it.substring(12,16)}-${it.substring(16,20)}-${it.substring(20)}"
            }

            val avatarUrl = "https://mc-heads.net/avatar/${account.uuid}/128"
            val playerUrl = "https://mc-heads.net/player/${account.uuid}/128"

            val embed = EmbedBuilder()
                .setTitle("👤 ${account.lastNick}'s Profile")
                .setThumbnail(avatarUrl)
                .setImage(playerUrl)
                .addField("Minecraft", "`${account.lastNick}`", true)
                .addField("UUID", "`${account.uuid}`", false)
                .addField("Linked since", linkedAt, true)
                .setColor(purple)
                .setFooter("myPlugins • myplugins.org", avatarUrl)
                .setTimestamp(java.time.Instant.now())
                .build()

            event.hook.sendMessageEmbeds(embed).queue()
        }
    }

    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        if (event.guild.id != plugin.cfg.guildId) return
        val addedRoles = event.roles.map { it.id }
        plugin.pluginScope.scope.launch {
            val account = plugin.linkService.findByDiscordId(event.member.id) ?: return@launch
            addedRoles.forEach { roleId ->
                plugin.groupSyncService.onDiscordRoleAdd(account.uuid, roleId)
            }
        }
    }

    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        if (event.guild.id != plugin.cfg.guildId) return
        val removedRoles = event.roles.map { it.id }
        plugin.pluginScope.scope.launch {
            val account = plugin.linkService.findByDiscordId(event.member.id) ?: return@launch
            removedRoles.forEach { roleId ->
                plugin.groupSyncService.onDiscordRoleRemove(account.uuid, roleId)
            }
        }
    }
}