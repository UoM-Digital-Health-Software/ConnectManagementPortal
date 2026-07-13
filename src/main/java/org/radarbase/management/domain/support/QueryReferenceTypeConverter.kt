package org.radarbase.management.domain.support

import org.radarbase.management.domain.enumeration.QueryReferenceType
import org.radarbase.management.domain.enumeration.QueryTimeFrame
import javax.persistence.AttributeConverter
import javax.persistence.Converter


@Converter(autoApply = false)
class QueryReferenceTypeConverter : AttributeConverter<QueryReferenceType, String> {
    override fun convertToDatabaseColumn(attribute: QueryReferenceType?): String? {

        if(attribute == null) {
            return null
        }

        return attribute.symbol
    }

    override fun convertToEntityAttribute(dbData: String?): QueryReferenceType? {
        return dbData?.let { symbol ->
            QueryReferenceType.fromSymbol(symbol)
        }
    }
}
