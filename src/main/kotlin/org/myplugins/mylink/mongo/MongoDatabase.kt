package org.myplugins.mylink.mongo

import com.mongodb.client.model.IndexOptions
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase as KMongoDatabase
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.myplugins.mylink.config.PluginConfig
import java.util.concurrent.TimeUnit

class MongoDatabase(private val config: PluginConfig) {

    private lateinit var client: MongoClient
    private lateinit var db: KMongoDatabase

    val links: MongoCollection<Document> get() = db.getCollection("links")
    val pending: MongoCollection<Document> get() = db.getCollection("pending_codes")

    fun connect() {
        client = MongoClient.create(config.mongoUri)
        db = client.getDatabase(config.mongoDatabase)
        runBlocking { ensureIndexes() }
    }

    fun disconnect() {
        client.close()
    }

    private suspend fun ensureIndexes() {
        // Unique index on uuid — fast lookup, no duplicate links
        links.createIndex(
            Document("uuid", 1),
            IndexOptions().unique(true)
        )
        // TTL index — MongoDB auto-deletes expired pending codes
        pending.createIndex(
            Document("createdAt", 1),
            IndexOptions().expireAfter(config.codeExpiry, TimeUnit.SECONDS)
        )
        // Unique index on code — no duplicate pending codes
        pending.createIndex(
            Document("code", 1),
            IndexOptions().unique(true)
        )
    }
}