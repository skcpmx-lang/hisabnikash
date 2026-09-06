package com.hisabnikash.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.ui.theme.BrandGold
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.BrandGreenDeep
import com.hisabnikash.app.ui.theme.BrandGreenSoft
import com.hisabnikash.app.ui.theme.Error
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.ui.theme.Success
import com.hisabnikash.app.ui.theme.Warning

@Composable
fun StatusChip(text: String, color: Color = BrandGreen) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun orderStatusColor(status: String): Color = when (status) {
    "DRAFT" -> InkFaint
    "CONFIRMED" -> Color(0xFF4A7DBB)
    "PROCESSING" -> Warning
    "PACKED" -> Color(0xFF7A5CA0)
    "SHIPPED" -> Color(0xFF2C6EA5)
    "DELIVERED" -> Success
    "RETURNED" -> Color(0xFF8A6D3B)
    "CANCELLED" -> Error
    else -> InkFaint
}

@Composable
fun FilterChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) BrandGreen else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) BrandGreen else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clickable { onSelect(option) }
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
fun QuickActionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(BrandGreenSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BrandGreen)
            }
            Spacer(Modifier.size(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}

/**
 * In-app brand mark. Uses the same abstract flow glyph as the launcher icon
 * so the identity stays consistent everywhere.
 */
@Composable
fun BrandMark(size: androidx.compose.ui.unit.Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(BrandGreenDeep, RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(com.hisabnikash.app.R.drawable.ic_brand_mark),
            contentDescription = "HisabNikash",
            tint = Color.Unspecified,
            modifier = Modifier.size(size * 0.72f)
        )
    }
}

@Composable
fun PeriodSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChips(
        options = listOf("1D", "7D", "10D", "30D", "90D", "1Y", "Custom"),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier
    )
}

@Composable
fun LabelValueRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onBackground) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkFaint, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun IntroRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(8.dp).background(BrandGold, CircleShape)
        )
        Text(
            "  $label",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
