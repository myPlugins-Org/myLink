package org.myplugins.mylink.link

import org.myplugins.mylink.config.PluginConfig
import org.myplugins.mylink.mongo.LinkedAccount
import org.myplugins.mylink.mongo.LinkRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

sealed class LinkResult {
    data class CodeGenerated(val code: String, val expirySeconds: Long) : LinkResult()
    object AlreadyLinked : LinkResult()
    data class CooldownRemaining(val seconds: Long) : LinkResult()
    object DatabaseError : LinkResult()
}

sealed class UnlinkResult {
    object Success : UnlinkResult()
    object NotLinked : UnlinkResult()
    object DatabaseError : UnlinkResult()
}

sealed class ConsumeCodeResult {
    data class Success(val uuid: UUID, val discordId: String) : ConsumeCodeResult()
    object InvalidCode : ConsumeCodeResult()
    object DatabaseError : ConsumeCodeResult()
}

class LinkService(
    private val repo: LinkRepository,
    private val config: PluginConfig,
) {

    private val cooldowns = ConcurrentHashMap<UUID, Long>()

    // ── /link command — player requested a code ───────────────────────────────

    suspend fun generateCode(uuid: UUID): LinkResult {
        return try {
            if (repo.isLinked(uuid)) return LinkResult.AlreadyLinked
            val remaining = cooldownRemaining(uuid)
            if (remaining > 0) return LinkResult.CooldownRemaining(remaining)
            repo.deletePendingCode(uuid)
            val code = generateUniqueCode()
            repo.savePendingCode(code, uuid)
            cooldowns[uuid] = System.currentTimeMillis()
            LinkResult.CodeGenerated(code, config.codeExpiry)
        } catch (e: Exception) {
            LinkResult.DatabaseError
        }
    }

    // ── Join gate — get existing code or generate a new one silently ──────────

    suspend fun getOrGenerateCode(uuid: UUID): String? {
        return try {
            // Reuse existing pending code if still valid
            if (repo.hasPendingCode(uuid)) {
                return repo.findPendingCode(uuid)
            }
            // Generate fresh code — no cooldown check here, player needs it to join
            repo.deletePendingCode(uuid)
            val code = generateUniqueCode()
            repo.savePendingCode(code, uuid)
            cooldowns[uuid] = System.currentTimeMillis()
            code
        } catch (e: Exception) {
            null
        }
    }

    // ── Consume code — called by Discord bot after /link <code> ──────────────

    suspend fun consumeCode(code: String, discordId: String, nick: String): ConsumeCodeResult {
        return try {
            val uuid = repo.consumePendingCode(code) ?: return ConsumeCodeResult.InvalidCode
            repo.saveLink(uuid, discordId, nick)
            cooldowns.remove(uuid)
            ConsumeCodeResult.Success(uuid, discordId)
        } catch (e: Exception) {
            ConsumeCodeResult.DatabaseError
        }
    }

    // ── Unlink ────────────────────────────────────────────────────────────────

    suspend fun unlink(uuid: UUID): UnlinkResult {
        return try {
            if (!repo.isLinked(uuid)) return UnlinkResult.NotLinked
            repo.removeLink(uuid)
            UnlinkResult.Success
        } catch (e: Exception) {
            UnlinkResult.DatabaseError
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    suspend fun getLinkedAccount(uuid: UUID): LinkedAccount? = repo.findByUuid(uuid)
    suspend fun isLinked(uuid: UUID): Boolean = repo.isLinked(uuid)
    suspend fun updateNick(uuid: UUID, nick: String) = repo.updateNick(uuid, nick)
    suspend fun findByDiscordId(discordId: String): LinkedAccount? = repo.findByDiscordId(discordId)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun cooldownRemaining(uuid: UUID): Long {
        val last = cooldowns[uuid] ?: return 0L
        val elapsed = (System.currentTimeMillis() - last) / 1000
        val remaining = config.codeCooldown - elapsed
        return if (remaining > 0) remaining else 0L
    }

    private suspend fun generateUniqueCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        repeat(10) {
            val code = "MP-" + (1..4).map { chars.random() }.joinToString("")
            if (!repo.hasPendingCode(UUID.randomUUID())) return code
        }
        throw IllegalStateException("Could not generate a unique code after 10 attempts")
    }
}