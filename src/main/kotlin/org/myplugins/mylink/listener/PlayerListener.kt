package org.myplugins.mylink.listener

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.myplugins.mylink.MyLinkPlugin
import org.myplugins.mylink.config.PluginConfig
import org.myplugins.mylink.link.LinkService
import org.myplugins.mylink.util.PluginScope

class PlayerListener(
    private val service: LinkService,
    private val config: PluginConfig,
    private val scope: PluginScope,
) : Listener {

    private val mm = MiniMessage.miniMessage()

    @EventHandler(priority = EventPriority.HIGH)
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (!config.joinGateEnabled) return
        if (event.name.lowercase() in config.joinGateBypassNames) return

        if (config.joinGateWhitelistBypass) {
            val whitelisted = Bukkit.getWhitelistedPlayers().any { it.uniqueId == event.uniqueId }
            if (whitelisted) return
        }

        val linked = runBlocking { service.isLinked(event.uniqueId) }
        if (!linked) {
            val code = runBlocking { service.getOrGenerateCode(event.uniqueId) } ?: "ERROR"
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                mm.deserialize(config.joinGateKickMessage.replace("%code%", code))
            )
            return
        }

        if (!config.joinGateRequireRoles) return

        val plugin = Bukkit.getServer().pluginManager
            .getPlugin("myLink") as? MyLinkPlugin ?: return

        if (!plugin.isBotReady()) return

        val account = runBlocking { service.getLinkedAccount(event.uniqueId) } ?: run {
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                mm.deserialize(config.joinGateKickMessage.replace("%code%", ""))
            )
            return
        }

        val member = plugin.botManager.guild?.getMemberById(account.discordId) ?: run {
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                mm.deserialize(config.joinGateKickMessage.replace("%code%", ""))
            )
            return
        }

        val memberRoleIds = member.roles.map { it.id }
        val requiredRoles = config.joinGateRoleIds
        val hasRoles = if (config.joinGateRequireAll) {
            requiredRoles.all { it in memberRoleIds }
        } else {
            requiredRoles.any { it in memberRoleIds }
        }

        if (!hasRoles) {
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                mm.deserialize(config.joinGateRoleKickMessage)
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onJoin(event: PlayerJoinEvent) {
        scope.scope.launch {
            if (service.isLinked(event.player.uniqueId)) {
                service.updateNick(event.player.uniqueId, event.player.name)
            }
        }
    }
}

// Handles kicking online players when they unlink with gate enabled
class UnlinkKickListener(
    private val config: PluginConfig,
) : Listener {

    private val mm = MiniMessage.miniMessage()

    fun kickIfNeeded(plugin: MyLinkPlugin, uuid: java.util.UUID) {
        if (!config.joinGateEnabled) return
        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.getPlayer(uuid)?.kick(
                mm.deserialize(config.joinGateUnlinkKickMessage)
            )
        })
    }
}