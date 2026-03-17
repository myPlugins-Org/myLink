package org.myplugins.mylink.config

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.myplugins.mylink.MyLinkPlugin

class PluginConfig(private val plugin: MyLinkPlugin) {

    private val mm = MiniMessage.miniMessage()

    private val prefixRaw = "<#CB54F4>m<#BB50E8>y<#AA4CDB>P<#9A48CF>l<#8944C3>u<#793FB6>g<#683BAA>i<#58379D>n<#473391>s"

    // ── Core ──────────────────────────────────────────────────────────────────
    val botToken: String get() = plugin.config.getString("bot-token", "")!!
    val guildId: String get() = plugin.config.getString("guild-id", "")!!

    // ── MongoDB ───────────────────────────────────────────────────────────────
    val mongoUri: String get() = plugin.config.getString("mongodb.uri", "")!!
    val mongoDatabase: String get() = plugin.config.getString("mongodb.database", "mylink")!!

    // ── Linking ───────────────────────────────────────────────────────────────
    val codeExpiry: Long get() = plugin.config.getLong("linking.code-expiry", 300)
    val codeCooldown: Long get() = plugin.config.getLong("linking.code-cooldown", 30)
    val verifyChannel: String get() = plugin.config.getString("linking.verify-channel", "")!!

    // ── Join Gate ─────────────────────────────────────────────────────────────
    val joinGateEnabled: Boolean get() = plugin.config.getBoolean("join-gate.enabled", false)
    val joinGateKickMessage: String get() = plugin.config.getString("join-gate.kick-message", "<red>You must link your account!")!!
    val joinGateUnlinkKickMessage: String get() = plugin.config.getString("join-gate.unlink-kick-message", "<red>Your account has been unlinked.")!!
    val joinGateBypassNames: Set<String> get() = plugin.config.getStringList("join-gate.bypass-names").map { it.lowercase() }.toSet()
    val joinGateWhitelistBypass: Boolean get() = plugin.config.getBoolean("join-gate.whitelist-bypass", true)
    val joinGateRequireGuild: Boolean get() = plugin.config.getBoolean("join-gate.require-guild-member", true)
    val joinGateRequireRoles: Boolean get() = plugin.config.getBoolean("join-gate.require-roles.enabled", false)
    val joinGateRequireAll: Boolean get() = plugin.config.getBoolean("join-gate.require-roles.require-all", false)
    val joinGateRoleIds: List<String> get() = plugin.config.getStringList("join-gate.require-roles.role-ids")
    val joinGateRoleKickMessage: String get() = plugin.config.getString("join-gate.require-roles.kick-message", "<red>You must have the required Discord role!")!!

    // ── Chat Bridge ───────────────────────────────────────────────────────────
    val chatBridgeEnabled: Boolean get() = plugin.config.getBoolean("chat-bridge.enabled", true)
    val chatBridgeChannel: String get() = plugin.config.getString("chat-bridge.channel-id", "")!!
    val chatBridgeWebhooks: Boolean get() = plugin.config.getBoolean("chat-bridge.use-webhooks", true)
    val chatMcToDcFormat: String get() = plugin.config.getString("chat-bridge.mc-to-dc-format", "**%player%** %message%")!!
    val chatDcToMcFormat: String get() = plugin.config.getString("chat-bridge.dc-to-mc-format", "<dark_gray>[<#9A48CF>Discord</dark_gray>] <white>%user%<gray>: %message%")!!
    val chatConvertFormatting: Boolean get() = plugin.config.getBoolean("chat-bridge.convert-formatting", true)

    // ── Console ───────────────────────────────────────────────────────────────
    val consoleEnabled: Boolean get() = plugin.config.getBoolean("console.enabled", false)
    val consoleChannel: String get() = plugin.config.getString("console.channel-id", "")!!
    val consoleWhitelist: Set<String> get() = plugin.config.getStringList("console.whitelist").toSet()
    val destructiveCommands: Set<String> get() = plugin.config.getStringList("console.destructive-commands").map { it.lowercase() }.toSet()

    // ── Group Sync ────────────────────────────────────────────────────────────
    val groupSyncEnabled: Boolean get() = plugin.config.getBoolean("group-sync.enabled", false)
    val groupSyncAddCommand: String get() = plugin.config.getString("group-sync.add-command", "lp user %player% parent add %group%")!!
    val groupSyncRemoveCommand: String get() = plugin.config.getString("group-sync.remove-command", "lp user %player% parent remove %group%")!!

    val groupMappings: List<GroupMapping> get() {
        val list = plugin.config.getList("group-sync.mappings") ?: return emptyList()
        return list.filterIsInstance<Map<*, *>>().mapNotNull { map ->
            val mc = map["mc-group"] as? String ?: return@mapNotNull null
            val dc = map["dc-role"] as? String ?: return@mapNotNull null
            GroupMapping(mc, dc)
        }
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    private fun raw(key: String): String =
        plugin.config.getString("messages.$key", "")!!

    fun msg(key: String, vararg placeholders: Pair<String, String>): Component {
        var text = raw(key)
        placeholders.forEach { (k, v) -> text = text.replace("%$k%", v) }
        return mm.deserialize(text)
    }

    fun msgRaw(key: String, vararg placeholders: Pair<String, String>): String {
        var text = raw(key)
        placeholders.forEach { (k, v) -> text = text.replace("%$k%", v) }
        return text
    }

    // ── Validation ────────────────────────────────────────────────────────────
    fun validate(): List<String> = buildList {
        if (botToken.isBlank() || botToken == "CHANGE_ME") add("bot-token is not configured")
        if (guildId.isBlank() || guildId == "CHANGE_ME") add("guild-id is not configured")
        if (mongoUri.isBlank() || mongoUri == "CHANGE_ME") add("mongodb.uri is not configured")
        if (verifyChannel.isBlank() || verifyChannel == "CHANGE_ME") add("linking.verify-channel is not configured")
        if (chatBridgeEnabled && (chatBridgeChannel.isBlank() || chatBridgeChannel == "CHANGE_ME")) add("chat-bridge.channel-id is not configured")
        if (consoleEnabled && (consoleChannel.isBlank() || consoleChannel == "CHANGE_ME")) add("console.channel-id is not configured")
    }
}

data class GroupMapping(
    val mcGroup: String,
    val dcRole: String,
)