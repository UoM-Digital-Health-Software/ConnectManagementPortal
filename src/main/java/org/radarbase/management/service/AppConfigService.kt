package org.radarbase.management.service
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.radarbase.management.domain.AppConfig
import org.radarbase.management.domain.CacheSizeLog
import org.radarbase.management.domain.User
import org.radarbase.management.repository.AppConfigRepository
import org.radarbase.management.repository.CacheSizeLogRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.WeekFields
import java.util.*

@Service
class AppConfigService(private val repository: AppConfigRepository, private val cacheSizeLoRepository: CacheSizeLogRepository) {

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
        if (!evaluateConditional(conditionalExpr, context)) {
            return false
        }

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

    fun evaluateConditional(expr: String?, context: Map<String, Any>): Boolean {
        if (expr.isNullOrBlank()) return true

        val parser = SpelExpressionParser()
        val ctx = StandardEvaluationContext()

        context.forEach { (k, v) ->
            ctx.setVariable(k, v)
        }

        return try {
            parser.parseExpression(expr).getValue(ctx, Boolean::class.java) ?: false
        } catch (e: Exception) {
            false
        }
    }



    @Transactional
    fun logCacheSize(user: User, size: Int) {


        val todayStartUtc = ZonedDateTime.now(ZoneOffset.UTC)
            .toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)


        val exists = cacheSizeLoRepository.existsByUserIdAndCreatedOn(user.id!!, todayStartUtc)

        if(!exists) {
            val newCacheSizeLog  = CacheSizeLog()
            newCacheSizeLog.userId = user.id
            newCacheSizeLog.createdOn = todayStartUtc
            newCacheSizeLog.value = size.toLong()
            cacheSizeLoRepository.saveAndFlush(newCacheSizeLog)
        } else {
            log.info("today already saved")
        }

    }





    companion object {
        private val log = LoggerFactory.getLogger(AppConfigService::class.java)
    }
}
