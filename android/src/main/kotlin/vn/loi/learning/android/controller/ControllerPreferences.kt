package vn.loi.learning.android.controller

import android.content.Context
import android.content.SharedPreferences
import vn.loi.learning.android.platform.coordinatedApply
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

interface ControllerPreferenceStore {
    fun load(): ControllerConfig
    fun save(config: ControllerConfig)
}

class ControllerPreferencesController(private val store: ControllerPreferenceStore) {
    private val mutableConfig = MutableStateFlow(store.load())
    val config: StateFlow<ControllerConfig> = mutableConfig.asStateFlow()

    fun current(): ControllerConfig = mutableConfig.value

    fun updateConfig(config: ControllerConfig) {
        store.save(config)
        mutableConfig.value = config
    }

    fun setControllerEnabled(enabled: Boolean) {
        updateConfig(mutableConfig.value.copy(isControllerEnabled = enabled))
    }

    fun setActiveProfile(profileId: String) {
        if (mutableConfig.value.profiles.any { it.id == profileId }) {
            updateConfig(mutableConfig.value.copy(activeProfileId = profileId))
        }
    }

    fun updateActiveProfile(transform: (ControllerProfile) -> ControllerProfile) {
        val current = mutableConfig.value
        val updatedProfiles = current.profiles.map { profile ->
            if (profile.id == current.activeProfileId) transform(profile) else profile
        }
        updateConfig(current.copy(profiles = updatedProfiles))
    }

    fun resetToDefaults() {
        val def = DefaultControllerProfiles.defaultConfig()
        updateConfig(def)
    }
}

class SharedPreferencesControllerPreferenceStore(context: Context) : ControllerPreferenceStore {
    private val preferences: SharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun load(): ControllerConfig {
        val jsonStr = preferences.getString(KEY_CONFIG_JSON, null) ?: return DefaultControllerProfiles.defaultConfig()
        return runCatching { parseConfigJson(jsonStr) }.getOrElse {
            DefaultControllerProfiles.defaultConfig()
        }
    }

    override fun save(config: ControllerConfig) {
        val jsonStr = encodeConfigJson(config)
        preferences.edit().putString(KEY_CONFIG_JSON, jsonStr).coordinatedApply()
    }

