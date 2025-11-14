package org.radarbase.management.domain.enumeration

enum class PhysicalMetric(
    val displayName: String,
    val type: String,
    val unit: String?,
) {
    HEART_RATE("Heart Rate", "number", "bpm"),

    HRV("HRV", "number", "ms"),

    BREATHING_RATE("Breathing Rate", "number", "rpm"),

    STEPS("Steps", "number", null),

    STAIRS("Stairs", "number", null),

    SLEEP_LENGTH("Sleep Duration", "number", "minutes"),

    ACTIVITY("Activities", "number", null),

    EXERCISE_TIME("Exercise time", "number", "count"),

    HEART_RATE_RESTING("Heart Rate Resting", "number", "bpm"),}



fun physicalMetricExists(name: String?): Boolean {
    return try {
        if (name != null) {
            PhysicalMetric.valueOf(name)
            return true
        }
        return false

    } catch (e: IllegalArgumentException) {
        return false
    }
}
