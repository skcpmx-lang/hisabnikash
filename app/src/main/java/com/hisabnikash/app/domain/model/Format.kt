package com.hisabnikash.app.domain.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val moneyFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

/**
 * Formats minor units as a readable money string. BDT is displayed without
 * forced decimals; full values always include at least two decimals.
 */
fun formatMoney(minor: Long, symbol: String = "\u09F3"): String {
    val whole = minor / MoneyScale.SCALE
    val cents = (minor % MoneyScale.SCALE).toInt()
    val text = if (cents == 0) moneyFormat.format(whole) else {
        val sign = if (whole < 0) "-" else ""
        String.format("%s%s.%02d", sign, DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
            .format(kotlin.math.abs(whole)), cents)
    }
    return "$symbol$text"
}

fun formatMoneyPlain(minor: Long): String {
    val whole = minor / MoneyScale.SCALE
    val cents = (minor % MoneyScale.SCALE).toInt()
    return if (cents == 0) DecimalFormat("#,##0").format(whole)
    else String.format("%s.%02d", DecimalFormat("#,##0").format(whole), kotlin.math.abs(cents))
}

fun formatMoneyExact(minor: Long): String =
    String.format(
        "%s.%02d",
        DecimalFormat("#,##0").format(minor / MoneyScale.SCALE),
        kotlin.math.abs(minor % MoneyScale.SCALE)
    )

/** Parses user input like "1,234.50" or "1234" into minor units. Returns null for invalid input. */
fun parseMoneyInput(raw: String): Long? {
    val trimmed = raw.trim().replace(",", "").replace("\u09F3", "").trim()
    if (trimmed.isEmpty()) return null
    return try {
        val value = trimmed.toBigDecimal()
        (value * BigDecimal100).toLong()
    } catch (_: Exception) {
        null
    }
}

private val BigDecimal100 = java.math.BigDecimal(100)

fun formatPercent(bps: Int): String =
    String.format(Locale.US, "%.2f%%", bps / 100.0)

fun formatBps(bps: Int): String = formatPercent(bps)

fun formatDate(epochMillis: Long): String {
    val formatter = java.text.SimpleDateFormat("d MMM yyyy", Locale.US)
    return formatter.format(java.util.Date(epochMillis))
}

fun formatDateTime(epochMillis: Long): String {
    val formatter = java.text.SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US)
    return formatter.format(java.util.Date(epochMillis))
}

fun formatTime(epochMillis: Long): String {
    val formatter = java.text.SimpleDateFormat("h:mm a", Locale.US)
    return formatter.format(java.util.Date(epochMillis))
}

fun formatDay(epochMillis: Long): String {
    val formatter = java.text.SimpleDateFormat("EEE, d MMM", Locale.US)
    return formatter.format(java.util.Date(epochMillis))
}
