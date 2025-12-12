package org.radarbase.management.service

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*

@Component
class WeekSaltProvider : SaltProvider {
    override fun getSalt(): String {
        val now = LocalDate.now()
        val week = now.get(WeekFields.of(Locale.UK).weekOfWeekBasedYear())
        return "week${now.year}_$week";
    }
}
