package dev.slne.surf.stats.paper.menu

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.stats.api.model.OptOutType
import dev.slne.surf.stats.core.client.service.OptOutStatisticService
import dev.slne.surf.stats.paper.plugin
import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.bukkit.api.builder.buildLore
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.outlineItem
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.titleBuilder
import dev.slne.surf.surfapi.core.api.font.toSmallCaps
import dev.slne.surf.surfapi.core.api.messages.adventure.key
import dev.slne.surf.surfapi.core.api.messages.adventure.playSound
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.state.MutableState
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

private const val CATEGORY_NAME_CUSTOM = "minecraft:custom"
private const val STAT_NAME_PLAYTIME = "minecraft:play_time"

object StatsOptOutView : View() {

    private val playtimeEnabledState: MutableState<Boolean> = mutableState(true)

    override fun onInit(config: ViewConfigBuilder) {
        config
            .titleBuilder {
                localColored("Stats Optout".toSmallCaps(), TextDecoration.BOLD)
            }
            .size(5)
            .layout(
                "OOOOOOOOO",
                "O       O",
                "O   P   O",
                "O       O",
                "OOOOOOOOO"
            )
            .cancelInteractions()
    }

    override fun onFirstRender(render: RenderContext) {
        plugin.launch {
            render.layoutSlot('O', outlineItem)

            val player = render.player
            val optOuts = OptOutStatisticService.getOptOutput(player.uniqueId)
            val isEnabled = optOuts.none {
                it.categoryName == key(CATEGORY_NAME_CUSTOM) && it.statisticName == key(STAT_NAME_PLAYTIME)
            }

            playtimeEnabledState.set(isEnabled, render)

            render.layoutSlot('P')
                .updateOnStateChange(playtimeEnabledState)
                .onRender { slotRender ->
                    val currentState = playtimeEnabledState.get(slotRender)
                    slotRender.item = playtimeToggleItem(currentState)
                }
                .onClick { context ->
                    val currentState = playtimeEnabledState.get(context)
                    val newState = !currentState

                    playtimeEnabledState.set(newState, context)
                    context.player.playClickSound()

                    val type = if (newState) OptOutType.OFF else OptOutType.ON

                    plugin.launch {
                        OptOutStatisticService.toggleOptOut(
                            context.player.uniqueId,
                            CATEGORY_NAME_CUSTOM,
                            STAT_NAME_PLAYTIME,
                            type
                        )
                        context.player.sendText {
                            appendSuccessPrefix()
                            success("Du hast die Statistiken für deine Spielzeit nun ")
                            variableValue(if (newState) "aktiviert" else "deaktiviert")
                            success(".")
                        }
                    }
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
}