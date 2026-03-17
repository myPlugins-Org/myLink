package org.myplugins.mylink.command

import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.myplugins.mylink.MyLinkPlugin
import org.myplugins.mylink.link.LinkService
import org.myplugins.mylink.link.UnlinkResult
import org.myplugins.mylink.util.PluginScope

class AdminCommand(
    private val plugin: MyLinkPlugin,
    private val service: LinkService,
    private val scope: PluginScope,
) : CommandExecutor, TabCompleter {

    private val mm = MiniMessage.miniMessage()
    private val prefix = "<#CB54F4>m<#BB50E8>y<#AA4CDB>P<#9A48CF>l<#8944C3>u<#793FB6>g<#683BAA>i<#58379D>n<#473391>s"

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("mylink.admin")) {
            sender.sendMessage(plugin.cfg.msg("no-permission"))
            return true
        }
        when (args.getOrNull(0)?.lowercase()) {
            "reload" -> handleReload(sender)
            "status" -> handleStatus(sender)
            "forceunlink" -> handleForceUnlink(sender, args)
            else -> handleHelp(sender)
        }
        return true
    }

    private fun handleReload(sender: CommandSender) {
        plugin.reloadConfig()
        sender.sendMessage(mm.deserialize("$prefix <dark_gray>» <green>Configuration reloaded successfully."))
    }

    private fun handleStatus(sender: CommandSender) {
        val botColor = if (plugin.isBotReady()) "#57F287" else "#ED4245"
        val botLabel = if (plugin.isBotReady()) "● Online" else "● Offline"
        val dbColor = if (plugin.isDbConnected()) "#57F287" else "#ED4245"
        val dbLabel = if (plugin.isDbConnected()) "● Connected" else "● Disconnected"

        val lines = listOf(
            "",
            "$prefix <dark_gray>| <#9A48CF>myLink <gray>v${plugin.description.version}",
            "<dark_gray>  ├ <gray>Bot    <$botColor>$botLabel",
            "<dark_gray>  └ <gray>DB     <$dbColor>$dbLabel",
            "",
        )
        lines.forEach { sender.sendMessage(mm.deserialize(it)) }
    }

    private fun handleForceUnlink(sender: CommandSender, args: Array<String>) {
        val name = args.getOrNull(1) ?: run {
            sender.sendMessage(mm.deserialize("$prefix <dark_gray>» <red>Usage: <gray>/mylink forceunlink <player>"))
            return
        }
        val target = plugin.server.getOfflinePlayer(name)
        scope.scope.launch {
            when (service.unlink(target.uniqueId)) {
                is UnlinkResult.Success ->
                    sender.sendMessage(mm.deserialize("$prefix <dark_gray>» <green>$name has been unlinked."))
                is UnlinkResult.NotLinked ->
                    sender.sendMessage(mm.deserialize("$prefix <dark_gray>» <yellow>$name is not linked."))
                is UnlinkResult.DatabaseError ->
                    sender.sendMessage(mm.deserialize("$prefix <dark_gray>» <red>Database error while unlinking."))
            }
        }
    }

    private fun handleHelp(sender: CommandSender) {
        val lines = listOf(
            "",
            "$prefix <dark_gray>| <#9A48CF>myLink <gray>— Admin Commands",
            "<dark_gray>  ├ <#9A48CF>/mylink reload <dark_gray>» <gray>Reload the configuration",
            "<dark_gray>  ├ <#9A48CF>/mylink status <dark_gray>» <gray>Show bot and database status",
            "<dark_gray>  └ <#9A48CF>/mylink forceunlink <player> <dark_gray>» <gray>Unlink a player's account",
            "",
        )
        lines.forEach { sender.sendMessage(mm.deserialize(it)) }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (!sender.hasPermission("mylink.admin")) return emptyList()
        return when (args.size) {
            1 -> listOf("reload", "status", "forceunlink").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> if (args[0].equals("forceunlink", ignoreCase = true))
                plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
            else emptyList()
            else -> emptyList()
        }
    }
}