package org.myplugins.mylink.bot.sync

import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.myplugins.mylink.MyLinkPlugin
import org.myplugins.mylink.config.GroupMapping
import java.util.UUID

class GroupSyncService(private val plugin: MyLinkPlugin) {

    private val mappings: List<GroupMapping> get() = plugin.cfg.groupMappings

    fun onMinecraftGroupAdd(uuid: UUID, group: String) {
        if (!plugin.cfg.groupSyncEnabled) return
        val mapping = mappings.firstOrNull { it.mcGroup.equals(group, ignoreCase = true) } ?: run {
            plugin.logger.info("[GroupSync] No mapping found for group: $group")
            return
        }
        plugin.logger.info("[GroupSync] Mapping found: $group -> role ${mapping.dcRole}")
        plugin.pluginScope.scope.launch {
            setDiscordRole(uuid, mapping.dcRole, add = true)
        }
    }

    fun onMinecraftGroupRemove(uuid: UUID, group: String) {
        if (!plugin.cfg.groupSyncEnabled) return
        val mapping = mappings.firstOrNull { it.mcGroup.equals(group, ignoreCase = true) } ?: run {
            plugin.logger.info("[GroupSync] No mapping found for group: $group")
            return
        }
        plugin.logger.info("[GroupSync] Mapping found: $group -> role ${mapping.dcRole}")
        plugin.pluginScope.scope.launch {
            setDiscordRole(uuid, mapping.dcRole, add = false)
        }
    }

    suspend fun onDiscordRoleAdd(uuid: String, roleId: String) {
        if (!plugin.cfg.groupSyncEnabled) return
        val mapping = mappings.firstOrNull { it.dcRole == roleId } ?: run {
            plugin.logger.info("[GroupSync] No mapping for roleId: $roleId")
            return
        }
        plugin.logger.info("[GroupSync] Discord -> MC add: group=${mapping.mcGroup} uuid=$uuid")
        val player = Bukkit.getOfflinePlayer(UUID.fromString(uuid))
        val name = player.name ?: run {
            plugin.logger.info("[GroupSync] Player name not found for UUID: $uuid")
            return
        }
        runCommand(plugin.cfg.groupSyncAddCommand, name, mapping.mcGroup)
    }

    suspend fun onDiscordRoleRemove(uuid: String, roleId: String) {
        if (!plugin.cfg.groupSyncEnabled) return
        val mapping = mappings.firstOrNull { it.dcRole == roleId } ?: run {
            plugin.logger.info("[GroupSync] No mapping for roleId: $roleId")
            return
        }
        plugin.logger.info("[GroupSync] Discord -> MC remove: group=${mapping.mcGroup} uuid=$uuid")
        val player = Bukkit.getOfflinePlayer(UUID.fromString(uuid))
        val name = player.name ?: run {
            plugin.logger.info("[GroupSync] Player name not found for UUID: $uuid")
            return
        }
        runCommand(plugin.cfg.groupSyncRemoveCommand, name, mapping.mcGroup)
    }

    suspend fun syncToDiscord(uuid: UUID) {
        if (!plugin.cfg.groupSyncEnabled) return
    }

    private suspend fun setDiscordRole(uuid: UUID, roleId: String, add: Boolean) {
        val account = plugin.linkService.getLinkedAccount(uuid) ?: run {
            plugin.logger.info("[GroupSync] No linked account found for UUID: $uuid")
            return
        }
        plugin.logger.info("[GroupSync] Found linked account: discordId=${account.discordId}")

        val guild = plugin.botManager.guild ?: run {
            plugin.logger.info("[GroupSync] Guild is null — bot not ready?")
            return
        }

        val role = guild.getRoleById(roleId) ?: run {
            plugin.logger.info("[GroupSync] Role not found: $roleId")
            return
        }

        val member = guild.getMemberById(account.discordId) ?: run {
            plugin.logger.info("[GroupSync] Member not found in guild: ${account.discordId} — trying to retrieve...")
            // Member might not be cached — retrieve explicitly
            guild.retrieveMemberById(account.discordId).queue({ retrieved ->
                plugin.logger.info("[GroupSync] Member retrieved: ${retrieved.user.name}")
                if (add) guild.addRoleToMember(retrieved, role).queue(
                    { plugin.logger.info("[GroupSync] Role added successfully.") },
                    { plugin.logger.info("[GroupSync] Failed to add role: ${it.message}") }
                )
                else guild.removeRoleFromMember(retrieved, role).queue(
                    { plugin.logger.info("[GroupSync] Role removed successfully.") },
                    { plugin.logger.info("[GroupSync] Failed to remove role: ${it.message}") }
                )
            }, {
                plugin.logger.info("[GroupSync] Could not retrieve member: ${it.message}")
            })
            return
        }

        plugin.logger.info("[GroupSync] Applying role ${role.name} to ${member.user.name}, add=$add")
        if (add) guild.addRoleToMember(member, role).queue(
            { plugin.logger.info("[GroupSync] Role added successfully.") },
            { plugin.logger.info("[GroupSync] Failed to add role: ${it.message}") }
        )
        else guild.removeRoleFromMember(member, role).queue(
            { plugin.logger.info("[GroupSync] Role removed successfully.") },
            { plugin.logger.info("[GroupSync] Failed to remove role: ${it.message}") }
        )
    }

    private fun runCommand(template: String, player: String, group: String) {
        val command = template
            .replace("%player%", player)
            .replace("%group%", group)
        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        })
    }
}