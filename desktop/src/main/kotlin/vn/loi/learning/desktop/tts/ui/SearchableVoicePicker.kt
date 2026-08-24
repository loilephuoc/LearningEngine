package vn.loi.learning.desktop.tts.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import vn.loi.learning.desktop.tts.TtsVoice
import vn.loi.learning.desktop.ui.designsystem.LEColors
import vn.loi.learning.desktop.ui.designsystem.LEIcons
import vn.loi.learning.desktop.ui.designsystem.LERadius
import vn.loi.learning.desktop.ui.designsystem.LESpacing
import vn.loi.learning.desktop.ui.designsystem.LETypography
import vn.loi.learning.desktop.ui.designsystem.components.LEPrimaryButton
import vn.loi.learning.desktop.ui.designsystem.components.LESecondaryButton

/**
 * Metadata descriptor for region/accent filtering.
 */
data class VoiceRegionFilter(
    val localeCode: String,
    val displayName: String
)

object SearchableVoicePickerHelper {

    /**
     * Resolves human-friendly display name for a locale code (e.g. "en-US" -> "United States (en-US)").
     */
    fun formatRegionDisplayName(localeCode: String): String {
        return try {
            val tag = localeCode.replace('_', '-')
            val javaLocale = Locale.forLanguageTag(tag)
            val country = javaLocale.getDisplayCountry(Locale.ENGLISH)
            val lang = javaLocale.getDisplayLanguage(Locale.ENGLISH)
            if (country.isNotBlank()) "$country ($localeCode)" else if (lang.isNotBlank()) "$lang ($localeCode)" else localeCode
        } catch (_: Exception) {
            localeCode
        }
    }

    /**
     * Extracts distinct available region filters from the candidate catalog.
     */
    fun extractRegions(catalog: List<TtsVoice>): List<VoiceRegionFilter> {
        return catalog
            .map { it.locale.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .map { locale -> VoiceRegionFilter(localeCode = locale, displayName = formatRegionDisplayName(locale)) }
            .sortedBy { it.displayName }
    }

    /**
     * Matches a voice against search query supporting display name, id, locale, region, and synonyms.
     */
    fun matchesQuery(voice: TtsVoice, query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isBlank()) return true

        if (voice.displayName.lowercase().contains(q)) return true
        if (voice.id.lowercase().contains(q)) return true
        if (voice.locale.lowercase().contains(q)) return true

        val region = formatRegionDisplayName(voice.locale).lowercase()
        if (region.contains(q)) return true

        // Common geographic synonyms
        val loc = voice.locale.lowercase()
        val isUsSynonym = q in listOf("us", "usa", "american", "united states", "america") && loc.startsWith("en-us")
        val isUkSynonym = q in listOf("uk", "gb", "british", "britain", "united kingdom", "england", "english") && loc.startsWith("en-gb")
        val isAuSynonym = q in listOf("au", "aus", "australia", "australian") && loc.startsWith("en-au")
        val isCaSynonym = q in listOf("ca", "can", "canada", "canadian") && loc.startsWith("en-ca")
        val isSgSynonym = q in listOf("sg", "singapore", "singaporean") && loc.startsWith("en-sg")
        val isNzSynonym = q in listOf("nz", "new zealand", "kiwi") && loc.startsWith("en-nz")
        val isInSynonym = q in listOf("in", "india", "indian") && loc.startsWith("en-in")
        val isIeSynonym = q in listOf("ie", "ireland", "irish") && loc.startsWith("en-ie")
        val isZaSynonym = q in listOf("za", "south africa", "south african") && loc.startsWith("en-za")
        val isVnSynonym = q in listOf("vn", "vietnam", "vietnamese") && loc.startsWith("vi")

        return isUsSynonym || isUkSynonym || isAuSynonym || isCaSynonym || isSgSynonym || isNzSynonym || isInSynonym || isIeSynonym || isZaSynonym || isVnSynonym
    }
}

/**
 * Compact clickable anchor button displaying the currently selected voice.
 */
