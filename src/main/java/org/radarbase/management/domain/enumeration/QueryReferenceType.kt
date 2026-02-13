package org.radarbase.management.domain.enumeration

enum class QueryReferenceType(val symbol: String) {
    ROLLING_AVG("rolling_avg"),
    ABSOLUTE("absolute");


    companion object {
        fun fromSymbol(symbol: String?): QueryReferenceType {
            return QueryReferenceType.values().find { it.symbol == symbol }
                ?: throw IllegalArgumentException("[ComparisonOperator] Unknown symbol: $symbol")
        }
    }
}
