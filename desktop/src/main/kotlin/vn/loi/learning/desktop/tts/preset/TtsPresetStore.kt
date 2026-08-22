package vn.loi.learning.desktop.tts.preset

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.loi.learning.desktop.tts.TtsAudioParameters
import vn.loi.learning.desktop.tts.TtsField
import vn.loi.learning.desktop.tts.strategy.VoiceStrategyMode

interface TtsPresetRepository {
    fun listPresets(): List<TtsPreset>
    fun getPreset(id: String): TtsPreset?
    fun savePreset(preset: TtsPreset): TtsPreset
    fun saveAsPreset(name: String, sourcePreset: TtsPreset): TtsPreset
    fun duplicatePreset(sourceId: String, newName: String? = null): TtsPreset
    fun renamePreset(id: String, newName: String): TtsPreset
    fun deletePreset(id: String): Boolean
    fun getDefaultPreset(): TtsPreset
    fun getSelectedPresetId(): String?
    fun setSelectedPresetId(id: String?)
}

/**
 * File-backed repository for TTS presets using atomic JSON storage.
 */
class TtsPresetStore(
    private val filePath: Path
) : TtsPresetRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val lock = Any()

    override fun listPresets(): List<TtsPreset> = synchronized(lock) {
        val document = loadDocument()
        val presets = document.presets.map { it.toDomain() }
        if (presets.isEmpty()) {
            listOf(TtsPreset.createDefault())
        } else {
            // Deterministic ordering: built-in first, then alphabetical by name
            presets.sortedWith(
                compareByDescending<TtsPreset> { it.isBuiltIn }
                    .thenBy { it.name.lowercase() }
            )
        }
    }

    override fun getPreset(id: String): TtsPreset? = synchronized(lock) {
        listPresets().firstOrNull { it.id == id }
    }

    override fun getDefaultPreset(): TtsPreset = synchronized(lock) {
        val document = loadDocument()
        val selectedId = document.selectedPresetId
        if (selectedId != null) {
            getPreset(selectedId)?.let { return it }
        }
        getPreset(TtsPreset.DEFAULT_PRESET_ID) ?: listPresets().firstOrNull() ?: TtsPreset.createDefault()
    }

    override fun getSelectedPresetId(): String? = synchronized(lock) {
        loadDocument().selectedPresetId
    }

    override fun setSelectedPresetId(id: String?): Unit = synchronized(lock) {
        val doc = loadDocument()
        val updated = doc.copy(selectedPresetId = id)
        saveDocument(updated)
    }

    override fun savePreset(preset: TtsPreset): TtsPreset = synchronized(lock) {
        val trimmedName = preset.name.trim()
        validateName(trimmedName, excludeId = preset.id)

        val doc = loadDocument()
        val currentPresets = doc.presets.toMutableList()
        val existingIndex = currentPresets.indexOfFirst { it.id == preset.id }

        val now = System.currentTimeMillis()
        val updatedDomain = preset.copy(
            name = trimmedName,
            updatedAtEpochMillis = now
        )
        val jsonDto = updatedDomain.toJson()

        if (existingIndex >= 0) {
            currentPresets[existingIndex] = jsonDto
        } else {
            currentPresets.add(jsonDto)
        }

        saveDocument(doc.copy(presets = currentPresets, selectedPresetId = updatedDomain.id))
        updatedDomain
    }

    override fun saveAsPreset(name: String, sourcePreset: TtsPreset): TtsPreset = synchronized(lock) {
        val trimmedName = name.trim()
        validateName(trimmedName, excludeId = null)

        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val newPreset = sourcePreset.copy(
            id = newId,
            name = trimmedName,
            isBuiltIn = false,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )

        val doc = loadDocument()
        val currentPresets = doc.presets.toMutableList()
        currentPresets.add(newPreset.toJson())

        saveDocument(doc.copy(presets = currentPresets, selectedPresetId = newId))
        newPreset
    }

    override fun duplicatePreset(sourceId: String, newName: String?): TtsPreset = synchronized(lock) {
        val source = requireNotNull(getPreset(sourceId)) { "Source preset '$sourceId' not found." }
        val generatedName = newName?.trim() ?: generateUniqueDuplicateName(source.name)
        validateName(generatedName, excludeId = null)

        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val cloned = source.copy(
            id = newId,
            name = generatedName,
            isBuiltIn = false,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )

        val doc = loadDocument()
        val currentPresets = doc.presets.toMutableList()
        currentPresets.add(cloned.toJson())

        saveDocument(doc.copy(presets = currentPresets, selectedPresetId = newId))
        cloned
    }

    override fun renamePreset(id: String, newName: String): TtsPreset = synchronized(lock) {
        val existing = requireNotNull(getPreset(id)) { "Preset '$id' not found." }
        if (existing.isBuiltIn) {
            throw IllegalArgumentException("Cannot rename built-in default preset.")
        }
        val trimmedName = newName.trim()
        validateName(trimmedName, excludeId = id)

        val updated = existing.copy(
            name = trimmedName,
            updatedAtEpochMillis = System.currentTimeMillis()
        )

        val doc = loadDocument()
        val currentPresets = doc.presets.toMutableList()
        val idx = currentPresets.indexOfFirst { it.id == id }
        if (idx >= 0) {
            currentPresets[idx] = updated.toJson()
            saveDocument(doc.copy(presets = currentPresets))
        }
        updated
    }

    override fun deletePreset(id: String): Boolean = synchronized(lock) {
        val existing = getPreset(id) ?: return false
        if (existing.isBuiltIn) {
            throw IllegalArgumentException("Cannot delete built-in default preset.")
        }

        val doc = loadDocument()
        val currentPresets = doc.presets.filter { it.id != id }
        val newSelected = if (doc.selectedPresetId == id) TtsPreset.DEFAULT_PRESET_ID else doc.selectedPresetId

        saveDocument(doc.copy(presets = currentPresets, selectedPresetId = newSelected))
        true
    }

    private fun validateName(name: String, excludeId: String?) {
        require(name.isNotBlank()) { "Preset name must not be blank." }
        require(name.length <= TtsPreset.MAX_NAME_LENGTH) {
            "Preset name must not exceed ${TtsPreset.MAX_NAME_LENGTH} characters."
        }
        val doc = loadDocument()
        val exists = doc.presets.any { it.id != excludeId && it.name.equals(name, ignoreCase = true) }
        if (exists) {
            throw IllegalArgumentException("A preset named '$name' already exists.")
        }
    }

    private fun generateUniqueDuplicateName(baseName: String): String {
        val cleanBase = baseName.replace(Regex(" \\(Copy( \\d+)?\\)$"), "")
        var index = 1
        var candidate = "$cleanBase (Copy)"
        val existingNames = listPresets().map { it.name.lowercase() }.toSet()
        while (candidate.lowercase() in existingNames) {
            index++
            candidate = "$cleanBase (Copy $index)"
        }
        return candidate
    }

    private fun loadDocument(): TtsPresetDocumentJson {
        if (!Files.isRegularFile(filePath)) {
            val initialDefault = TtsPresetDocumentJson(
                presets = listOf(TtsPreset.createDefault().toJson()),
                selectedPresetId = TtsPreset.DEFAULT_PRESET_ID
            )
            return initialDefault
        }

        return try {
            val content = Files.readString(filePath, StandardCharsets.UTF_8)
            if (content.isBlank()) {
                TtsPresetDocumentJson(presets = listOf(TtsPreset.createDefault().toJson()))
            } else {
                val parsed = json.decodeFromString<TtsPresetDocumentJson>(content)
                if (parsed.schemaVersion > TtsPresetDocumentJson.CURRENT_SCHEMA_VERSION) {
                    throw IllegalStateException("Unsupported TTS preset schema version: ${parsed.schemaVersion}")
                }
                // Ensure at least default preset is present
                if (parsed.presets.isEmpty()) {
                    parsed.copy(presets = listOf(TtsPreset.createDefault().toJson()))
                } else {
                    parsed
                }
            }
        } catch (ex: Exception) {
            if (ex is IllegalStateException) throw ex
            // Corrupt file fallback
            TtsPresetDocumentJson(presets = listOf(TtsPreset.createDefault().toJson()))
        }
    }

    private fun saveDocument(doc: TtsPresetDocumentJson) {
        val target = filePath.toAbsolutePath().normalize()
        val parent = target.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }
        val temp = Files.createTempFile(parent, "tts-presets-", ".tmp")
        try {
            val content = json.encodeToString(doc)
            Files.writeString(temp, content, StandardCharsets.UTF_8)
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private fun TtsPresetJson.toDomain(): TtsPreset = TtsPreset(
        id = id,
        name = name,
        selectedFields = selectedFields.mapNotNull { TtsField.fromToken(it) }.toSet().ifEmpty {
            setOf(TtsField.QUESTION, TtsField.ANSWER, TtsField.EXAMPLE, TtsField.TRANSLATION)
        },
        english = english.toDomain(),
        vietnamese = vietnamese.toDomain(),
        isBuiltIn = isBuiltIn,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis
    )

    private fun TtsLanguageConfigPresetJson.toDomain(): TtsLanguagePresetConfig = TtsLanguagePresetConfig(
        strategyMode = runCatching { VoiceStrategyMode.valueOf(strategyMode) }.getOrDefault(VoiceStrategyMode.FALLBACK_CHAIN),
        primaryVoiceId = primaryVoiceId,
        fallbackVoiceIds = fallbackVoiceIds,
        candidateVoiceIds = candidateVoiceIds.ifEmpty { listOf(primaryVoiceId) + fallbackVoiceIds },
        audioParameters = TtsAudioParameters(
            ratePercent = ratePercent.coerceIn(TtsAudioParameters.MIN_RATE, TtsAudioParameters.MAX_RATE),
            pitchHz = pitchHz.coerceIn(TtsAudioParameters.MIN_PITCH, TtsAudioParameters.MAX_PITCH),
            volumePercent = volumePercent.coerceIn(TtsAudioParameters.MIN_VOLUME, TtsAudioParameters.MAX_VOLUME)
        )
    )

    private fun TtsPreset.toJson(): TtsPresetJson = TtsPresetJson(
        id = id,
        name = name,
        selectedFields = selectedFields.map { it.name.lowercase() },
        english = english.toJson(),
        vietnamese = vietnamese.toJson(),
        isBuiltIn = isBuiltIn,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis
    )

    private fun TtsLanguagePresetConfig.toJson(): TtsLanguageConfigPresetJson = TtsLanguageConfigPresetJson(
        strategyMode = strategyMode.name,
        primaryVoiceId = primaryVoiceId,
        fallbackVoiceIds = fallbackVoiceIds,
        candidateVoiceIds = candidateVoiceIds,
        ratePercent = audioParameters.ratePercent,
        pitchHz = audioParameters.pitchHz,
        volumePercent = audioParameters.volumePercent
    )
}
