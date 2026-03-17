package org.myplugins.mylink.mongo

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.Updates
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import java.util.UUID

class LinkRepository(private val db: MongoDatabase) {

    // ── Pending codes ─────────────────────────────────────────────────────────

    suspend fun savePendingCode(code: String, uuid: UUID) {
        db.pending.insertOne(Document(mapOf(
            "code" to code,
            "uuid" to uuid.toString(),
            "createdAt" to java.util.Date(),
        )))
    }

    suspend fun consumePendingCode(code: String): UUID? {
        val doc = db.pending.findOneAndDelete(Filters.eq("code", code.uppercase())) ?: return null
        return doc.getString("uuid")?.let { UUID.fromString(it) }
    }

    suspend fun hasPendingCode(uuid: UUID): Boolean =
        db.pending.find(Filters.eq("uuid", uuid.toString())).firstOrNull() != null

    suspend fun findPendingCode(uuid: UUID): String? =
        db.pending.find(Filters.eq("uuid", uuid.toString())).firstOrNull()?.getString("code")

    suspend fun deletePendingCode(uuid: UUID) {
        db.pending.deleteMany(Filters.eq("uuid", uuid.toString()))
    }

    // ── Linked accounts ───────────────────────────────────────────────────────

    suspend fun saveLink(uuid: UUID, discordId: String, nick: String) {
        db.links.findOneAndReplace(
            Filters.eq("uuid", uuid.toString()),
            Document(mapOf(
                "uuid" to uuid.toString(),
                "discordId" to discordId,
                "linkedAt" to java.util.Date(),
                "lastNick" to nick,
            )),
            FindOneAndReplaceOptions().upsert(true)
        )
    }

    suspend fun removeLink(uuid: UUID) {
        db.links.deleteOne(Filters.eq("uuid", uuid.toString()))
    }

    suspend fun findByUuid(uuid: UUID): LinkedAccount? =
        db.links.find(Filters.eq("uuid", uuid.toString())).firstOrNull()?.toLinkedAccount()

    suspend fun findByDiscordId(discordId: String): LinkedAccount? =
        db.links.find(Filters.eq("discordId", discordId)).firstOrNull()?.toLinkedAccount()

    suspend fun isLinked(uuid: UUID): Boolean = findByUuid(uuid) != null

    suspend fun updateNick(uuid: UUID, nick: String) {
        db.links.updateOne(
            Filters.eq("uuid", uuid.toString()),
            Updates.set("lastNick", nick)
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun Document.toLinkedAccount() = LinkedAccount(
        uuid = getString("uuid"),
        discordId = getString("discordId"),
        linkedAt = getDate("linkedAt")?.time ?: 0L,
        lastNick = getString("lastNick") ?: "",
    )
}

data class LinkedAccount(
    val uuid: String,
    val discordId: String,
    val linkedAt: Long,
    val lastNick: String,
)