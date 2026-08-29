package com.shiki.vocabulary.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF18231F)
val Muted = Color(0xFF60706A)
val Paper = Color(0xFFF5F3ED)
val Card = Color(0xFFFFFDF8)
val Forest = Color(0xFF174F3B)
val ForestSecondary = Color(0xFF246B51)
val Mint = Color(0xFFDCECDF)
val Sun = Color(0xFFF3B64C)
val Coral = Color(0xFFDF765F)
val Correct = Color(0xFF26704C)
val CorrectBackground = Color(0xFFDFF2E6)
val Wrong = Color(0xFFB75241)
val WrongBackground = Color(0xFFFFE7E1)

val StageColors = listOf(
    Color(0xFF276F55),
    Color(0xFFA15E48),
    Color(0xFF365D8A),
    Color(0xFF8A6740),
    Color(0xFF73538C),
)

private val VocabularyColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    secondary = ForestSecondary,
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDF1ED),
    onSurfaceVariant = Muted,
    outline = Color(0xFFD2D9D4),
    error = Wrong,
)

@Composable
fun VocabularyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VocabularyColors,
        typography = Typography(),
        content = content,
    )
}