@Composable
fun VoicePickerAnchor(
    currentVoice: TtsVoice?,
    onClick: () -> Unit,
    placeholder: String = "Select Voice...",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(BatchTtsUiScale.voiceAnchorHeight)
            .clip(LERadius.xs)
            .clickable(onClick = onClick)
            .border(1.dp, LEColors.borderSubtle, LERadius.xs),
        color = LEColors.surface
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.md, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentVoice != null) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = currentVoice.displayName,
                        style = BatchTtsUiScale.controlPrimary,
                        color = LEColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${SearchableVoicePickerHelper.formatRegionDisplayName(currentVoice.locale)} · ${currentVoice.gender ?: "Neutral"}",
                        style = BatchTtsUiScale.controlSecondary,
                        color = LEColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = placeholder,
                    style = BatchTtsUiScale.controlPrimary,
                    color = LEColors.textMuted
                )
            }
            Spacer(modifier = Modifier.width(LESpacing.xs))
            Text("▾", style = BatchTtsUiScale.controlPrimary, color = LEColors.textMuted)
        }
    }
}

/**
 * State holder for session-persistent filters in SearchableVoicePickerDialog.
 */
class VoicePickerFilterState(
    initialSearchQuery: String = "",
    initialRegionCode: String? = null,
    initialGender: String? = null
) {
    var searchQuery by mutableStateOf(initialSearchQuery)
    var selectedRegionCode by mutableStateOf(initialRegionCode)
    var selectedGender by mutableStateOf(initialGender)

    fun clear() {
        searchQuery = ""
        selectedRegionCode = null
        selectedGender = null
    }
}

/**
 * Reusable modal dialog for searching and selecting TTS voices with region and gender filtering,
 * session-persistent filter state, and inline preview.
 */
