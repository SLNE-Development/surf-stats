package dev.slne.surf.stats.paper.menu

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import org.bukkit.Material

private val borderItem = GuiItem(buildItem(Material.GRAY_STAINED_GLASS_PANE) {
    displayName {
        text(" ")
    }
})

fun ChestGui.withOutline(width: Int, height: Int) = apply {
    addPane(StaticPane(0, 0, width, height).apply {
        for (y in 1 until height - 1) {
            addItem(borderItem, 0, y)
            addItem(borderItem, width - 1, y)
        }

        for (x in 0 until width) {
            addItem(borderItem, x, 0)
            addItem(borderItem, x, height - 1)
        }
    })
}


fun ChestGui.withOutClicks() = apply {
    setOnGlobalClick { it.isCancelled = true }
    setOnGlobalDrag { it.isCancelled = true }
}