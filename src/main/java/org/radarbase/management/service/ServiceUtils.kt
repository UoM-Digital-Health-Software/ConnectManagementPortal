package org.radarbase.management.service



import java.time.LocalTime
import java.time.ZoneId

object TimeUtils {
    fun getCurrentTime(zone: ZoneId = ZoneId.systemDefault()): LocalTime {
        return LocalTime.now(zone)
    }
}
