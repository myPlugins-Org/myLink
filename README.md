<div align="center">

<img src="https://i.imgur.com/uHZq9Df.png" alt="myLink" width="80"/>

# myLink

**The bridge between your Minecraft server and Discord.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Paper](https://img.shields.io/badge/Paper-1.21.11-F7CF0D?style=flat-square&logo=papercms&logoColor=black)](https://papermc.io)
[![JDA](https://img.shields.io/badge/JDA-5.2.1-5865F2?style=flat-square&logo=discord&logoColor=white)](https://github.com/discord-jda/JDA)
[![MongoDB](https://img.shields.io/badge/MongoDB-5.2.1-47A248?style=flat-square&logo=mongodb&logoColor=white)](https://www.mongodb.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square)](LICENSE)
[![Release](https://img.shields.io/badge/Release-1.0.0--beta-CB54F4?style=flat-square)](https://github.com/myplugins-mc/myLink/releases)

Part of the **[myPlugins](https://myplugins.org)** ecosystem.

[📦 Download](#download) • [📖 Documentation](#configuration) • [🔌 API](#developer-api) • [💬 Discord](https://discord.gg/myplugins)

</div>

---

## ✨ Features

- 🔗 **Account Linking** — Players link their Minecraft account to Discord with a simple code. Supports join gate to require linking before playing.
- 💬 **Bidirectional Chat Bridge** — Sync chat between Minecraft and Discord with full webhook support, showing player skins via MCHeads.
- 🖥️ **Console Forwarding** — Execute server commands from a Discord channel. Destructive commands require confirmation. Shows the last 20 log lines after execution.
- 🔄 **Bidirectional Group Sync** — LuckPerms groups and Discord roles stay in sync automatically. Native LuckPerms event API with command fallback for other permission plugins.
- 🎨 **Modern Slash Commands** — `/link`, `/unlink`, `/profile` with rich embeds.
- 🔌 **Public API** — Expose linking data and bot channels to other plugins in your ecosystem.

---

## 📋 Requirements

| Dependency | Version | Required |
|---|---|---|
| [Spigot/Paper](https://papermc.io/downloads/paper) | 1.21+ | ✅ |
| [MongoDB Atlas](https://www.mongodb.com/atlas) | Any | ✅ |
| [LuckPerms](https://luckperms.net) | 5.4+ | ⚠️ Optional (for group sync) |

> **Note:** myLink requires your own Discord bot. Create one at [discord.com/developers](https://discord.com/developers/applications).

---

## 📦 Download

Download the latest `.jar` from the [Releases](https://github.com/myplugins-mc/myLink/releases) page and drop it into your `plugins/` folder.

No additional setup is required — myLink generates a `config.yml` on first boot.

---

## ⚙️ Configuration

On first boot, myLink creates `plugins/myLink/config.yml`. The minimum required configuration is:

```yaml
bot-token: "YOUR_BOT_TOKEN"
guild-id: "YOUR_DISCORD_SERVER_ID"

mongodb:
  uri: "mongodb+srv://user:pass@cluster.mongodb.net/mylink"

linking:
  verify-channel: "DISCORD_CHANNEL_ID"

chat-bridge:
  channel-id: "DISCORD_CHANNEL_ID"
```

After editing, run `/mylink reload` in-game or in the console.

### Full configuration reference

<details>
<summary>Click to expand</summary>

```yaml
# -----------------------------------------------
#  myLink — Configuration
# -----------------------------------------------

bot-token: "CHANGE_ME"

guild-id: "CHANGE_ME"

mongodb:
  uri: "CHANGE_ME"
  database: "mylink"

# -----------------------------------------------
#  Account Linking
# -----------------------------------------------
linking:
  code-expiry: 300
  code-cooldown: 30
  verify-channel: "CHANGE_ME"

# -----------------------------------------------
#  Join Gate
# -----------------------------------------------
join-gate:
  enabled: false
  kick-message: "<red><bold>Account not linked!</bold></red>\n\n<gray>Link your account to play.\n\n<yellow>Your code: <white><bold>%code%</bold></white>\n<yellow>Join our Discord and type: <white>/link %code%"
  unlink-kick-message: "<red><bold>Account unlinked.</bold></red>\n\n<gray>Your account has been unlinked from our systems.\n<gray>Sync your account again to continue playing.\n\n<yellow>Join our Discord: <white>myplugins.org/discord"
  bypass-names: []
  whitelist-bypass: true
  require-guild-member: true
  require-roles:
    enabled: false
    require-all: false
    role-ids: []
    kick-message: "<red>You must have the required Discord role to play!"

# -----------------------------------------------
#  Chat Bridge
# -----------------------------------------------
chat-bridge:
  enabled: true
  channel-id: "CHANGE_ME"
  use-webhooks: true
  mc-to-dc-format: "**%player%** %message%"
  dc-to-mc-format: "<dark_gray>[<#9A48CF>Discord</dark_gray>] <white>%user%<gray>: %message%"
  convert-formatting: true

# -----------------------------------------------
#  Console Forward
# -----------------------------------------------
console:
  enabled: false
  channel-id: "CHANGE_ME"
  whitelist:
    - "YOUR_DISCORD_ID_HERE"
  destructive-commands:
    - "stop"
    - "restart"
    - "ban-ip"

# -----------------------------------------------
#  Group Sync
# -----------------------------------------------
group-sync:
  enabled: false
  add-command: "lp user %player% parent add %group%"
  remove-command: "lp user %player% parent remove %group%"
  mappings:
    - mc-group: "vip"
      dc-role: "DISCORD_ROLE_ID"
    - mc-group: "mvp"
      dc-role: "DISCORD_ROLE_ID"
```

</details>

---

## 🔗 Setting up the Discord Bot

1. Go to [discord.com/developers/applications](https://discord.com/developers/applications)
2. Create a **New Application** and go to the **Bot** tab
3. Enable **Server Members Intent** and **Message Content Intent**
4. Copy the bot token and paste it into `config.yml`
5. Invite the bot to your server with `bot` + `applications.commands` scopes

---

## 🎮 Commands

### Minecraft

| Command | Description | Permission |
|---|---|---|
| `/link` | Generate a linking code | `mylink.link` |
| `/unlink` | Unlink your Discord account | `mylink.unlink` |
| `/mylink reload` | Reload configuration | `mylink.admin` |
| `/mylink status` | Show bot and database status | `mylink.admin` |
| `/mylink forceunlink <player>` | Force unlink a player | `mylink.admin` |

### Discord

| Command | Description |
|---|---|
| `/link <code>` | Link your Minecraft account |
| `/unlink` | Unlink your Minecraft account |
| `/profile` | View your linked account profile |

---

## 🔄 Group Sync

myLink supports **bidirectional** group synchronization between Minecraft and Discord.

**Minecraft → Discord:** When a player receives or loses a group in LuckPerms, the corresponding Discord role is automatically added or removed.

**Discord → Minecraft:** When a Discord role is added or removed from a linked member, the corresponding LuckPerms group is automatically applied via command.

```yaml
group-sync:
  enabled: true
  add-command: "lp user %player% parent add %group%"
  remove-command: "lp user %player% parent remove %group%"
  mappings:
    - mc-group: "vip"
      dc-role: "123456789012345678"
    - mc-group: "staff"
      dc-role: "987654321098765432"
```

> Works best with **LuckPerms**. For other permission plugins, configure the `add-command` and `remove-command` with the appropriate command syntax.

---

## 🔌 Developer API

myLink exposes a public API for other plugins to use.

### Dependency

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven("https://jitpack.io")
    }
}

// build.gradle.kts
dependencies {
    compileOnly("com.github.myplugins-mc:myLink:1.0.0-beta")
}
```

### Usage

```kotlin
// Get the API instance
val api = server.servicesManager.load(MyLinkAPI::class.java) ?: return

// Check if a player is linked
val linked = api.isLinked(player.uniqueId)

// Get the Discord ID of a linked player
val discordId = api.getDiscordId(player.uniqueId)

// Get the full linked account
val account = api.getLinkedAccount(player.uniqueId)

// Send a message to a configured channel
api.sendToChannel("chat-sync", "Hello from another plugin!")

// Check if the bot is ready
val botReady = api.isBotReady()
```

---

## 🏗️ Building from Source

```bash
git clone https://github.com/myplugins-mc/myLink.git
cd myLink
./gradlew shadowJar
```

The compiled `.jar` will be in `build/libs/`.

---

## 📄 License

myLink is licensed under the [Apache License 2.0](LICENSE).

```
Copyright 2026 myPlugins (myplugins.org)

Licensed under the Apache License, Version 2.0
```

---

<div align="center">

Made with 💜 by **[myPlugins](https://myplugins.org)**

</div>
