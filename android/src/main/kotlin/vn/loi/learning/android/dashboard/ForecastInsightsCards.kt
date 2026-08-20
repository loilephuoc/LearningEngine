package vn.loi.learning.android.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.loi.learning.android.ui.LearningSpacing
import vn.loi.learning.android.ui.StudyRatingColors

@Composable
fun InsightsScopeSelector(
    currentScope: AndroidInsightsScope,
    availableScopes: List<AndroidInsightsScopeOption>,
    onScopeChange: (AndroidInsightsScope) -> Unit,
    modifier: Modifier = Modifier
) {
    if (availableScopes.size <= 1) return

    var expanded by remember { mutableStateOf(false) }

    val currentLabel = when (currentScope) {
        is AndroidInsightsScope.AllPackages -> "All packages"
        is AndroidInsightsScope.SpecificPackage -> currentScope.packageName
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Insights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() }
        )

        Box {
            FilterChip(
                selected = currentScope !is AndroidInsightsScope.AllPackages,
                onClick = { expanded = true },
                label = {
                    Text(
                        text = currentLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .defaultMinSize(minHeight = LearningSpacing.touchTarget)
                    .semantics {
                        contentDescription = "Insights scope, $currentLabel"
                        stateDescription = if (expanded) "Expanded" else "Collapsed"
                    }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableScopes.forEach { option ->
                    val isSelected = option.scope == currentScope
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (option.isActivePackage) {
                                    Text(
                                        text = "· Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            expanded = false
                            onScopeChange(option.scope)
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewForecastCard(
    forecast: List<ForecastDayBucket>,
    modifier: Modifier = Modifier
) {
    val totalDue7Days = forecast.sumOf { it.count }
    val maxCount = forecast.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(LearningSpacing.large),
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Review forecast",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        if (totalDue7Days > 0) "$totalDue7Days cards due in next 7 days" else "No upcoming reviews in next 7 days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "7-day review forecast: $totalDue7Days cards total"
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                forecast.forEach { bucket ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = "${bucket.label} (${bucket.date.dayOfMonth}/${bucket.date.monthValue}): ${bucket.count} cards due"
                        }
                    ) {
                        Text(
                            text = bucket.count.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (bucket.count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val fillFraction = (bucket.count.toFloat() / maxCount).coerceIn(0f, 1f)
                        val barHeight = (48 * fillFraction).dp.coerceAtLeast(if (bucket.count > 0) 4.dp else 0.dp)

                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(48.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (bucket.count > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }

                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "${bucket.date.dayOfMonth}/${bucket.date.monthValue}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryRetentionCard(
    distribution: MemoryDistributionInsights,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(LearningSpacing.large),
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
            ) {
                Icon(
                    Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Memory retention",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        "${distribution.totalCount} total memories in scope",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Multi-segment progress bar
            if (distribution.totalCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (distribution.newPercent > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(distribution.newPercent)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                    if (distribution.learningPercent > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(distribution.learningPercent)
                                .fillMaxHeight()
                                .background(StudyRatingColors.again.border)
                        )
                    }
                    if (distribution.youngPercent > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(distribution.youngPercent)
                                .fillMaxHeight()
                                .background(StudyRatingColors.hard.border)
                        )
                    }
                    if (distribution.retainedPercent > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(distribution.retainedPercent)
                                .fillMaxHeight()
                                .background(StudyRatingColors.good.border)
                        )
                    }
                }
            }

            // 4 breakdown items
            Column(verticalArrangement = Arrangement.spacedBy(LearningSpacing.extraSmall)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
                ) {
                    DistributionTile(
                        label = "New",
                        count = distribution.newCount,
                        percent = distribution.newPercent,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.weight(1f)
                    )
                    DistributionTile(
                        label = "Learning",
                        count = distribution.learningCount,
                        percent = distribution.learningPercent,
                        color = StudyRatingColors.again.border,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
                ) {
                    DistributionTile(
                        label = "Young (≤21d)",
                        count = distribution.youngCount,
                        percent = distribution.youngPercent,
                        color = StudyRatingColors.hard.border,
                        modifier = Modifier.weight(1f)
                    )
                    DistributionTile(
                        label = "Retained (>21d)",
                        count = distribution.retainedCount,
                        percent = distribution.retainedPercent,
                        color = StudyRatingColors.good.border,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DistributionTile(
    label: String,
    count: Int,
    percent: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$count (${(percent * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TodayRatingsCard(
    ratings: TodayRatingsInsights,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(LearningSpacing.large),
            verticalArrangement = Arrangement.spacedBy(LearningSpacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
            ) {
                Icon(
                    Icons.Default.HistoryEdu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Today's ratings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        if (ratings.totalCount > 0) "${ratings.totalCount} reviews completed today" else "No reviews completed yet today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LearningSpacing.small)
            ) {
                RatingTile(
                    label = "Again",
                    count = ratings.againCount,
                    background = StudyRatingColors.again.background.copy(alpha = 0.5f),
                    border = StudyRatingColors.again.border,
                    contentColor = StudyRatingColors.again.content,
                    modifier = Modifier.weight(1f)
                )
                RatingTile(
                    label = "Hard",
                    count = ratings.hardCount,
                    background = StudyRatingColors.hard.background.copy(alpha = 0.5f),
                    border = StudyRatingColors.hard.border,
                    contentColor = StudyRatingColors.hard.content,
                    modifier = Modifier.weight(1f)
                )
                RatingTile(
                    label = "Good",
                    count = ratings.goodCount,
                    background = StudyRatingColors.good.background.copy(alpha = 0.5f),
                    border = StudyRatingColors.good.border,
                    contentColor = StudyRatingColors.good.content,
                    modifier = Modifier.weight(1f)
                )
                RatingTile(
                    label = "Easy",
                    count = ratings.easyCount,
                    background = StudyRatingColors.easy.background.copy(alpha = 0.5f),
                    border = StudyRatingColors.easy.border,
                    contentColor = StudyRatingColors.easy.content,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RatingTile(
    label: String,
    count: Int,
    background: Color,
    border: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(1.dp, border, MaterialTheme.shapes.medium)
            .semantics { contentDescription = "$label: $count reviews today" },
        shape = MaterialTheme.shapes.medium,
        color = background
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}
