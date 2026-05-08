package org.radarbase.management.domain.enumeration

enum class QueryTimeFrame(val length: Int) {
    PAST_WEEK(7),
    PAST_MONTH(30),
    PAST_3_MONTH(60),
    PAST_6_MONTH(180),
    TODAY(1),
    PAST_YEAR(365);

    companion object {
        fun fromSymbol(length: Int): QueryTimeFrame {
            return values().find { it.length == length }
                ?: throw IllegalArgumentException("[ComparisonOperator] Unknown symbol: $length")
        }
    }
}
