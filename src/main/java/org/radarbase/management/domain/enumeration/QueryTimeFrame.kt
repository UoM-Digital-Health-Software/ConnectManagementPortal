package org.radarbase.management.domain.enumeration

enum class QueryTimeFrame(val symbol: String) {
    PAST_WEEK("1_weeks"),
    PAST_MONTH("1_months"),
    PAST_6_MONTH("6_months"),
    PAST_YEAR("1_years");

    companion object {
        fun fromSymbol(symbol: String): QueryTimeFrame {
            return values().find { it.symbol == symbol }
                ?: throw IllegalArgumentException("[ComparisonOperator] Unknown symbol: $symbol")
        }
    }
}
