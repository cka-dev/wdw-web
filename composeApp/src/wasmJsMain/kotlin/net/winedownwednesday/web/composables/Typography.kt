package net.winedownwednesday.web.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import wdw_web.composeapp.generated.resources.CormorantGaramond_Bold
import wdw_web.composeapp.generated.resources.CormorantGaramond_Regular
import wdw_web.composeapp.generated.resources.Res

@Composable
fun displayFontFamily() = FontFamily(
    Font(Res.font.CormorantGaramond_Regular, weight = FontWeight.Normal),
    Font(Res.font.CormorantGaramond_Bold,    weight = FontWeight.Bold),
)
