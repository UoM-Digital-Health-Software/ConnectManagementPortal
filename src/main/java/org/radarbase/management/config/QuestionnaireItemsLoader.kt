package org.radarbase.management.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component


@JsonIgnoreProperties(ignoreUnknown = true)
data class QuestionanireField(
    val field_name: String,
    val field_label: String,
    val field_sublabel: String?,
    val field_type: String,
    val group_name: String,
    val select_choices_or_calculations: List<SelectChoice>?
)

data class SelectChoice (
    val code: String,
    val label: String
)


@Component
class QuestionnaireItemsLoader {

    val questionnaireItems: List<QuestionanireField> = loadQuestionnaireItems("questionnaire.json")
    val delusionsItems : List<QuestionanireField> = loadQuestionnaireItems("delusions.json")

    private fun loadQuestionnaireItems(fileName: String): List<QuestionanireField> {
        val resource = ClassPathResource(fileName)
        val mapper = jacksonObjectMapper()
        resource.inputStream.use { input ->
            return mapper.readValue(input)
        }
    }




}
