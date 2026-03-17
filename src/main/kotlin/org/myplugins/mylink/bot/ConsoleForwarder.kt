package org.myplugins.mylink.bot

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.layout.PatternLayout
import org.bukkit.Bukkit
import org.myplugins.mylink.MyLinkPlugin
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

class ConsoleForwarder(private val plugin: MyLinkPlugin) {

    private var appender: DiscordAppender? = null
    val recentLines: ArrayDeque<String> = ArrayDeque(20)

    fun start() {
        if (!plugin.cfg.consoleEnabled) return
        val channelId = plugin.cfg.consoleChannel
        if (channelId.isBlank()) return

        appender = DiscordAppender(plugin, channelId, recentLines)
        appender!!.start()

        val rootLogger = LogManager.getRootLogger()
                as org.apache.logging.log4j.core.Logger
        rootLogger.addAppender(appender!!)

        plugin.logger.info("Console forwarding enabled.")
        plugin.botManager.jda?.addEventListener(ConsoleCommandListener(plugin, this))
    }

    fun stop() {
        val rootLogger = LogManager.getRootLogger()
                as? org.apache.logging.log4j.core.Logger
        appender?.let {
            rootLogger?.removeAppender(it)
            it.stop()
        }
    }
}

class DiscordAppender(
    private val plugin: MyLinkPlugin,
    private val channelId: String,
    private val recentLines: ArrayDeque<String>,
) : AbstractAppender(
    "DiscordConsoleAppender",
    null,
    PatternLayout.createDefaultLayout(),
    true,
    arrayOf()
) {
    override fun append(event: LogEvent) {
        val msg = event.message.formattedMessage.take(200)
        synchronized(recentLines) {
            if (recentLines.size >= 20) recentLines.removeFirst()
            recentLines.addLast(msg)
        }
    }
}

class ConsoleCommandListener(
    private val plugin: MyLinkPlugin,
    private val forwarder: ConsoleForwarder,
) : ListenerAdapter() {

    private val pendingConfirm = ConcurrentHashMap<String, String>()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        if (event.channel.id != plugin.cfg.consoleChannel) return
        if (event.author.id !in plugin.cfg.consoleWhitelist) return

        val content = event.message.contentRaw.trim()
        if (content.isBlank()) return

        val channel = event.channel as? TextChannel ?: return
        val userId = event.author.id

        if (content.equals("confirm", ignoreCase = true)) {
            val pending = pendingConfirm.remove(userId)
            if (pending != null) {
                executeCommand(pending, channel)
            } else {
                channel.sendMessage("⚠️ No pending command to confirm.").queue()
            }
            return
        }

        val baseCmd = content.split(" ").first().lowercase()
        if (baseCmd in plugin.cfg.destructiveCommands) {
            pendingConfirm[userId] = content
            val embed = EmbedBuilder()
                .setTitle("⚠️ Destructive Command")
                .setDescription("You are about to run:\n```\n$content\n```\nType `confirm` within **30 seconds** to execute.")
                .setColor(Color(255, 165, 0))
                .setFooter("myPlugins • myplugins.org")
                .build()
            channel.sendMessageEmbeds(embed).queue()
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
                if (pendingConfirm.remove(userId) != null) {
                    channel.sendMessage("⏱️ Confirmation expired for `$content`.").queue()
                }
            }, 600L)
            return
        }

        executeCommand(content, channel)
    }

    private fun executeCommand(command: String, channel: TextChannel) {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                val lines = synchronized(forwarder.recentLines) {
                    forwarder.recentLines.takeLast(20).joinToString("\n")
                }
                val embed = EmbedBuilder()
                    .setTitle("✅ Executed: `$command`")
                    .setDescription("**Last 20 console lines:**\n```ansi\n${lines.take(3900)}\n```")
                    .setColor(Color(171, 76, 219))
                    .setFooter("myPlugins • myplugins.org")
                    .setTimestamp(java.time.Instant.now())
                    .build()
                channel.sendMessageEmbeds(embed).queue()
            }, 20L)
        })
    }
}