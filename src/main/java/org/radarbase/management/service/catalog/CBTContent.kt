package org.radarbase.management.service.catalog


data class CBTContent(
    val name: String,
    val questionSourceUUID: String,
    val version: String,
    val conditionalResponses: List<ConditionalResponse>
)

data class ConditionalResponse(
    val route: String,
    val items: List<ContentItem>
)

data class ContentItem(
    val index: String,
    val type: String,
    val text: String
)
