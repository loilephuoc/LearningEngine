package vn.loi.learning.android.controller

import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Controller Settings screen with Simple & Unified UX (Phase 1.4.1).
 * Mental model: Physical Button -> Action.
 * Organized into:
 * 1. Button Assignments (Everywhere / Default Mappings)
 * 2. Context Overrides (Advanced) - Collapsible by default
 * 3. Advanced Profile & Diagnostics - Collapsible by default
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerSettingsScreen(
    onBack: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? vn.loi.learning.android.LearningEngineAndroidApplication
    val preferencesController = remember {
        app?.controllerPreferencesController
            ?: ControllerPreferencesController(SharedPreferencesControllerPreferenceStore(context))
    }

    val config by preferencesController.config.collectAsStateWithLifecycle()
    val activeProfile = config.activeProfile

    var editingMapping by remember { mutableStateOf<ControllerMapping?>(null) }
    var isAddingMapping by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var isOverridesExpanded by remember { mutableStateOf(false) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    // Level 1: Everywhere / Normal Button Assignments
    val everywhereMappings = remember(activeProfile.mappings) {
        activeProfile.mappings
            .filter { it.context == ControllerContext.GLOBAL }
            .sortedWith(
                compareBy(
                    { it.action.category.ordinal },
                    { it.action.ordinal }
                )
            )
    }

    // Level 2: Context Overrides (Advanced)
    val overrideMappings = remember(activeProfile.mappings) {
        activeProfile.mappings
            .filter { it.context != ControllerContext.GLOBAL }
            .sortedWith(
                compareBy(
                    { it.context.ordinal },
                    { it.action.category.ordinal },
                    { it.action.ordinal }
                )
            )
    }

    val groupedOverrides = remember(overrideMappings) {
        overrideMappings.groupBy { it.context }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "8BitDo / Controller Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeProfile.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Defaults")
                    }
                    IconButton(onClick = onOpenDiagnostics) {
                        Icon(Icons.Default.BugReport, contentDescription = "Diagnostics")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                // Master Enable & Hardware Connection Status Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val diagState by ControllerDiagnosticsHolder.state.collectAsStateWithLifecycle()
                    val candidateDevice = diagState.connectedDevices.firstOrNull { it.isCandidateController }

                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Controller Input",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Process hardware controller actions in foreground, background, and screen-off",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = config.isControllerEnabled,
                                onCheckedChange = { preferencesController.setControllerEnabled(it) }
                            )
                        }

                        HorizontalDivider()

                        // 1. Accessibility Service Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Accessibility Service:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (diagState.isAccessibilityServiceConnected) {
                                        "Service active (key filter ready)"
                                    } else {
                                        "Required for background / screen-off input"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            if (diagState.isAccessibilityServiceConnected) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "ENABLED",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(
                                            android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Enable", fontSize = 11.sp)
                                }
                            }
                        }

                        // 2. Controller Connection Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Controller Hardware:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (candidateDevice != null) {
                                        candidateDevice.name
                                    } else {
                                        "Auto-resumes immediately upon Bluetooth reconnect"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (candidateDevice != null) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = if (candidateDevice != null) "CONNECTED" else "STANDBY",
                                    color = if (candidateDevice != null) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 1: Button Assignments (Everywhere / Default Mappings)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Button Assignments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Default actions that apply everywhere across learning modes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = { isAddingMapping = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Binding")
                    }
                }
            }

            if (everywhereMappings.isEmpty() && overrideMappings.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No button assignments configured",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { isAddingMapping = true }) {
                                Text("Add First Assignment")
                            }
                        }
                    }
                }
            } else {
                items(everywhereMappings, key = { "${it.context}_${it.gesture.displayLabel}_${it.action.name}" }) { mapping ->
                    UnifiedMappingCard(
                        mapping = mapping,
                        onEdit = { editingMapping = mapping },
                        onDelete = {
                            preferencesController.updateActiveProfile { prof ->
                                ControllerMappingResolver.removeMapping(prof, mapping.context, mapping.gesture)
                            }
                        }
                    )
                }
            }

            // Section 2: Context Overrides (Advanced)
            if (overrideMappings.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isOverridesExpanded = !isOverridesExpanded }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Context overrides (Advanced)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${overrideMappings.size} overrides configured for specific screens",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Icon(
                                if (isOverridesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isOverridesExpanded) "Collapse overrides" else "Expand overrides"
                            )
                        }
                    }
                }

                if (isOverridesExpanded) {
                    for ((ctx, ctxMappings) in groupedOverrides) {
                        item(key = "ctx_header_${ctx.name}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = ctx.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "(${ctxMappings.size})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(ctxMappings, key = { "override_${it.context}_${it.gesture.displayLabel}_${it.action.name}" }) { mapping ->
                            UnifiedMappingCard(
                                mapping = mapping,
                                onEdit = { editingMapping = mapping },
                                onDelete = {
                                    preferencesController.updateActiveProfile { prof ->
                                        ControllerMappingResolver.removeMapping(prof, mapping.context, mapping.gesture)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Section 3: Advanced Profile & Diagnostics Collapsible Section
            item {
                Spacer(Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Advanced Profile & Diagnostics",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isAdvancedExpanded) "Collapse" else "Expand"
                            )
                        }

                        AnimatedVisibility(
                            visible = isAdvancedExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Active Profile:", style = MaterialTheme.typography.bodyMedium)
                                    Text(activeProfile.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Modifier Chord Key:", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        activeProfile.modifierInput?.keyCodeName ?: "None",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                val basicCount = activeProfile.mappings.count { it.context == ControllerContext.GLOBAL }
                                val overrideCount = activeProfile.mappings.size - basicCount
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Assignments Breakdown:", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "$basicCount Everywhere, $overrideCount Overrides",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onOpenDiagnostics,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Open Diagnostics", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { showResetConfirm = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Reset Defaults", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Add or Edit Mapping Dialog
    if (isAddingMapping || editingMapping != null) {
        val initial = editingMapping
        UnifiedMappingEditDialog(
            initialMapping = initial,
            activeProfile = activeProfile,
            onDismiss = {
                isAddingMapping = false
                editingMapping = null
            },
            onSave = { updatedMapping ->
                preferencesController.updateActiveProfile { prof ->
                    if (initial != null) {
                        ControllerMappingResolver.replaceMapping(prof, initial, updatedMapping)
                    } else {
                        ControllerMappingResolver.upsertMapping(prof, updatedMapping)
                    }
                }
                isAddingMapping = false
                editingMapping = null
                Toast.makeText(context, "Mapping saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset to Default Mappings?") },
            text = { Text("This will restore the standard 8BitDo Micro (K-Mode) default configuration.") },
            confirmButton = {
                Button(
                    onClick = {
                        preferencesController.resetToDefaults()
                        showResetConfirm = false
                        Toast.makeText(context, "Restored default mappings", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Compact, responsive assignment card designed to prevent horizontal constraint competition (Phase 1.4.1).
 * Layout hierarchy:
 * - Top: Action Title (Full width) + Description (Full width directly below title)
 * - Bottom: Scope Badge + Controller Button Badge (Left) ... Edit & Delete Controls (Right)
 */
@Composable
fun UnifiedMappingCard(
    mapping: ControllerMapping,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEverywhere = mapping.context == ControllerContext.GLOBAL

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // 1. Action Title (Full available width)
            Text(
                text = mapping.action.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Description directly below title
            if (mapping.action.description.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = mapping.action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(10.dp))

            // 3. Bottom controls row: Scope badge, Controller button badge, Edit/Delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Badges Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Scope / Context Badge (concise, single-line, no "Only:")
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isEverywhere) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        }
                    ) {
                        Text(
                            text = mapping.context.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isEverywhere) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            },
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    // Physical Controller Button Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = mapping.gesture.displayLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Edit & Delete Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit assignment",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete assignment",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnifiedMappingEditDialog(
    initialMapping: ControllerMapping?,
    activeProfile: ControllerProfile,
    onDismiss: () -> Unit,
    onSave: (ControllerMapping) -> Unit
) {
    var selectedAction by remember {
        mutableStateOf(initialMapping?.action ?: ControllerAction.REPLAY_PRIMARY_AUDIO)
    }

    var selectedKeyCode by remember {
        mutableStateOf(initialMapping?.gesture?.input?.keyCode ?: KeyEvent.KEYCODE_K) // Default to L1
    }

    var selectedPressType by remember {
        mutableStateOf(initialMapping?.gesture?.pressType ?: ControllerPressType.PRESS)
    }

    var isModifierChord by remember {
        mutableStateOf(initialMapping?.gesture?.modifier != null)
    }

    var isAdvancedOptionsExpanded by remember {
        mutableStateOf(initialMapping?.context != null && initialMapping.context != ControllerContext.GLOBAL)
    }

    var selectedContext by remember {
        mutableStateOf(initialMapping?.context ?: ControllerContext.GLOBAL)
    }

    var isDetectingButton by remember { mutableStateOf(false) }

    // Live controller input detection
    val diagState by ControllerDiagnosticsHolder.state.collectAsStateWithLifecycle()
    val lastEvent = diagState.events.firstOrNull()

    LaunchedEffect(lastEvent, isDetectingButton) {
        if (isDetectingButton && lastEvent != null && lastEvent.isCandidateController && lastEvent.action == KeyEvent.ACTION_DOWN) {
            selectedKeyCode = lastEvent.keyCode
            isDetectingButton = false
        }
    }

    val currentModifier = if (isModifierChord) activeProfile.modifierInput else null
    val targetGesture = ControllerGesture(
        input = ControllerPhysicalInput(
            vendorId = activeProfile.deviceVendorId,
            productId = activeProfile.deviceProductId,
            keyCode = selectedKeyCode
        ),
        pressType = selectedPressType,
        modifier = currentModifier
    )
    val candidateMapping = ControllerMapping(selectedContext, targetGesture, selectedAction)
    val conflict = ControllerMappingResolver.findConflict(activeProfile, candidateMapping)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialMapping == null) "Add Button Assignment" else "Edit Button Assignment",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 1. Action Selector (First!)
                Text("1. Select Action:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                var actionExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = actionExpanded,
                    onExpandedChange = { actionExpanded = !actionExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAction.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = actionExpanded,
                        onDismissRequest = { actionExpanded = false }
                    ) {
                        ControllerAction.entries.filter { it.isUserSelectable }.forEach { act ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(act.label, fontWeight = FontWeight.Bold)
                                        Text(
                                            act.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedAction = act
                                    actionExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. Controller Button Selector
                Text("2. Controller Button:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                var keyExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = keyExpanded,
                    onExpandedChange = { keyExpanded = !keyExpanded }
                ) {
                    val label = ControllerButtonDirectory.getButtonFullName(selectedKeyCode)
                    OutlinedTextField(
                        value = label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = keyExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = keyExpanded,
                        onDismissRequest = { keyExpanded = false }
                    ) {
                        ControllerButtonDirectory.ALL_BUTTONS.forEach { info ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(info.primaryName, fontWeight = FontWeight.Bold)
                                        Text(
                                            info.secondaryDescription,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedKeyCode = info.keyCode
                                    keyExpanded = false
                                }
                            )
                        }
                    }
                }

                // Optional "Press button to detect" button
                OutlinedButton(
                    onClick = { isDetectingButton = !isDetectingButton },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Gamepad, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isDetectingButton) "🎮 Press physical button on 8BitDo now..." else "🎮 Press button to detect automatically",
                        fontSize = 12.sp,
                        color = if (isDetectingButton) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // 3. Press Type Selector
                Text("3. Press Type:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                var pressTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = pressTypeExpanded,
                    onExpandedChange = { pressTypeExpanded = !pressTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPressType.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pressTypeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = pressTypeExpanded,
                        onDismissRequest = { pressTypeExpanded = false }
                    ) {
                        ControllerPressType.entries.forEach { pt ->
                            DropdownMenuItem(
                                text = { Text(pt.label, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedPressType = pt
                                    pressTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                // 4. Advanced Context Options (Collapsible)
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAdvancedOptionsExpanded = !isAdvancedOptionsExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Advanced Context & Modifiers",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            if (isAdvancedOptionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isAdvancedOptionsExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Apply In:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                            var contextExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = contextExpanded,
                                onExpandedChange = { contextExpanded = !contextExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedContext.label,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contextExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = contextExpanded,
                                    onDismissRequest = { contextExpanded = false }
                                ) {
                                    ControllerContext.entries.forEach { ctx ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(ctx.label, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        ctx.description,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedContext = ctx
                                                contextExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Modifier Checkbox
                            if (activeProfile.modifierInput != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = isModifierChord,
                                        onCheckedChange = { isModifierChord = it }
                                    )
                                    Text(
                                        text = "Require modifier key (${activeProfile.modifierInput.keyCodeName})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // Conflict warning
                if (conflict != null && conflict != initialMapping) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "⚠️ Button '${targetGesture.displayLabel}' is already assigned to '${conflict.action.label}' in ${if (selectedContext == ControllerContext.GLOBAL) "Everywhere" else selectedContext.label}. Saving will replace it.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(candidateMapping) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
