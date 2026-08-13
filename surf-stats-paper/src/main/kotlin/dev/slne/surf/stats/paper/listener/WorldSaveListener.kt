package dev.slne.surf.stats.paper.listener

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.stats.paper.plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldSaveEvent
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.seconds

/**
 * Syncs advancements whenever the server saves.
 *
 * Minecraft writes player advancement files as part of its save cycle, so this
 * is the point at which the files on disk are fresh.
 *
 * [WorldSaveEvent] fires once per world, so a server with three worlds fires
 * three times per autosave. Events arriving within [DEBOUNCE] of the last
 * handled one are ignored.
 */
object WorldSaveListener : Listener {
    private val DEBOUNCE = 30.seconds.inWholeNanoseconds

    private val lastRun = AtomicLong(0)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onWorldSave(event: WorldSaveEvent) {
        val now = System.nanoTime()
        val previous = lastRun.get()

        if (now - previous < DEBOUNCE) {
            return
        }

        if (!lastRun.compareAndSet(previous, now)) {
            return
        }

        plugin.launch {
            plugin.saveTrackedPlayerAdvancements()
        }
    }
}
