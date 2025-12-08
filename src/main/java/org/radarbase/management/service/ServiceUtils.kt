package org.radarbase.management.service



import java.time.LocalTime
import java.time.ZoneId

object TimeUtils {
    fun getCurrentTime(zone: ZoneId = ZoneId.systemDefault()): LocalTime {
        return LocalTime.now(zone)
    }
}


object QueryEvaluationOptions {

    //TODO:  change this to S3 before deploying
    var source = DataSource.CLASSPATH
    var minimumExpectedData = 0.3
    var aggregationLevel = AggregationLevel.DAY
    val resetThresholdDays = 2
    //TODO:  currently set for testing prposes, change to 7 for deployment
    val minNotificationIntervalDays = 1


}
