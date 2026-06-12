package llc.lookatwhataicando.codeoba.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateSurface, RoundedCornerShape(8.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AccentCyan.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun SourceDistributionRow(sourceId: String, count: Int, total: Int) {
    val badgeColors = getSourceBadgeColors(sourceId)
    val percentage = if (total > 0) (count.toFloat() / total * 100).toInt() else 0

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(badgeColors.first, RoundedCornerShape(2.dp))
                )
                Text(
                    text = formatSourceDisplayName(sourceId),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 12.sp
                )
            }
            Text(
                text = "$count ${if (count == 1) "session" else "sessions"} ($percentage%)",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 12.sp
            )
        }
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(SlateSurface.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(count.toFloat() / total)
                    .height(8.dp)
                    .background(badgeColors.first, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun SessionStatBadge(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SlateSurface.copy(alpha = 0.5f))
            .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 11.sp
        )
    }
}
