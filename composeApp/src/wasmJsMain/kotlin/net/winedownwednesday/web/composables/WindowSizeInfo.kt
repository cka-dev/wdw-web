package net.winedownwednesday.web.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Width breakpoints (matches Material3 Adaptive + extended large / xlarge)
// ---------------------------------------------------------------------------
enum class WidthClass {
    /** < 600dp  — phones portrait */
    Compact,
    /** 600–839dp — portrait tablets, large unfolded inner displays portrait */
    Medium,
    /** 840–1199dp — landscape tablets, large unfolded inner displays landscape */
    Expanded,
    /** 1200–1599dp — large tablet / desktop displays */
    Large,
    /** ≥ 1600dp — extra-large desktop / TV */
    XLarge
}

// ---------------------------------------------------------------------------
// Height breakpoints (standard Material3)
// ---------------------------------------------------------------------------
enum class HeightClass {
    /** < 480dp  — phones landscape */
    Compact,
    /** 480–899dp — most phones portrait, tablets landscape */
    Medium,
    /** ≥ 900dp  — tablets portrait */
    Expanded
}

// ---------------------------------------------------------------------------
// WindowSizeInfo — single source of truth for all responsive decisions
// ---------------------------------------------------------------------------
data class WindowSizeInfo(
    val widthClass:  WidthClass,
    val heightClass: HeightClass
) {
    val isCompact:  Boolean get() = widthClass == WidthClass.Compact
    val isMedium:   Boolean get() = widthClass == WidthClass.Medium
    val isExpanded: Boolean get() = widthClass == WidthClass.Expanded
    val isLarge:    Boolean get() = widthClass == WidthClass.Large
    val isXLarge:   Boolean get() = widthClass == WidthClass.XLarge

    // ---- Navigation groupings ----

    /**
     * Compact + Medium → bottom-bar + hamburger-drawer (phone-style nav)
     */
    val useCompactNav: Boolean get() =
        widthClass == WidthClass.Compact || widthClass == WidthClass.Medium

    /**
     * Expanded, Large, XLarge → full horizontal top nav bar
     */
    val useWideNav: Boolean get() = !useCompactNav

    // ---- Content groupings ----

    /** Side-by-side two-column layout available from Expanded upward */
    val useTwoColumnLayout: Boolean get() =
        widthClass == WidthClass.Expanded ||
        widthClass == WidthClass.Large    ||
        widthClass == WidthClass.XLarge

    // ---- Spacing helpers ----

    /** Horizontal content padding that scales with width class */
    val horizontalPadding: Dp get() = when (widthClass) {
        WidthClass.Compact  -> 16.dp
        WidthClass.Medium   -> 24.dp
        WidthClass.Expanded -> 48.dp
        WidthClass.Large    -> 80.dp
        WidthClass.XLarge   -> 120.dp
    }

    /**
     * Maximum width for content columns so ultra-wide screens don't stretch
     * everything edge-to-edge. Use `Dp.Unspecified` to mean "no cap" (Compact).
     */
    val maxContentWidth: Dp get() = when (widthClass) {
        WidthClass.Compact  -> Dp.Unspecified
        WidthClass.Medium   -> 720.dp
        WidthClass.Expanded -> 1100.dp
        WidthClass.Large    -> 1400.dp
        WidthClass.XLarge   -> 1600.dp
    }
}

// ---------------------------------------------------------------------------
// Composable factory — pure dp-based calculation, no external library needed.
// Both width (5 classes incl. Large/XLarge) and height follow Material3 specs.
// The adaptive:1.1.0 library is used for layout composables (Phase 3).
// ---------------------------------------------------------------------------
@Composable
fun rememberWindowSizeInfo(): WindowSizeInfo {
    val windowInfo = LocalWindowInfo.current
    val density    = LocalDensity.current

    return remember(windowInfo.containerSize) {
        val widthDp  = with(density) { windowInfo.containerSize.width.toDp() }
        val heightDp = with(density) { windowInfo.containerSize.height.toDp() }

        val widthClass = when {
            widthDp <  600.dp  -> WidthClass.Compact
            widthDp <  840.dp  -> WidthClass.Medium
            widthDp < 1200.dp  -> WidthClass.Expanded
            widthDp < 1600.dp  -> WidthClass.Large
            else               -> WidthClass.XLarge
        }

        val heightClass = when {
            heightDp < 480.dp -> HeightClass.Compact
            heightDp < 900.dp -> HeightClass.Medium
            else              -> HeightClass.Expanded
        }

        WindowSizeInfo(widthClass = widthClass, heightClass = heightClass)
    }
}
