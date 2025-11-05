package dk.marcusrokatis.broadcastForwarder

import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.wrappers.WrappedChatComponent
import github.scarsz.discordsrv.DiscordSRV
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel
import net.kyori.adventure.text.logger.slf4j.ComponentLogger.logger
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

fun shouldForward(msg: String): Boolean {
    if (BroadcastForwarder.configuration.forwardAllBroadcasts) return true

    if (BroadcastForwarder.configuration.allowedPrefixes.any { msg.startsWith(it, true) }) return true

    return false
}

fun forwardToDiscord(message: String) {
    try {
        val channel: TextChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(BroadcastForwarder.configuration.forwardToChannel)
        channel.sendMessage(message).queue()
    } catch (t: Throwable) {
        BroadcastForwarder.INSTANCE.logger.warning("Could not send broadcast to Discord: ${t.message}")
    }
}

fun extractPlainText(packet: PacketContainer): String? {
    runCatching {
        val comp = packet.chatComponents.readSafely(0)
        if (comp != null) return comp.toPlain()
    }

    runCatching {
        val s = packet.strings.readSafely(0)
        if (!s.isNullOrBlank()) return s
    }

    runCatching {
        when (val data = packet.modifier.readSafely(0)) {
            is WrappedChatComponent -> return data.toPlain()
            is String -> if (data.isNotBlank()) return data
        }
    }

    return null
}

fun extractLegacyStyledText(packet: PacketContainer): String? {
    runCatching {
        val comp = packet.chatComponents.readSafely(0)
        if (comp != null) return comp.toLegacyAmpersand()
    }

    runCatching {
        val s = packet.strings.readSafely(0)
        if (!s.isNullOrBlank()) return s
    }

    runCatching {
        when (val data = packet.modifier.readSafely(0)) {
            is WrappedChatComponent -> return data.toLegacyAmpersand()
            is String -> if (data.isNotBlank()) return data
        }
    }

    return null
}

fun WrappedChatComponent.toPlain(): String {
    val json = this.json ?: return ""
    val component = GsonComponentSerializer.gson().deserialize(json)
    return PlainTextComponentSerializer.plainText().serialize(component).trim()
}

fun WrappedChatComponent.toLegacyAmpersand(): String {
    val json = this.json ?: return ""
    val component = GsonComponentSerializer.gson().deserialize(json)
    return LegacyComponentSerializer.legacyAmpersand().serialize(component).trim()
}