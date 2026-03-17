package org.myplugins.mylink.listener

import net.luckperms.api.LuckPerms
import net.luckperms.api.event.node.NodeAddEvent
import net.luckperms.api.event.node.NodeRemoveEvent
import net.luckperms.api.model.user.User
import net.luckperms.api.node.types.InheritanceNode
import org.bukkit.Bukkit
import org.myplugins.mylink.MyLinkPlugin

class LuckPermsListener(private val plugin: MyLinkPlugin) {

    fun register() {
        val lp = Bukkit.getServicesManager().load(LuckPerms::class.java) ?: run {
            plugin.logger.info("LuckPerms not found — group sync will use command fallback only.")
            return
        }

        lp.eventBus.subscribe(plugin, NodeAddEvent::class.java) { event ->
            // Only care about users, not groups
            if (!event.isUser) return@subscribe
            val node = event.node as? InheritanceNode ?: return@subscribe
            val user = event.target as? User ?: return@subscribe
            plugin.logger.info("[GroupSync] Detected group ADD: ${node.groupName} for ${user.uniqueId}")
            plugin.groupSyncService.onMinecraftGroupAdd(user.uniqueId, node.groupName)
        }

        lp.eventBus.subscribe(plugin, NodeRemoveEvent::class.java) { event ->
            if (!event.isUser) return@subscribe
            val node = event.node as? InheritanceNode ?: return@subscribe
            val user = event.target as? User ?: return@subscribe
            plugin.logger.info("[GroupSync] Detected group REMOVE: ${node.groupName} for ${user.uniqueId}")
            plugin.groupSyncService.onMinecraftGroupRemove(user.uniqueId, node.groupName)
        }

        plugin.logger.info("LuckPerms integration enabled for group sync.")
    }
}