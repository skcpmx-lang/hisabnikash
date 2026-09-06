package com.hisabnikash.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * One spacing scale for the whole product.
 *
 * Every margin, gap and section offset should be one of these values.
 * Using a single scale is what makes the UI feel deliberate and
 * professional instead of a random collection of paddings.
 */
object Spacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 20.dp
    val Xxl = 24.dp
    val Xxxl = 32.dp

    /** Standard screen margin used by headers, cards and lists. */
    val ScreenMargin = Lg

    /** Standard gap between related cards / rows of cards. */
    val CardGap = Md

    /** Standard vertical gap between sections. */
    val SectionGap = Xxl
}
