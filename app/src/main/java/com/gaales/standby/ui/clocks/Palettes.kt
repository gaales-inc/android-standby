package com.gaales.standby.ui.clocks

import androidx.compose.ui.graphics.Color
import com.gaales.standby.data.ClockStyle

/** A clock color option: primary is the main ink, secondary and tertiary support it. */
data class ClockPalette(val primary: Color, val secondary: Color = primary, val tertiary: Color = secondary)

object ClockPalettes {
    // Analog: the second hand and the weekday take the accent color.
    private val analog = listOf(
        0xFFFF9F0A, 0xFFFF453A, 0xFFFF375F, 0xFFBF5AF2, 0xFF5E5CE6, 0xFF0A84FF,
        0xFF64D2FF, 0xFF40C8E0, 0xFF63E6E2, 0xFF30D158, 0xFFFFD60A, 0xFFF2F2F7,
    ).map { ClockPalette(Color(it)) }

    // Digital: big numerals in the main color, the date and info line in a softer tone.
    private val digital = listOf(
        0xFFFF6961 to 0xFFFFB3AE,
        0xFFFF9F0A to 0xFFFFD08A,
        0xFFFFD60A to 0xFFFFEB8A,
        0xFF30D158 to 0xFF9BE9AE,
        0xFF63E6E2 to 0xFFB3F3F1,
        0xFF64D2FF to 0xFFB8E9FF,
        0xFF0A84FF to 0xFF8CC4FF,
        0xFF7D7AFF to 0xFFC2C0FF,
        0xFFBF5AF2 to 0xFFE2B6F8,
        0xFFFF375F to 0xFFFFA3B5,
        0xFFF2F2F7 to 0xFF98989D,
        0xFF98989D to 0xFF636366,
        0xFFFFB4A2 to 0xFFFFDDD4,
        0xFFC8B6FF to 0xFFE6DDFF,
        0xFFA0D8FF to 0xFFD6EEFF,
        0xFFC7F464 to 0xFFE6FAB5,
        0xFFAC8E68 to 0xFFD8C6AE,
        0xFFFF453A to 0xFFFF9F0A,
        0xFF0A84FF to 0xFF64D2FF,
        0xFFBF5AF2 to 0xFFFF375F,
        0xFF30D158 to 0xFFFFD60A,
        0xFF5E5CE6 to 0xFF63E6E2,
        0xFFFFFFFF to 0xFFFF9F0A,
        0xFFFFFFFF to 0xFF0A84FF,
    ).map { (a, b) -> ClockPalette(Color(a), Color(b)) }

    // Solar: core, glow and halo of the sun.
    private val solar = listOf(
        Triple(0xFFFFF4D6, 0xFFFF9F0A, 0xFFFF453A),
        Triple(0xFFFFFBE6, 0xFFFFD60A, 0xFFFF9F0A),
        Triple(0xFFFFE5EC, 0xFFFF6482, 0xFFBF5AF2),
        Triple(0xFFF1E5FF, 0xFFBF5AF2, 0xFF5E5CE6),
        Triple(0xFFE5F6FF, 0xFF64D2FF, 0xFF0A84FF),
        Triple(0xFFE8FFF6, 0xFF63E6E2, 0xFF30D158),
        Triple(0xFFFFFFFF, 0xFFAEAEB2, 0xFF636366),
    ).map { (a, b, c) -> ClockPalette(Color(a), Color(b), Color(c)) }

    // Float: each digit is a vertical gradient between two colors.
    private val float = listOf(
        0xFFFF6482 to 0xFFFF9F0A,
        0xFFFFD60A to 0xFFFF9F0A,
        0xFF64D2FF to 0xFF5E5CE6,
        0xFF30D158 to 0xFF63E6E2,
        0xFFBF5AF2 to 0xFFFF375F,
        0xFFF2F2F7 to 0xFFAEAEB2,
        0xFFFF9F0A to 0xFFFF453A,
        0xFFA0FFD6 to 0xFF64D2FF,
    ).map { (a, b) -> ClockPalette(Color(a), Color(b)) }

    // World comes in a single color, as on iOS.
    private val world = listOf(ClockPalette(Color.White, Color(0xFFFFD60A)))

    fun forStyle(style: ClockStyle): List<ClockPalette> = when (style) {
        ClockStyle.ANALOG -> analog
        ClockStyle.DIGITAL -> digital
        ClockStyle.WORLD -> world
        ClockStyle.SOLAR -> solar
        ClockStyle.FLOAT -> float
    }
}
