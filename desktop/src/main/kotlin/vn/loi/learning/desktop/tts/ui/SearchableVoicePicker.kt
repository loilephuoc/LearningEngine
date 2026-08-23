package vn.loi.learning.desktop.tts.ui

import androidx.compose.foundation.BorderStroke
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
            .height(38.dp)
            .clip(LERadius.xs)
            .clickable(onClick = onClick)
            .border(1.dp, LEColors.borderSubtle, LERadius.xs),
        color = LEColors.surface
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentVoice != null) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = currentVoice.displayName,
                        style = LETypography.caption,
                        fontWeight = FontWeight.Bold,
                        color = LEColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${SearchableVoicePickerHelper.formatRegionDisplayName(currentVoice.locale)} · ${currentVoice.gender ?: "Neutral"}",
                        style = LETypography.caption,
                        color = LEColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = placeholder,
                    style = LETypography.caption,
                    color = LEColors.textMuted
                )
            }
            Spacer(modifier = Modifier.width(LESpacing.xs))
            Text("▾", style = LETypography.fieldValue, color = LEColors.textMuted)
        }
    }
}

/**
 * Reusable modal dialog for searching and selecting TTS voices with region and gender filtering.
 */
@Composable
fun SearchableVoicePickerDialog(
    title: String,
    currentVoice: TtsVoice?,
    candidateVoices: List<TtsVoice>,
    onSelectVoice: (TtsVoice) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRegionCode by remember { mutableStateOf<String?>(null) }
    var selectedGender by remember { mutableStateOf<String?>(null) } // null = All, "Female", "Male"
    var regionDropdownExpanded by remember { mutableStateOf(false) }

    val availableRegions = remember(candidateVoices) {
        SearchableVoicePickerHelper.extractRegions(candidateVoices)
    }

    val filteredVoices = remember(candidateVoices, searchQuery, selectedRegionCode, selectedGender) {
        candidateVoices.filter { voice ->
            val matchesSearch = SearchableVoicePickerHelper.matchesQuery(voice, searchQuery)
            val matchesRegion = selectedRegionCode == null || voice.locale.equals(selectedRegionCode, ignoreCase = true)
            val matchesGender = selectedGender == null || voice.gender.equals(selectedGender, ignoreCase = true)
            matchesSearch && matchesRegion && matchesGender
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(540.dp)
                .height(520.dp)
                .clip(LERadius.md)
                .border(1.dp, LEColors.borderSubtle, LERadius.md)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Escape) {
                        onDismiss()
                        true
                    } else false
                },
            color = LEColors.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(LESpacing.lg),
                verticalArrangement = Arrangement.spacedBy(LESpacing.sm)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = LETypography.paneTitle,
                        fontWeight = FontWeight.Bold,
                        color = LEColors.textPrimary
                    )
                    LESecondaryButton(
                        text = "✕",
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm kiếm theo tên giọng, quốc gia, locale (ví dụ: Ava, US, en-GB)...", style = LETypography.caption) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LETypography.fieldValue,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LEColors.primary,
                        unfocusedBorderColor = LEColors.borderSubtle
                    ),
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            Text(
                                text = "✕",
                                style = LETypography.caption,
                                color = LEColors.textMuted,
                                modifier = Modifier.clickable { searchQuery = "" }.padding(LESpacing.xs)
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
                                .height(32.dp)
                                .clip(LERadius.xs)
                                .clickable { regionDropdownExpanded = true }
                                .border(1.dp, if (selectedRegionCode != null) LEColors.primary else LEColors.borderSubtle, LERadius.xs),
                            color = if (selectedRegionCode != null) LEColors.primarySoft else LEColors.surfaceElevated
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = LESpacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedRegionCode?.let { SearchableVoicePickerHelper.formatRegionDisplayName(it) } ?: "Tất cả vùng / accent",
                                    style = LETypography.caption,
                                    fontWeight = if (selectedRegionCode != null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedRegionCode != null) LEColors.primary else LEColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("▾", style = LETypography.caption, color = LEColors.textMuted)
                            }
                        }

                        DropdownMenu(
                            expanded = regionDropdownExpanded,
                            onDismissRequest = { regionDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tất cả vùng / accent", style = LETypography.caption, fontWeight = if (selectedRegionCode == null) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedRegionCode = null
                                    regionDropdownExpanded = false
                                }
                            )
                            availableRegions.forEach { region ->
                                DropdownMenuItem(
                                    text = { Text(region.displayName, style = LETypography.caption, fontWeight = if (selectedRegionCode == region.localeCode) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedRegionCode = region.localeCode
                                        regionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Gender Filter Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(null to "Tất cả", "Female" to "Nữ", "Male" to "Nam").forEach { (genderValue, label) ->
                            val isSelected = selectedGender == genderValue
                            Surface(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(LERadius.xs)
                                    .clickable { selectedGender = genderValue }
                                    .border(1.dp, if (isSelected) LEColors.primary else LEColors.borderSubtle, LERadius.xs),
                                color = if (isSelected) LEColors.primarySoft else LEColors.surfaceElevated
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = LESpacing.sm),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = LETypography.caption,
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
                        style = LETypography.caption,
                        color = LEColors.textMuted
                    )
                    if (selectedRegionCode != null || selectedGender != null || searchQuery.isNotBlank()) {
                        Text(
                            text = "Xóa bộ lọc",
                            style = LETypography.caption,
                            fontWeight = FontWeight.Bold,
                            color = LEColors.primary,
                            modifier = Modifier.clickable {
                                searchQuery = ""
                                selectedRegionCode = null
                                selectedGender = null
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
                            style = LETypography.fieldValue,
                            color = LEColors.textMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredVoices, key = { it.id }) { voice ->
                            val isSelected = voice.id == currentVoice?.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(LERadius.xs)
                                    .clickable {
                                        onSelectVoice(voice)
                                        onDismiss()
                                    }
                                    .border(
                                        1.dp,
                                        if (isSelected) LEColors.primary else LEColors.borderSubtle.copy(alpha = 0.6f),
                                        LERadius.xs
                                    ),
                                color = if (isSelected) LEColors.primarySoft else LEColors.surface
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
                                                style = LETypography.fieldValue,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) LEColors.primary else LEColors.textPrimary
                                            )
                                            if (voice.gender != null) {
                                                Surface(
                                                    color = LEColors.surfaceElevated,
                                                    shape = LERadius.xs
                                                ) {
                                                    Text(
                                                        text = voice.gender,
                                                        style = LETypography.caption,
                                                        color = LEColors.textSecondary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${SearchableVoicePickerHelper.formatRegionDisplayName(voice.locale)} · ${voice.id}",
                                            style = LETypography.caption,
                                            color = if (isSelected) LEColors.primary.copy(alpha = 0.8f) else LEColors.textMuted
                                        )
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(LEColors.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "✓",
                                                style = LETypography.caption,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
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
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}
