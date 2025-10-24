package org.radarbase.management.service

import org.radarbase.management.domain.AppConfig
import org.radarbase.management.repository.AppConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*

@Service
class AppConfigService(private val repository: AppConfigRepository) {

    fun getMergedConfig(site: String?, userId: Long?): Map<String?, AppConfig?> {
        val configMap = mutableMapOf<String?, AppConfig?>()


        repository.findBySiteIsNullAndUserIdIsNull()
            .forEach { configMap[it.key] = it }

        site?.let {
            repository.findBySiteAndUserIdIsNull(it)
                .forEach { configMap[it.key] = it }
        }

        userId?.let {
            repository.findByUserId(it)
                .forEach { configMap[it.key] = it }
        }

        log.info("returning config {}", configMap)

        return configMap
    }


    fun isFeatureEnabled(site: String?, userId: Long?, userLogin:String, feature: String,  context: Map<String, Any> = emptyMap()): Boolean {
        val config  = getMergedConfig(site, userId)

        val enabled = config[feature]?.value.toBoolean() ?: false
        if (!enabled) return false

        val rolloutPct = config[feature]?.rolloutPct?.toInt() ?: 100

        val salt = getWeekSalt()
        val bucket = getUserBucket(userLogin, salt)
        if (bucket >= rolloutPct) return false

        val conditionalExpr = config[feature]?.conditional
        if (!evaluateConditional(conditionalExpr, context)) return false

        return true
    }

    private fun getUserBucket(userLogin: String, salt: String): Int {
        val input = "${userLogin}_${salt}"

        var hash = 0
        for (c in input) {
            hash = (hash shl 5) - hash + c.code
            hash = hash and 0xFFFFFFFF.toInt()
        }
        return kotlin.math.abs(hash) % 100
    }

    private fun getWeekSalt(): String {
        val now = LocalDate.now()
        val week = now.get(WeekFields.of(Locale.UK).weekOfWeekBasedYear())
        return "week${now.year}_$week"
    }

    private fun evaluateConditional(expr: String?, context: Map<String, Any>): Boolean {
        if (expr.isNullOrBlank()) return true
        var replaced = expr
        context.forEach { (k, v) ->
            replaced = replaced!!.replace("\b$k\b".toRegex(), v.toString())
        }
        return try {
            val engine = javax.script.ScriptEngineManager().getEngineByName("nashorn")
            engine.eval(replaced) as Boolean
        } catch (e: Exception) {
            false
        }
    }





    companion object {
        private val log = LoggerFactory.getLogger(AppConfigService::class.java)
    }
}
