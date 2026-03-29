package dev.slne.surf.stats.paper.menu

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.stats.api.model.OptOutType
import dev.slne.surf.stats.core.client.service.OptOutStatisticService
import dev.slne.surf.stats.paper.plugin
import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.bukkit.api.builder.buildLore
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl.layout
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl.layoutSlot
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl.onItemClick
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.*
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.blockRow
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.ViewRows
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state.get
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state.initialState
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state.mutableState
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state.set
import dev.slne.surf.surfapi.core.api.font.toSmallCaps
import dev.slne.surf.surfapi.core.api.messages.adventure.playSound
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

const val CATEGORY_NAME_CUSTOM = "minecraft:custom"
const val STAT_NAME_PLAYTIME = "minecraft:play_time"

val statsOptOutView = surfView("optout") {
    val playtimeOptOutStateHolder = mutableState(false)
    val initialStateHolder = initialState<Boolean>("playtime-opt-in")

    settings {
        navigateBackOnOutsideClick(false)
        rows(ViewRows.THREE)
    }

    containerDefaults {
        blockRow(1)
        blockRow(2, exemptColumns = intArrayOf(4))
        blockRow(3)
    }

    onInit {
        layout {
            empty()
            row("    P    ")
            empty()
        }
    }

    onFirstRender {
        val initialState = initialStateHolder[this]
        playtimeOptOutStateHolder[this] = initialState

        layoutSlot('P') {
            updateOnClick()

            onRender {
                it.item = playtimeToggleItem(playtimeOptOutStateHolder[it])
            }

            onItemClick {
                val currentState = playtimeOptOutStateHolder[this]
                val newState = !currentState
                playtimeOptOutStateHolder[this] = newState

                player.playClickSound()

                player.sendText {
                    appendSuccessPrefix()
                    success("Du hast die Statistiken für deine Spielzeit nun ")
                    variableValue(if (newState) "aktiviert" else "deaktiviert")
                    success(".")
                }
            }
        }
    }

    onClose {
        val initialState = initialStateHolder[this]
        val state = playtimeOptOutStateHolder[this]

        if (initialState == state) {
            return@onClose
        }

        val type = if (state) OptOutType.IN else OptOutType.OUT
        plugin.launch {
            OptOutStatisticService.toggleOptOut(
                player.uniqueId,
                CATEGORY_NAME_CUSTOM,
                STAT_NAME_PLAYTIME,
                type
            )
        }
    }
}

private fun playtimeToggleItem(currentState: Boolean) = buildItem(Material.CLOCK) {
    displayName {
        localColored("Spielzeit-Statistik".toSmallCaps(), TextDecoration.BOLD)
    }

    buildLore {
        emptyLine()
        line {
            variableValue("Beschreibung:".toSmallCaps())
        }
        line {
            localColored("Legt fest, ob deine Spielzeit auf der")
        }
        line {
            localColored("Statistik-Webseite einsehbar ist.")
        }
        emptyLine()
        line {
            variableValue("Status:".toSmallCaps())
        }
        line {
            spacer("-")
            appendSpace()
            if (currentState) {
                success("Aktiviert".toSmallCaps())
            } else {
                error("Deaktiviert".toSmallCaps())
            }
        }
        emptyLine()
        line {
            appendSpace()
            variableValue("Klicke, um die Einstellung zu ändern.".toSmallCaps())
        }
    }
}

private fun Player.playClickSound() {
    this.playSound(true) {
        type(Sound.UI_BUTTON_CLICK)
    }
}

private fun SurfComponentBuilder.localColored(text: String, vararg decoration: TextDecoration) =
    text(text, TextColor.fromHexString("#00d492"), *decoration)