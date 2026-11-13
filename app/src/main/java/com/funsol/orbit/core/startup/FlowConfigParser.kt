package com.funsol.orbit.core.startup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

object FlowConfigParser {
    fun parse(json: String?, fallback: FlowConfig): FlowConfig {
        if (json.isNullOrBlank()) return fallback
        return try {
            val root = Json.parseToJsonElement(json)
            val screens = (root as? JsonObject)?.get("screens") as? JsonArray ?: return fallback
            val steps = screens.mapNotNull { element -> parseStep(element) }
            if (steps.isEmpty()) fallback else FlowConfig(steps)
        } catch (_: Throwable) {
            fallback
        }
    }

    private fun parseStep(element: kotlinx.serialization.json.JsonElement): FlowStep? {
        val obj = runCatching { element.jsonObject }.getOrNull() ?: return null
        val id = (obj["id"] as? JsonPrimitive)?.contentOrNull?.trim()?.uppercase().orEmpty()
        val type = FlowStepType.entries.firstOrNull { it.name == id } ?: return null
        val enabled = (obj["enabled"] as? JsonPrimitive)?.booleanOrNull ?: true
        val variant = ((obj["screenType"] as? JsonPrimitive)?.intOrNull ?: 1).coerceIn(1, MAX_PREMIUM_VARIANT)
        return FlowStep(type = type, enabled = enabled, premiumVariant = variant)
    }

    const val MAX_PREMIUM_VARIANT = 3
}
