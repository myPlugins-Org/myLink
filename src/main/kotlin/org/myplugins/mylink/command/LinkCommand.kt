package org.myplugins.mylink.command

import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.myplugins.mylink.config.PluginConfig
import org.myplugins.mylink.link.LinkResult
import org.myplugins.mylink.link.LinkService
import org.myplugins.mylink.link.UnlinkResult
import org.myplugins.mylink.util.PluginScope
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LinkCommand(
    private val service: LinkService,
    private val config: PluginConfig,
    private val scope: PluginScope,
) : CommandExecutor {

    private val mm = MiniMessage.miniMessage()
    private val awaitingConfirm = ConcurrentHashMap.newKeySet<UUID>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used in-game.")
            return true
        }
        return when (command.name.lowercase()) {
            "link" -> handleLink(sender)
            "unlink" -> handleUnlink(sender, args)
            else -> false
        }
    }

    private fun handleLink(player: Player): Boolean {
        scope.scope.launch {
            val result = service.generateCode(player.uniqueId)
            player.scheduler.run(player.server.pluginManager.getPlugin("myLink")!!, { _ ->
                when (result) {
                    is LinkResult.CodeGenerated -> {
                        // Main message
                        player.sendMessage(
                            config.msg("link-code", "code" to result.code, "expiry" to result.expirySeconds.toString())
                        )
                        // Click to copy button
                        val copyCmd = "/link ${result.code}"
                        val copyButton = mm.deserialize(
                            "<dark_gray>  » <click:copy_to_clipboard:'$copyCmd'><#9A48CF>[ Click to copy <white>$copyCmd</white> ]</click>"
                        ).clickEvent(ClickEvent.copyToClipboard(copyCmd))
                        player.sendMessage(copyButton)
                    }
                    is LinkResult.AlreadyLinked -> player.sendMessage(config.msg("link-already-linked"))
                    is LinkResult.CooldownRemaining -> player.sendMessage(
                        config.msg("link-cooldown", "seconds" to result.seconds.toString())
                    )
                    is LinkResult.DatabaseError -> player.sendMessage(config.msg("db-error"))
                }
            }, null)
        }
        return true
    }

    private fun handleUnlink(player: Player, args: Array<String>): Boolean {
        val uuid = player.uniqueId
        if (args.isNotEmpty() && args[0].equals("confirm", ignoreCase = true) && awaitingConfirm.remove(uuid)) {
            scope.scope.launch {
                val result = service.unlink(uuid)
                player.scheduler.run(player.server.pluginManager.getPlugin("myLink")!!, { _ ->
                    when (result) {
                        is UnlinkResult.Success -> {
                            player.sendMessage(config.msg("unlink-success"))
                            // Kick if join gate is enabled
                            if (config.joinGateEnabled) {
                                player.kick(
                                    MiniMessage.miniMessage().deserialize(config.joinGateUnlinkKickMessage)
                                )
                            }
                        }                        is UnlinkResult.NotLinked -> player.sendMessage(config.msg("unlink-not-linked"))
                        is UnlinkResult.DatabaseError -> player.sendMessage(config.msg("db-error"))
                    }
                }, null)
            }
        } else {
            awaitingConfirm.add(uuid)
            player.sendMessage(config.msg("unlink-confirm"))
            player.server.scheduler.runTaskLaterAsynchronously(
                player.server.pluginManager.getPlugin("myLink")!!,
                Runnable { awaitingConfirm.remove(uuid) },
                600L
            )
        }
        return true
    }
}