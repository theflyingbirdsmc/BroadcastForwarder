package dk.marcusrokatis.broadcastForwarder

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class BroadcastForwarder : JavaPlugin() {

    private val recentMessages = ConcurrentHashMap<String, Long>()
    private val messageCooldownMs = 400L

    init {
        INSTANCE = this
    }

    override fun onEnable() {
        try {
            configuration = PluginConfig(INSTANCE)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(
            object : PacketAdapter(
                params()
                    .plugin(INSTANCE)
                    .serverSide()
                    .types(PacketType.Play.Server.SYSTEM_CHAT)
            ) {
                override fun onPacketSending(event: PacketEvent) {
                    val packet = event.packet

                    val legacy = extractLegacyStyledText(packet) ?: return
                    if (legacy.isBlank()) return

                    val plain = extractPlainText(packet) ?: return
                    if (plain.isBlank()) return

                    if (!shouldForward(legacy)) return
                    if (!dedup(plain)) return

                    forwardToDiscord(plain)
                }
            }
        )

        logger.info("BroadcastForwarder enabled ✅")
    }

    override fun onDisable() {
        logger.info("BroadcastForwarder disabled 🚫")
    }

    private fun registerEvents() {

        val eventHandlers: Array<Listener> = emptyArray()

        eventHandlers.forEach { server.pluginManager.registerEvents(it, this) }
    }

    private fun dedup(text: String): Boolean {
        val now = System.currentTimeMillis()
        recentMessages.entries.removeIf { now - it.value > messageCooldownMs }
        val prev = recentMessages.putIfAbsent(text, now)
        return prev == null || (now - prev) > messageCooldownMs
    }

    companion object {

        @JvmStatic
        lateinit var INSTANCE: BroadcastForwarder
        @JvmStatic
        lateinit var configuration: PluginConfig
    }
}
