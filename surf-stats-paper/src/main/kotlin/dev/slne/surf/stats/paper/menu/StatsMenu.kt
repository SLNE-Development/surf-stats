package dev.slne.surf.stats.paper.menu

import com.github.shynixn.mccoroutine.folia.launch
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.pane.component.ToggleButton
import dev.slne.surf.stats.api.model.OptOutType
import dev.slne.surf.stats.core.client.service.OptOutStatisticService
import dev.slne.surf.stats.paper.plugin
import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.bukkit.api.builder.buildLore
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.menu
import dev.slne.surf.surfapi.bukkit.api.inventory.types.SurfChestGui
import dev.slne.surf.surfapi.core.api.font.toSmallCaps
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import dev.slne.surf.surfapi.core.api.messages.adventure.key
import dev.slne.surf.surfapi.core.api.messages.adventure.playSound
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.HumanEntity

private const val height = 5
private const val width = 9

private const val CATEGORY_NAME_CUSTOM = "minecraft:custom"
private const val STAT_NAME_PLAYTIME = "minecraft:play_time"

fun openStatsSettingsMenu(player: HumanEntity): SurfChestGui =
    menu(buildText { spacer("Stats Optout") }, height) {

        withOutline(width, height)
        withOutClicks()

        val optOuts = OptOutStatisticService.getOptOutput(player.uniqueId)

        var isPlaytimeEnabled = optOuts.none {
            it.categoryName == key(CATEGORY_NAME_CUSTOM) && it.statisticName == key(STAT_NAME_PLAYTIME)
        }
        val originalPlaytimeEnabled = isPlaytimeEnabled

        addPane(
            ToggleButton(
                2, 2, 1, 1, isPlaytimeEnabled
            ).apply {
                setDisabledItem(GuiItem(playtimeToggleItem(false)) {
                    isPlaytimeEnabled = true
                    it.whoClicked.sendText {
                        appendSuccessPrefix()
                        success("Du hast die Statistiken für deine Spielzeit nun ")
                        variableValue("aktiviert")
                        success(".")
                    }
                    it.whoClicked.playClickSound()
                })

                setEnabledItem(GuiItem(playtimeToggleItem(true)) {
                    isPlaytimeEnabled = false
                    it.whoClicked.sendText {
                        appendSuccessPrefix()
                        success("Du hast die Statistiken für deine Spielzeit nun ")
                        variableValue("deaktiviert")
                        success(".")
                    }
                    it.whoClicked.playClickSound()
                })
            })

        show(player)

        setOnClose {
            plugin.launch {
                if (isPlaytimeEnabled != originalPlaytimeEnabled) {

                    val type = if (isPlaytimeEnabled) {
                        OptOutType.OFF
                    } else {
                        OptOutType.ON
                    }

                    OptOutStatisticService.toggleOptOut(
                        it.player.uniqueId,
                        CATEGORY_NAME_CUSTOM,
                        STAT_NAME_PLAYTIME,
                        type
                    )
                }
            }
        }
    }


private fun HumanEntity.playClickSound() {
    this.playSound(true) {
        type(Sound.UI_BUTTON_CLICK)
    }
}

private fun playtimeToggleItem(currentState: Boolean) = buildItem(Material.CLOCK) {
    displayName {
        localColored("Chat Pings".toSmallCaps(), TextDecoration.BOLD)
    }

    buildLore {
        emptyLine()
        line {
            variableValue("Beschreibung:".toSmallCaps())
        }

        line {
            localColored("Legt fest, ob deine Spielzeit auf der Statistik-Webseite einsehbar ist.")
        }

        emptyLine()
        line {
            variableValue("Status:".toSmallCaps())
        }

        line {
            spacer("-")
            appendSpace()
            localColored(if (currentState) "Aktiviert" else "Deaktiviert")
        }

        emptyLine()

        line {
            spacer("Klicke, um die Einstellung zu ändern")
        }
    }
}

private fun SurfComponentBuilder.localColored(text: Any, vararg decoration: TextDecoration) =
    text(text.toString(), TextColor.fromHexString("#00d492"), *decoration)