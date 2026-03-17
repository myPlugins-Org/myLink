package org.myplugins.mylink

import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import org.myplugins.mylink.api.BotAdapterImpl
import org.myplugins.mylink.api.MyLinkAPI
import org.myplugins.mylink.api.MyLinkAPIImpl
import org.myplugins.mylink.bot.BotManager
import org.myplugins.mylink.bot.ConsoleForwarder
import org.myplugins.mylink.bot.sync.GroupSyncService
import org.myplugins.mylink.command.AdminCommand
import org.myplugins.mylink.command.LinkCommand
import org.myplugins.mylink.config.PluginConfig
import org.myplugins.mylink.link.LinkService
import org.myplugins.mylink.listener.ChatListener
import org.myplugins.mylink.listener.LuckPermsListener
import org.myplugins.mylink.listener.PlayerListener
import org.myplugins.mylink.mongo.LinkRepository
import org.myplugins.mylink.mongo.MongoDatabase
import org.myplugins.mylink.util.PluginScope

class MyLinkPlugin : JavaPlugin() {

    lateinit var cfg: PluginConfig
        private set
    lateinit var pluginScope: PluginScope
        private set
    lateinit var linkService: LinkService
        private set
    lateinit var botManager: BotManager
        private set
    lateinit var groupSyncService: GroupSyncService
        private set

    private lateinit var database: MongoDatabase
    private lateinit var consoleForwarder: ConsoleForwarder
    private lateinit var apiImpl: MyLinkAPIImpl

    private var dbConnected = false
    private var botReady = false

    override fun onEnable() {
        saveDefaultConfig()
        cfg = PluginConfig(this)

        printBanner()

        val problems = cfg.validate()
        if (problems.isNotEmpty()) {
            logger.warning("════════════════════════════════════")
            logger.warning("  myLink — missing configuration:")
            problems.forEach { logger.warning("  • $it") }
            logger.warning("  Edit plugins/myLink/config.yml")
            logger.warning("  and run /mylink reload when done.")
            logger.warning("════════════════════════════════════")
        }

        pluginScope = PluginScope(name)

        if (cfg.mongoUri.isBlank() || cfg.mongoUri == "CHANGE_ME") {
            logger.severe("MongoDB URI not configured. Plugin will not function.")
            return
        }

        try {
            database = MongoDatabase(cfg)
            database.connect()
            dbConnected = true
            logger.info("Connected to MongoDB.")
        } catch (e: Exception) {
            logger.severe("Failed to connect to MongoDB: ${e.message}")
            return
        }

        val repo = LinkRepository(database)
        linkService = LinkService(repo, cfg)
        groupSyncService = GroupSyncService(this)
        apiImpl = MyLinkAPIImpl(linkService)

        botManager = BotManager(this)
        pluginScope.scope.launch {
            try {
                botManager.start()
                apiImpl.botAdapter = BotAdapterImpl(this@MyLinkPlugin)
                consoleForwarder = ConsoleForwarder(this@MyLinkPlugin)
                consoleForwarder.start()
                logger.info("Discord bot ready.")
            } catch (e: Exception) {
                logger.severe("Failed to start Discord bot: ${e.message}")
            }
        }

        server.servicesManager.register(MyLinkAPI::class.java, apiImpl, this, ServicePriority.Normal)

        val linkCmd = LinkCommand(linkService, cfg, pluginScope)
        getCommand("link")!!.setExecutor(linkCmd)
        getCommand("unlink")!!.setExecutor(linkCmd)
        getCommand("mylink")!!.let {
            val cmd = AdminCommand(this, linkService, pluginScope)
            it.setExecutor(cmd)
            it.tabCompleter = cmd
        }

        server.pluginManager.registerEvents(PlayerListener(linkService, cfg, pluginScope), this)
        server.pluginManager.registerEvents(ChatListener(this), this)

        LuckPermsListener(this).register()
    }

    override fun onDisable() {
        if (::consoleForwarder.isInitialized) consoleForwarder.stop()
        if (::botManager.isInitialized) botManager.shutdown()
        pluginScope.cancel()
        if (dbConnected) database.disconnect()
        server.servicesManager.unregisterAll(this)
        logger.info("myLink disabled.")
    }

    private fun printBanner() {
        val mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
        val lines = listOf(
            "",
            "  <#CB54F4>m<#BB50E8>y<#AA4CDB>P<#9A48CF>l<#8944C3>u<#793FB6>g<#683BAA>i<#58379D>n<#473391>s",
            "  <#9A48CF>myLink <gray>— Minecraft ↔ Discord Bridge",
            "  <gray>v${description.version} by <#CB54F4>m<#BB50E8>y<#AA4CDB>P<#9A48CF>l<#8944C3>u<#793FB6>g<#683BAA>i<#58379D>n<#473391>s",
            "",
        )
        lines.forEach { Bukkit.getConsoleSender().sendMessage(mm.deserialize(it)) }
    }

    fun isDbConnected() = dbConnected
    fun isBotReady() = botReady
    fun setBotReady(ready: Boolean) { botReady = ready }
}