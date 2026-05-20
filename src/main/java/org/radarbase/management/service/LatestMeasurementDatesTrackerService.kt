package org.radarbase.management.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.joda.time.LocalDate
import org.radarbase.management.domain.ConnectDataLogAWS
import org.radarbase.management.domain.LatestMeasurementDatesTracker
import org.radarbase.management.domain.User
import org.radarbase.management.repository.*
import org.radarbase.management.web.rest.util.Topics
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


@Service
@Transactional
class LatestMeasurementDatesTrackerService(
    @Autowired private val awsService: AWSService,
    @Autowired  private val subjectRepository: SubjectRepository,
    @Autowired private val latestMeasurementDatesTrackerRepository: LatestMeasurementDatesTrackerRepository,
    @Autowired private val connectDataLogAWSRepository: ConnectDataLogAWSRepository
) {
    private var lastMeasurementDataFolder = "fitbit-latest-measurement-dates"
    private val log = LoggerFactory.getLogger(javaClass)
    private var bucket: String = "connect-uom"


    fun loadLatestMeasurementData(latestMeasurementDatesFolder: LatestMeasurementDatesTracker) {
        val classLoader =  Thread.currentThread().contextClassLoader
        val jsonMapper = jacksonObjectMapper()
        val s3Client = awsService.createS3Client()

        val sitesList = listOf("Cardiff", "Edinburgh", "Glasgow", "KCL", "Manchester", "Sussex")


        if(s3Client == null) {
            log.error("[getLatestMeasurementData] s3client is null, cannot continue")
            return
        }

        sitesList.forEach { site ->
            val sitePrefix = "fitbit-latest-measurement-dates/${latestMeasurementDatesFolder.summaryId}/latest-measurement-data/$site/"

            log.info("[loadLatestMeasurementData] site prefix {}", sitePrefix)

            val request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(sitePrefix)
                .delimiter("/")
                .build()

            val response = s3Client.listObjectsV2(request)


            response.commonPrefixes().forEach { commonPrefix ->

                val folderPath = commonPrefix.prefix()
                val subjectId = folderPath.dropLast(1).substringAfterLast("/")
                val jsonKey = "${folderPath}${subjectId}_latest_measurement_dates.json"

                log.info("[loadLatestMeasurementData] jsonKey: {}", jsonKey)

                val getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(jsonKey)
                    .build()

                try {
                    s3Client.getObject(getRequest).use { objStream ->
                        val jsonContent = objStream.bufferedReader().readText()
                        if(jsonContent.isNotEmpty()){
                            val jsonData: LatestMeasurementDates = jsonMapper.readValue(jsonContent)


                            Topics.values().forEach {
                                val result = jsonData.measures.get(it.toString())?.last_measurement_date


                                if(result != null) {
                                    addConnectDataLog(subjectId ,result, it.toString(), site)
                                }
                            }
                        }
                        log.info("[loadLatestMeasurementData]Successfully read data for subject: $subjectId")
                    }
                } catch (e: Exception) {
                    log.error("[[loadLatestMeasurementData]] Error downloading $jsonKey: ${e.message}")
                }
            }
        }

        latestMeasurementDatesFolder.loaded = true

        latestMeasurementDatesTrackerRepository.saveAndFlush(latestMeasurementDatesFolder)
    }


    private fun readLocalResource(latestMeasurementDatesFolder:  LatestMeasurementDatesTracker){
        val jsonMapper = jacksonObjectMapper()

        val allSubjects =  subjectRepository.findAll()

        allSubjects.forEach {
            val project = it.activeProject?.projectName
            val userId = it.user?.login

            if(!userId.isNullOrEmpty() && !project.isNullOrEmpty()) {
                val path = "$lastMeasurementDataFolder/${latestMeasurementDatesFolder.summaryId}/$project/$userId"

                log.info("[LAST_DATA] path : {}", path)

                try {
                    val customFilePath = "$path/${userId}_latest_measurement_dates.json"
                    val jsonString = awsService.readClassPathJson(customFilePath)

                    if(jsonString.isNotEmpty()){
                        val jsonData: LatestMeasurementDates = jsonMapper.readValue(jsonString)


                        Topics.values().forEach {
                            val result = jsonData.measures.get(it.toString())?.last_measurement_date


                            if(result != null) {
                                addConnectDataLog(userId,result, it.toString(), project)
                            }
                        }
                    }

                }
                catch (e: Exception) {
                    log.error("[LAST_DATA] Failed to process user $userId")
                }
            }
        }

    }

    fun getLatestProcessedManifest(): LatestMeasurementDatesTracker? {
        val latestManifest =  latestMeasurementDatesTrackerRepository.findFirstByGeneratedTrueAndLoadedTrueOrderByRequestedOnDesc()
        return latestManifest
    }


    private fun addLatestMeasurementDatesTracker(summaryId: String) {

        val latestMeasurementDatesTracker = LatestMeasurementDatesTracker()
        latestMeasurementDatesTracker.summaryId = summaryId
        latestMeasurementDatesTracker.generated = false
        latestMeasurementDatesTracker.loaded = false
        latestMeasurementDatesTracker.requestedOn = ZonedDateTime.now()

        latestMeasurementDatesTrackerRepository.saveAndFlush(latestMeasurementDatesTracker)
    }
    fun writeLatestMeasurementsManifest(

        currentUser: User?,
        resourceFolderPath: String,
        createdBy: String = "system",
        local: Boolean = false
    ) : ApiResponse {
       val local = true // remove

        if( currentUser == null) {
            throw IllegalArgumentException("Subject or current user is not present")
        }

        val bucket: String = "connect-uom"

        val today = java.time.LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val summaryId = "${today}_latest-measurement-dates"
        val createdAt = java.time.Instant.now().toString()

        val existingManifest = latestMeasurementDatesTrackerRepository.findBySummaryId(summaryId)

        if(existingManifest.isNotEmpty()) {
            log.info("[AWS] already requested")

            return ApiResponse(success = false, message = "The summary has been already requested.")
        }

        val templateStream = this::class.java.getResourceAsStream("/latest-dates-manifest-template.yaml")
            ?: throw IllegalStateException("manifest-template.yaml not found in resources")

        val template = templateStream.bufferedReader().use { it.readText() }

        val yamlContent = template
            .replace("{{RUN_ID}}", summaryId)
            .replace("{{CREATED_BY}}", createdBy)
            .replace("{{CREATED_AT}}", createdAt)

        val s3Client =  awsService.createS3Client()



        if(local) {
            val outputDir = File("src/main/resources/$resourceFolderPath")

            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val file = File(outputDir, "manifest-$summaryId.yaml")
            file.writeText(yamlContent, StandardCharsets.UTF_8)
            addLatestMeasurementDatesTracker(summaryId)
            return ApiResponse(success = true, message = "Summary requested.")

        }


        if(s3Client == null) {

            log.error("[AWS] s3 client is null")

            return ApiResponse(success = false, message = "Summary requested.")

        }

        try {
                val key = "run-specs/pending/manifest-$summaryId.yaml"

                val request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/x-yaml")
                    .build()


                val result = s3Client.putObject(
                    request,
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(
                        yamlContent.toByteArray(StandardCharsets.UTF_8)
                    )
                )
                addLatestMeasurementDatesTracker(summaryId)
                return ApiResponse(success = true, message = "Summary requested.")

            } catch(e: Exception) {
                log.error("[AWS] the summary failed creation failed for user {}, with error {}", currentUser.id,  e.message)
                throw Exception("Summary file could not be created. Please try again or contact support")
            }

        return ApiResponse(success = false, message = "Something went wrong...")
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun addConnectDataLog(userId: String, date: String, topic: String, projectName: String) {
        try {
            val connectDataLogAWS = ConnectDataLogAWS()

            connectDataLogAWS.createdAt = java.time.Instant.now()
            connectDataLogAWS.userId = userId
            connectDataLogAWS.time = LocalDate.parse(date).toDate().toInstant()
            connectDataLogAWS.dataGroupingType = topic
            connectDataLogAWS.projectId = projectName

            connectDataLogAWSRepository.saveAndFlush(connectDataLogAWS)
        } catch (e: Exception) {
            log.error("Failed to save log for user $userId: ${e.message}")
        }
    }




    @Scheduled(cron = "0 0 * * * ?")
    fun checkAndProcessLatestMeasurementsDates(){
        val s3Client = awsService.createS3Client()
       val datesTracker =  latestMeasurementDatesTrackerRepository.findFirstByGeneratedFalseOrderByRequestedOnDesc()

        if(s3Client == null) {
            return
        }

        datesTracker?.let {
            log.info("[checkAndProcessLatestMeasurementsDates] dates tracker id {}", it.summaryId)

            val folderExists = folderExists(s3Client, it.summaryId.toString())

            if(folderExists) {
                it.generated = true
                latestMeasurementDatesTrackerRepository.saveAndFlush(it)

                loadLatestMeasurementData(it)
            }
        }
    }



    // move to awsservice

    fun folderExists(s3Client: S3Client, folderTarget: String): Boolean {
        val sanitizedTarget = folderTarget.trim('/')

        val prefix = "fitbit-latest-measurement-dates/$sanitizedTarget/"

        val request = ListObjectsV2Request.builder()
            .bucket(bucket)
            .prefix(prefix)
            .maxKeys(1)
            .build()

        val response = s3Client.listObjectsV2(request)

        return response.hasContents() || response.hasCommonPrefixes()
    }
}