@Composable
fun SearchableVoicePickerDialog(
    title: String,
    currentVoice: TtsVoice?,
    candidateVoices: List<TtsVoice>,
    filterState: VoicePickerFilterState = remember { VoicePickerFilterState() },
    previewingVoiceId: String? = null,
    onPreviewVoice: ((TtsVoice) -> Unit)? = null,
    onStopPreview: (() -> Unit)? = null,
    onSelectVoice: (TtsVoice) -> Unit,
    onDismiss: () -> Unit
) {
    var regionDropdownExpanded by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var lastPreviewedVoiceId by remember { mutableStateOf<String?>(null) }

    val availableRegions = remember(candidateVoices) {
        SearchableVoicePickerHelper.extractRegions(candidateVoices)
    }

    val filteredVoices = remember(candidateVoices, filterState.searchQuery, filterState.selectedRegionCode, filterState.selectedGender) {
        candidateVoices.filter { voice ->
            val matchesSearch = SearchableVoicePickerHelper.matchesQuery(voice, filterState.searchQuery)
            val matchesRegion = filterState.selectedRegionCode == null || voice.locale.equals(filterState.selectedRegionCode, ignoreCase = true)
            val matchesGender = filterState.selectedGender == null || voice.gender.equals(filterState.selectedGender, ignoreCase = true)
            matchesSearch && matchesRegion && matchesGender
        }
    }

    val handleDismiss = {
        onStopPreview?.invoke()
        onDismiss()
    }

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(BatchTtsUiScale.pickerDialogWidth)
                .height(BatchTtsUiScale.pickerDialogHeight)
                .clip(LERadius.md)
                .border(1.dp, LEColors.borderSubtle, LERadius.md)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Escape) {
                        handleDismiss()
                        true
                    } else false
                },
            color = LEColors.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(BatchTtsUiScale.cardPadding),
                verticalArrangement = Arrangement.spacedBy(LESpacing.md)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = BatchTtsUiScale.sectionHeading,
                        color = LEColors.textPrimary
                    )
                    LESecondaryButton(
                        text = "✕",
                        onClick = handleDismiss,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Search Box
                OutlinedTextField(
                    value = filterState.searchQuery,
                    onValueChange = { filterState.searchQuery = it },
                    placeholder = { Text("Tìm kiếm theo tên giọng, quốc gia, locale (ví dụ: Ava, US, en-GB)...", style = BatchTtsUiScale.body) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = BatchTtsUiScale.controlPrimary,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LEColors.primary,
                        unfocusedBorderColor = LEColors.borderSubtle
                    ),
                    trailingIcon = {
                        if (filterState.searchQuery.isNotBlank()) {
                            Text(
                                text = "✕",
                                style = BatchTtsUiScale.buttonLabel,
                                color = LEColors.textMuted,
                                modifier = Modifier.clickable { filterState.searchQuery = "" }.padding(LESpacing.xs)
                            )
                        }
                    }
                )

                // Filter Bar: Region Dropdown + Gender Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LESpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Region Dropdown Button
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(LERadius.xs)
                                .clickable { regionDropdownExpanded = true }
                                .border(1.dp, if (filterState.selectedRegionCode != null) LEColors.primary else LEColors.borderSubtle, LERadius.xs),
                            color = if (filterState.selectedRegionCode != null) LEColors.primarySoft else LEColors.surfaceElevated
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = filterState.selectedRegionCode?.let { SearchableVoicePickerHelper.formatRegionDisplayName(it) } ?: "Tất cả vùng / accent",
                                    style = BatchTtsUiScale.controlSecondary,
                                    fontWeight = if (filterState.selectedRegionCode != null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (filterState.selectedRegionCode != null) LEColors.primary else LEColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("▾", style = BatchTtsUiScale.controlSecondary, color = LEColors.textMuted)
                            }
                        }

                        DropdownMenu(
                            expanded = regionDropdownExpanded,
                            onDismissRequest = { regionDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tất cả vùng / accent", style = BatchTtsUiScale.body, fontWeight = if (filterState.selectedRegionCode == null) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    filterState.selectedRegionCode = null
                                    regionDropdownExpanded = false
                                }
                            )
                            availableRegions.forEach { region ->
                                DropdownMenuItem(
                                    text = { Text(region.displayName, style = BatchTtsUiScale.body, fontWeight = if (filterState.selectedRegionCode == region.localeCode) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        filterState.selectedRegionCode = region.localeCode
                                        regionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Gender Filter Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(null to "Tất cả", "Female" to "Nữ", "Male" to "Nam").forEach { (genderValue, label) ->
                            val isSelected = filterState.selectedGender == genderValue
                            Surface(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(LERadius.xs)
                                    .clickable { filterState.selectedGender = genderValue }
                                    .border(1.dp, if (isSelected) LEColors.primary else LEColors.borderSubtle, LERadius.xs),
                                color = if (isSelected) LEColors.primarySoft else LEColors.surfaceElevated
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = LESpacing.md),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = BatchTtsUiScale.controlSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) LEColors.primary else LEColors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Results Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hiển thị ${filteredVoices.size} / ${candidateVoices.size} giọng đọc",
                        style = BatchTtsUiScale.controlSecondary,
                        color = LEColors.textMuted
                    )
                    if (filterState.selectedRegionCode != null || filterState.selectedGender != null || filterState.searchQuery.isNotBlank()) {
                        Text(
                            text = "Xóa bộ lọc",
                            style = BatchTtsUiScale.controlSecondary,
                            fontWeight = FontWeight.Bold,
                            color = LEColors.primary,
                            modifier = Modifier.clickable {
                                filterState.clear()
                            }
                        )
                    }
                }

                HorizontalDivider(color = LEColors.borderSubtle)

                // Voice List
                if (filteredVoices.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không tìm thấy giọng đọc nào khớp với tìm kiếm.",
                            style = BatchTtsUiScale.body,
                            color = LEColors.textMuted
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredVoices, key = { it.id }) { voice ->
                                val isSelected = voice.id == currentVoice?.id
                                val isPlayingThis = previewingVoiceId == voice.id
                                val isLastPreviewed = !isPlayingThis && voice.id == lastPreviewedVoiceId

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(LERadius.xs)
                                        .clickable {
                                            handleDismiss()
                                            onSelectVoice(voice)
                                        }
                                        .border(
                                            width = if (isPlayingThis) 1.5.dp else 1.dp,
                                            color = when {
                                                isPlayingThis -> LEColors.primary
                                                isSelected -> LEColors.primary
                                                isLastPreviewed -> LEColors.primary.copy(alpha = 0.5f)
                                                else -> LEColors.borderSubtle.copy(alpha = 0.6f)
                                            },
                                            shape = LERadius.xs
                                        ),
                                    color = when {
                                        isPlayingThis -> LEColors.primarySoft
                                        isSelected -> LEColors.primarySoft.copy(alpha = 0.4f)
                                        isLastPreviewed -> LEColors.surfaceElevated
                                        else -> LEColors.surface
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = LESpacing.md, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(LESpacing.xs)
                                            ) {
                                                Text(
                                                    text = voice.displayName,
                                                    style = BatchTtsUiScale.controlPrimary,
                                                    fontWeight = if (isSelected || isPlayingThis) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSelected || isPlayingThis) LEColors.primary else LEColors.textPrimary
                                                )
                                                if (isPlayingThis) {
                                                    Surface(
                                                        color = LEColors.primary,
                                                        shape = LERadius.xs
                                                    ) {
                                                        Text(
                                                            text = "Đang phát",
                                                            style = BatchTtsUiScale.badge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else if (isLastPreviewed) {
                                                    Surface(
                                                        color = LEColors.surfaceElevated,
                                                        border = BorderStroke(1.dp, LEColors.primary.copy(alpha = 0.4f)),
                                                        shape = LERadius.xs
                                                    ) {
                                                        Text(
                                                            text = "Vừa nghe",
                                                            style = BatchTtsUiScale.badge,
                                                            color = LEColors.textMuted,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                if (voice.gender != null) {
                                                    Surface(
                                                        color = LEColors.surfaceElevated,
                                                        shape = LERadius.xs
                                                    ) {
                                                        Text(
                                                            text = voice.gender,
                                                            style = BatchTtsUiScale.controlSecondary,
                                                            color = LEColors.textSecondary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "${SearchableVoicePickerHelper.formatRegionDisplayName(voice.locale)} · ${voice.id}",
                                                style = BatchTtsUiScale.controlSecondary,
                                                color = LEColors.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(LESpacing.sm)
                                        ) {
                                            if (onPreviewVoice != null) {
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(LERadius.xs)
                                                        .clickable {
                                                            if (isPlayingThis) {
                                                                onStopPreview?.invoke()
                                                            } else {
                                                                lastPreviewedVoiceId = voice.id
                                                                onPreviewVoice(voice)
                                                            }
                                                        }
                                                        .border(
                                                            1.dp,
                                                            if (isPlayingThis) LEColors.primary else LEColors.borderSubtle,
                                                            LERadius.xs
                                                        ),
                                                    color = if (isPlayingThis) LEColors.primarySoft else LEColors.surfaceElevated
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isPlayingThis) "■ Stop" else "▶ Nghe thử",
                                                            style = BatchTtsUiScale.controlSecondary,
                                                            fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isPlayingThis) LEColors.primary else LEColors.textSecondary
                                                        )
                                                    }
                                                }
                                            }

                                            if (isSelected) {
                                                Text(
                                                    text = "✓",
                                                    style = BatchTtsUiScale.sectionHeading,
                                                    color = LEColors.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val canScrollUp = lazyListState.canScrollBackward || lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
                        val canScrollDown = lazyListState.canScrollForward

                        Column(
                            modifier = Modifier.fillMaxHeight().width(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Up navigation button (moves one row upward)
                            Surface(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(LERadius.xs)
                                    .clickable(enabled = canScrollUp) {
                                        coroutineScope.launch {
                                            val target = (lazyListState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                                            lazyListState.animateScrollToItem(target)
                                        }
                                    }
                                    .border(
                                        1.dp,
                                        if (canScrollUp) LEColors.borderSubtle else LEColors.borderSubtle.copy(alpha = 0.3f),
                                        LERadius.xs
                                    ),
                                color = if (canScrollUp) LEColors.surfaceElevated else LEColors.surface
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "▲",
                                        style = BatchTtsUiScale.previewSource,
                                        color = if (canScrollUp) LEColors.textPrimary else LEColors.textMuted
                                    )
                                }
                            }

                            // Draggable Vertical Scrollbar
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(lazyListState),
                                modifier = Modifier.weight(1f).width(8.dp),
                                style = ScrollbarStyle(
                                    minimalHeight = 24.dp,
                                    thickness = 8.dp,
                                    shape = LERadius.xs,
                                    hoverDurationMillis = 200,
                                    unhoverColor = LEColors.borderSubtle,
                                    hoverColor = LEColors.primary
                                )
                            )

                            // Down navigation button (moves one row downward)
                            Surface(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(LERadius.xs)
                                    .clickable(enabled = canScrollDown) {
                                        coroutineScope.launch {
                                            val target = (lazyListState.firstVisibleItemIndex + 1).coerceAtMost(filteredVoices.lastIndex)
                                            lazyListState.animateScrollToItem(target)
                                        }
                                    }
                                    .border(
                                        1.dp,
                                        if (canScrollDown) LEColors.borderSubtle else LEColors.borderSubtle.copy(alpha = 0.3f),
                                        LERadius.xs
                                    ),
                                color = if (canScrollDown) LEColors.surfaceElevated else LEColors.surface
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "▼",
                                        style = BatchTtsUiScale.previewSource,
                                        color = if (canScrollDown) LEColors.textPrimary else LEColors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    LESecondaryButton(
                        text = "Đóng",
                        onClick = handleDismiss,
                        modifier = Modifier.height(BatchTtsUiScale.buttonHeightSmall)
                    )
                }
            }
        }
    }
}