    companion object {
        private const val FILE_NAME = "learning-engine-controller"
        private const val KEY_CONFIG_JSON = "config_json"
        private val jsonParser = Json { ignoreUnknownKeys = true }

        fun encodeConfigJson(config: ControllerConfig): String {
            val root = buildJsonObject {
                put("activeProfileId", config.activeProfileId)
                put("isControllerEnabled", config.isControllerEnabled)
                put("profiles", buildJsonArray {
                    for (profile in config.profiles) {
                        add(buildJsonObject {
                            put("id", profile.id)
                            put("name", profile.name)
                            put("enabled", profile.enabled)
                            put("deviceVendorId", profile.deviceVendorId)
                            put("deviceProductId", profile.deviceProductId)

                            profile.modifierInput?.let { mod ->
                                put("modifierInput", buildJsonObject {
                                    put("vendorId", mod.vendorId)
                                    put("productId", mod.productId)
                                    put("keyCode", mod.keyCode)
                                })
                            }

                            put("mappings", buildJsonArray {
                                for (mapping in profile.mappings) {
                                    add(buildJsonObject {
                                        put("context", mapping.context.name)
                                        put("action", mapping.action.name)
                                        put("gesture", buildJsonObject {
                                            put("pressType", mapping.gesture.pressType.name)
                                            put("input", buildJsonObject {
                                                put("vendorId", mapping.gesture.input.vendorId)
                                                put("productId", mapping.gesture.input.productId)
                                                put("keyCode", mapping.gesture.input.keyCode)
                                            })
                                            mapping.gesture.modifier?.let { gMod ->
                                                put("modifier", buildJsonObject {
                                                    put("vendorId", gMod.vendorId)
                                                    put("productId", gMod.productId)
                                                    put("keyCode", gMod.keyCode)
                                                })
                                            }
                                        })
                                    })
                                }
                            })
                        })
                    }
                })
            }
            return root.toString()
        }

        fun parseConfigJson(jsonStr: String): ControllerConfig = runCatching {
            val root = jsonParser.parseToJsonElement(jsonStr).jsonObject
            val activeProfileId = root["activeProfileId"]?.jsonPrimitive?.content ?: DefaultControllerProfiles.DEFAULT_PROFILE_ID
            val isControllerEnabled = root["isControllerEnabled"]?.jsonPrimitive?.booleanOrNull ?: true

            val profilesArray = root["profiles"]?.let { runCatching { it.jsonArray }.getOrNull() } ?: emptyList()
            val profiles = mutableListOf<ControllerProfile>()

            for (pElem in profilesArray) {
                val pObj = runCatching { pElem.jsonObject }.getOrNull() ?: continue
                val id = pObj["id"]?.jsonPrimitive?.content ?: continue
                if (id.isBlank()) continue
                val name = pObj["name"]?.jsonPrimitive?.content ?: "Custom Profile"
                val enabled = pObj["enabled"]?.jsonPrimitive?.booleanOrNull ?: true
                val vendorId = pObj["deviceVendorId"]?.jsonPrimitive?.intOrNull ?: DefaultControllerProfiles.VENDOR_8BITDO
                val productId = pObj["deviceProductId"]?.jsonPrimitive?.intOrNull ?: DefaultControllerProfiles.PRODUCT_MICRO_K

                val modifierInput = pObj["modifierInput"]?.let { runCatching { it.jsonObject }.getOrNull() }?.let { mObj ->
                    val mKeyCode = mObj["keyCode"]?.jsonPrimitive?.intOrNull ?: 0
                    if (mKeyCode != 0) {
                        ControllerPhysicalInput(
                            vendorId = mObj["vendorId"]?.jsonPrimitive?.intOrNull ?: vendorId,
                            productId = mObj["productId"]?.jsonPrimitive?.intOrNull ?: productId,
                            keyCode = mKeyCode
                        )
                    } else null
                }

                val mappingsArray = pObj["mappings"]?.let { runCatching { it.jsonArray }.getOrNull() } ?: emptyList()
                val mappings = mutableListOf<ControllerMapping>()

                for (mElem in mappingsArray) {
                    val mObj = runCatching { mElem.jsonObject }.getOrNull() ?: continue
                    val contextName = mObj["context"]?.jsonPrimitive?.content ?: continue
                    val actionName = mObj["action"]?.jsonPrimitive?.content ?: continue

                    val context = runCatching { ControllerContext.valueOf(contextName) }.getOrNull() ?: continue
                    val action = runCatching { ControllerAction.valueOf(actionName) }.getOrNull() ?: continue

                    val gObj = mObj["gesture"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: continue
                    val pressTypeName = gObj["pressType"]?.jsonPrimitive?.content ?: ControllerPressType.PRESS.name
                    val pressType = runCatching { ControllerPressType.valueOf(pressTypeName) }.getOrDefault(ControllerPressType.PRESS)

                    val inObj = gObj["input"]?.let { runCatching { it.jsonObject }.getOrNull() } ?: continue
                    val inKeyCode = inObj["keyCode"]?.jsonPrimitive?.intOrNull ?: continue
                    if (inKeyCode == 0) continue

                    val input = ControllerPhysicalInput(
                        vendorId = inObj["vendorId"]?.jsonPrimitive?.intOrNull ?: vendorId,
                        productId = inObj["productId"]?.jsonPrimitive?.intOrNull ?: productId,
                        keyCode = inKeyCode
                    )

                    val gestureModifier = gObj["modifier"]?.let { runCatching { it.jsonObject }.getOrNull() }?.let { gmObj ->
                        val gmKeyCode = gmObj["keyCode"]?.jsonPrimitive?.intOrNull ?: 0
                        if (gmKeyCode != 0) {
                            ControllerPhysicalInput(
                                vendorId = gmObj["vendorId"]?.jsonPrimitive?.intOrNull ?: vendorId,
                                productId = gmObj["productId"]?.jsonPrimitive?.intOrNull ?: productId,
                                keyCode = gmKeyCode
                            )
                        } else null
                    }

                    mappings.add(
                        ControllerMapping(
                            context = context,
                            gesture = ControllerGesture(
                                input = input,
                                pressType = pressType,
                                modifier = gestureModifier
                            ),
                            action = action
                        )
                    )
                }

                profiles.add(
                    ControllerProfile(
                        id = id,
                        name = name,
                        enabled = enabled,
                        deviceVendorId = vendorId,
                        deviceProductId = productId,
                        modifierInput = modifierInput,
                        mappings = mappings
                    )
                )
            }

            val finalProfiles = if (profiles.isEmpty()) listOf(DefaultControllerProfiles.defaultProfile()) else profiles
            ControllerConfig(
                activeProfileId = activeProfileId,
                profiles = finalProfiles,
                isControllerEnabled = isControllerEnabled
            )
        }.getOrElse {
            DefaultControllerProfiles.defaultConfig()
        }
    }
}
