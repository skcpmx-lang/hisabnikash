package com.hisabnikash.app.ui.theme

import androidx.compose.ui.graphics.Color

// ===========================================================================
// HisabNikash design tokens — "Warm Ledger" identity.
//
// A calm, premium commerce OS palette: warm ivory base, deep navy/charcoal
// ink, a sophisticated deep-emerald primary, and a restrained amber used ONLY
// for financial highlights. No neon, no rainbow, no saturated green flood.
// Light mode only by design.
// ===========================================================================

// Base surfaces (tonal layering: background -> surface -> tint -> alt)
val Background = Color(0xFFF7F4ED)      // warm ivory app background
val Surface = Color(0xFFFFFFFF)         // primary surface
val SurfaceTint = Color(0xFFF0ECE2)     // secondary surface
val SurfaceAlt = Color(0xFFE9E3D6)      // tertiary / sunken surface

// Ink hierarchy (deep navy-charcoal, never pure black)
val Ink = Color(0xFF1C2530)
val InkSoft = Color(0xFF48545F)
val InkFaint = Color(0xFF7C8794)

// Structure
val OutlineSoft = Color(0xFFDCD5C7)     // warm stone borders
val SurfaceStroke = Color(0xFFE4DFD3)

// Brand
val BrandGreen = Color(0xFF0F6B54)      // deep sophisticated emerald (primary)
val BrandGreenDark = Color(0xFF0A4F3F)
val BrandGreenDeep = Color(0xFF07392F)
val BrandGreenSoft = Color(0xFFE4EFE9)  // soft emerald tint (selected/highlight)

// Financial accent — restrained amber, never neon
val BrandGold = Color(0xFFB8862B)
val BrandGoldSoft = Color(0xFFF4ECDB)

// Semantic states (muted, professional)
val Success = Color(0xFF1F7B52)
val Warning = Color(0xFFA37A1C)
val Error = Color(0xFFB5452F)
val Info = Color(0xFF3E6C9B)

// Chart series: emerald -> slate -> amber -> violet -> teal -> rose -> stone -> bronze.
// Muted enough to remain calm; distinct enough to be readable.
val ChartSeries = listOf(
    BrandGreen,
    Color(0xFF54749E),
    BrandGold,
    Color(0xFF8A6FA8),
    Color(0xFF5B978F),
    Color(0xFFC57A8C),
    Color(0xFF7E9387),
    Color(0xFFA9765A)
)
