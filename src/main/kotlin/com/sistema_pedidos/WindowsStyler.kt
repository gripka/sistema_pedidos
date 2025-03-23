package com.sistema_pedidos.util

import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.win32.W32APIOptions
import javafx.application.Platform
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.util.concurrent.CountDownLatch

interface DwmApi : com.sun.jna.Library {
    companion object {
        val INSTANCE = Native.load("dwmapi", DwmApi::class.java, W32APIOptions.DEFAULT_OPTIONS)
        val DARK_MODE_COLOR: Int = 0xFF312D2B.toInt()
    }

    fun DwmSetWindowAttribute(hwnd: HWND, dwAttribute: Int, pvAttribute: IntArray, cbAttribute: Int): Int
}

class WindowsStyler {
    companion object {
        // Windows 11 22H2+
        private const val DWMWA_CAPTION_COLOR = 35

        // Windows 10 1809+
        private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20

        // Windows 10 pre-1809
        private const val DWMWA_USE_IMMERSIVE_DARK_MODE_OLD = 19

        fun setTitleBarColor(stage: Stage, color: Int) {
            Platform.runLater {
                if (!stage.isShowing) {
                    val showListener = object : javafx.beans.value.ChangeListener<Boolean> {
                        override fun changed(observable: javafx.beans.value.ObservableValue<out Boolean>, oldValue: Boolean, newValue: Boolean) {
                            if (newValue) {
                                applyStyling(stage, color)
                                stage.showingProperty().removeListener(this)
                            }
                        }
                    }
                    stage.showingProperty().addListener(showListener)
                } else {
                    applyStyling(stage, color)
                }
            }
        }

        fun applyStyling(stage: Stage, color: Int) {
            try {
                val hWnd = getWindowHandle(stage)
                if (hWnd != null) {
                    println("Window handle found, applying styling...")

                    val darkModeResult = setDarkMode(hWnd)
                    val colorResult = setSpecificTitleBarColor(hWnd, color)

                    println("Applied dark mode: $darkModeResult, Applied color: $colorResult")
                } else {
                    println("Failed to get window handle")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun setSpecificTitleBarColor(hWnd: HWND, color: Int): Boolean {
            try {
                val colorArray = intArrayOf(color)
                val result = DwmApi.INSTANCE.DwmSetWindowAttribute(
                    hWnd,
                    DWMWA_CAPTION_COLOR,
                    colorArray,
                    4
                )
                println("Set specific color result: $result (0 means success)")
                return result == 0
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        private fun setDarkMode(hWnd: HWND): Boolean {
            // Try Windows 10 1809+
            try {
                val darkModeFlag = intArrayOf(1) // 1 = true
                val result = DwmApi.INSTANCE.DwmSetWindowAttribute(
                    hWnd,
                    DWMWA_USE_IMMERSIVE_DARK_MODE,
                    darkModeFlag,
                    4
                )

                if (result == 0) {
                    println("Dark mode enabled with attribute 20")
                    return true
                }

                // If that fails, try pre-1809 approach
                val oldResult = DwmApi.INSTANCE.DwmSetWindowAttribute(
                    hWnd,
                    DWMWA_USE_IMMERSIVE_DARK_MODE_OLD,
                    darkModeFlag,
                    4
                )

                println("Dark mode with attribute 19 result: $oldResult")
                return oldResult == 0
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        // Add this method to WindowsStyler companion object
        fun applyStyleBeforeShowing(stage: Stage, color: Int): Boolean {
            // Use the right BGR format for #2B2D31
            val correctColor = 0x00312D2B // BGR format

            try {
                // Stage must be shown to get window handle
                // So we need to make a one-time call
                val temporaryStage = Stage()
                temporaryStage.initStyle(StageStyle.UTILITY)
                temporaryStage.opacity = 0.0
                temporaryStage.show()

                // Get HWND for temporary stage
                val hWnd = getWindowHandle(stage)
                if (hWnd != null) {
                    // Apply all styles at once before showing main window
                    val darkModeResult = setDarkMode(hWnd)
                    val colorResult = setSpecificTitleBarColor(hWnd, correctColor)
                    println("Pre-applied style results - Dark mode: $darkModeResult, Color: $colorResult")
                    temporaryStage.close()
                    return true
                }
                temporaryStage.close()
                return false
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        private fun getWindowHandle(stage: Stage): HWND? {
            try {
                val windowTitle = stage.title
                val hWnd = User32.INSTANCE.FindWindow(null, windowTitle)
                println("Finding window '$windowTitle': ${hWnd != null}")
                return hWnd
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }
}