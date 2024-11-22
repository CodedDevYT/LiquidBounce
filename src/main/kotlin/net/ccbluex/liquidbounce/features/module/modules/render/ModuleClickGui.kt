/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClickGuiValueChangeEvent
import net.ccbluex.liquidbounce.event.events.ClickGuiScaleChangeEvent
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.VrScreen
import net.ccbluex.liquidbounce.integration.browser.supports.tab.ITab
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import org.lwjgl.glfw.GLFW

/**
 * ClickGUI module
 *
 * Shows you an easy-to-use menu to toggle and configure modules.
 */

object ModuleClickGui :
    Module("ClickGUI", Category.RENDER, bind = GLFW.GLFW_KEY_RIGHT_SHIFT, disableActivation = true) {

    @Suppress("UnusedPrivateProperty")
    private val scale by float("Scale", 1f, 0.5f..2f).onChanged {
        EventManager.callEvent(ClickGuiScaleChangeEvent(it))
        EventManager.callEvent(ClickGuiValueChangeEvent(this))
    }

    @Suppress("UnusedPrivateProperty")
    private val cache by boolean("Cache", true).onChanged { cache ->
        RenderSystem.recordRenderCall {
            if (cache) {
                createView()
            } else {
                closeView()
            }

            if (mc.currentScreen is VrScreen || mc.currentScreen is ClickScreen) {
                enable()
            }
        }
    }

    @Suppress("UnusedPrivateProperty")
    private val searchBarAutoFocus by boolean("SearchBarAutoFocus", true)

    object Snapping : ToggleableConfigurable(this, "Snapping", true) {

        @Suppress("UnusedPrivateProperty")
        private val gridSize by int("GridSize", 10, 1..100, "px").onChanged {
            EventManager.callEvent(ClickGuiValueChangeEvent(ModuleClickGui))
        }

        init {
            inner.find { it.name == "Enabled" }?.onChanged {
                EventManager.callEvent(ClickGuiValueChangeEvent(ModuleClickGui))
            }
        }
    }

    private var clickGuiTab: ITab? = null

    init {
        tree(Snapping)
    }

    override fun enable() {
        // Pretty sure we are not in a game, so we can't open the clickgui
        if (!inGame) {
            return
        }

        mc.setScreen(
            if (clickGuiTab == null) {
                VrScreen(VirtualScreenType.CLICK_GUI)
            } else {
                ClickScreen()
            }
        )
        super.enable()
    }

    /**
     * Creates the ClickGUI view
     */
    private fun createView() {
        if (clickGuiTab != null) {
            return
        }

        clickGuiTab = ThemeManager.openInputAwareImmediate(VirtualScreenType.CLICK_GUI, true) {
            mc.currentScreen is ClickScreen
        }.preferOnTop()
    }

    /**
     * Closes the ClickGUI view
     */
    private fun closeView() {
        clickGuiTab?.closeTab()
        clickGuiTab = null
    }

    /**
     * Restarts the ClickGUI view
     */
    fun restartView() {
        closeView()
        createView()
    }

    /**
     * Synchronizes the ClickGUI with the module values until there is a better solution
     * for updating setting changes
     */
    fun reloadView() {
        clickGuiTab?.reload()
    }

    @Suppress("unused")
    private val gameRenderHandler = handler<GameRenderEvent>(
        priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING,
        ignoreCondition = true
    ) {
        // A hack to prevent the clickgui from being drawn
        if (mc.currentScreen !is ClickScreen) {
            clickGuiTab?.drawn = true
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent>(
        priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING,
        ignoreCondition = true
    ) { event ->
        // When changing the world or disconnecting from a server,
        // close the ClickGUI to free resources
        if (event.world == null) {
            closeView()
        } else {
            createView()
        }
    }

    /**
     * An empty screen that acts as hint when to draw the clickgui
     */
    class ClickScreen : Screen("ClickGUI".asText()) {
        override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
            super.render(context, mouseX, mouseY, delta)
        }

        override fun shouldPause(): Boolean {
            // preventing game pause
            return false
        }
    }

}
