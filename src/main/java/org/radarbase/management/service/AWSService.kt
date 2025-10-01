package org.radarbase.management.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.net.HttpURLConnection
import java.net.URL
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

import com.fasterxml.jackson.module.kotlin.readValue


import org.radarbase.management.domain.PdfSummaryRequest
import org.radarbase.management.domain.Subject
import org.radarbase.management.domain.User
import org.radarbase.management.repository.PdfSummaryRequestRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.io.*
import java.lang.IllegalArgumentException
import java.nio.charset.StandardCharsets
import java.time.*
import java.time.format.DateTimeFormatter

enum class DataSource {
    S3, CLASSPATH
}
@JsonIgnoreProperties(ignoreUnknown = true)
data class S3JsonData(
    val patient_id: String,
    val site: String,
    val data_summary: DataSummary,
    val feature_statistics: Map<String, FeatureStatistics>,
    val questionnaire_responses: QuestionnaireResponses
)
data class ApiResponse(
    val success: Boolean,
    val message: String
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DataSummary(val start_date: String?, val end_date: String?, val total_days_with_data: Int)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeatureStatistics(val mean: Double?, val total_responses: Double?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class QuestionnaireResponses(
    val total_responses: Int,
    val days_with_responses: Int,
    val slider: Map<String, SliderResponses>,
    val histogram:  Histogram)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SliderResponses(
    val total_entries: Double,
    val mean: Double)


// OUTPUT
data class DataSummaryResult(
    var data: MutableMap<String, DataSummaryCategory>,
    val allSlider: MutableList<String>,
    val allHistogram: MutableList<String>,
    val allPhysical: MutableList<String>,

)

data class DataSummaryCategory(
    var physical:  MutableMap<String, Double>,
    var questionnaire_total:  Double,
    var questionnaire_slider: MutableMap<String,  Double>,
    var histogram: HistogramResponse,


)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Histogram(
    val social: Map<String, Map<String, Int>> = emptyMap(),
    val whereabouts: Map<String, Map<String, Int>> = emptyMap(),
    val sleep: Map<String, Map<String, Int>> = emptyMap())


@JsonIgnoreProperties(ignoreUnknown = true)
data class HistogramResponse(
    val social: MutableMap<String,  Int>,
    val whereabouts: MutableMap<String,  Int>,
    val sleep: MutableMap<String,  Int>)



@Service
class AWSService(
    @Autowired private val pdfSummaryRequestRepository: PdfSummaryRequestRepository,
    @Autowired private val mailService: MailService,
    @Value("\${test.dataSource:}") private val dataSourceTest: String) {
    private val log = LoggerFactory.getLogger(javaClass)
    private var s3AsyncClient: S3AsyncClient? = null
    private var bucketName: String = "connect-uom"
    private var folderPath = "summary-data/"
    var region = Region.of("eu-west-2")




    private fun createS3Client() : S3Client?  {
        val region = Region.EU_WEST_2
        val s3Client: S3Client? = runCatching {
            val client = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()

            // running the listbuckets to test if credentials exists
            client.listBuckets()
            client
        }.getOrNull()


        return s3Client
    }



    private fun extractDateFromFile(prefix: String): LocalDate {
        val folderName = prefix.removePrefix("summary-data/").removeSuffix("/")
        val datePart = folderName.substringBefore("_")
        return LocalDate.parse(datePart, DateTimeFormatter.ISO_DATE)
    }


    fun startProcessing(projectName: String, login: String, dataSource: DataSource) : DataSummaryResult? {
        val s3Client = createS3Client()

        val files = if (s3Client != null) {
            listS3JsonFiles(s3Client, bucketName, folderPath, login, projectName)
        } else {
            log.info("[AWS] getting files through classpath")
            listClasspathJsonFiles(folderPath, login, projectName)
        }


        if(files.isEmpty()) {
            return null;
        }

        return processJsonFiles(s3Client, bucketName, files, dataSource)
    }

    fun listS3JsonFiles(s3Client: S3Client, bucket: String, prefix: String, userId: String, projectName: String): List<String> {
        val request = ListObjectsV2Request.builder()
            .bucket(bucket)
            .prefix(prefix)
            .delimiter("/") // ensures we only list top-level folders
            .build()


        val response = s3Client.listObjectsV2(request)

        val matchingFolders = response.commonPrefixes()
            .map { it.prefix() }
            .filter { it.endsWith("_$userId/") }




        if (matchingFolders.isEmpty()) {
            log.info("[AWS] there is no matching folders" )
            return emptyList()
        }


        val latestFolder = matchingFolders
            .maxByOrNull { extractDateFromFile(it) }!!


        log.info("[AWS] latest folder {}", latestFolder )

        val filesRequest = ListObjectsV2Request.builder()
            .bucket(bucket)
            .prefix("$latestFolder$projectName/$userId")
            .build()

        log.info("[AWS] latest folder url {}",         "$latestFolder/$projectName/$userId" )



        val filesResponse = s3Client.listObjectsV2(filesRequest)
        log.info("[AWS] number of files {}", filesResponse.contents().size )


        return filesResponse.contents().map(S3Object::key)
            .filter { it != prefix } // Exclude the folder itself
    }
    fun listClasspathJsonFiles(prefix: String, userId: String, projectName: String) : List<String> {
        val testUserId = userId

        val classLoader =  Thread.currentThread().contextClassLoader
        val resource = classLoader.getResource(prefix) ?: return emptyList()
        val allFolders = java.io.File(resource.toURI())
        log.info("[AWS] prefi {}", prefix)

        val userFolders = allFolders
            .listFiles { file -> file.isDirectory }  // only keep directories
            ?.map { it.name }                        // get just the folder names
            ?.filter { it.endsWith("_$testUserId") } // filter by suffix
            ?: emptyList()

        log.info("[AWS] userFolders {}", userFolders)

        val latestFolder = userFolders?.maxByOrNull {  extractDateFromFile(it) }  ?: return emptyList()  // or handle the case gracefully

        log.info("[AWS] latestFolder {}", latestFolder)

        val files  = classLoader.getResource("$prefix$latestFolder/$projectName/$testUserId") ?: return emptyList()

        log.info("[AWS] files {}", files)

        val allFiles = java.io.File(files.toURI())

        return  allFiles.list()?.map  {
            "$prefix$latestFolder/$projectName/$testUserId/$it"
        }  ?: emptyList()

    }


    private fun updatePdfSummaryRequestAsCompleted(pdfSummaryRequest: PdfSummaryRequest) {
        pdfSummaryRequest.emailSent = true
        pdfSummaryRequest.summaryCreatedOn = ZonedDateTime.now()
        pdfSummaryRequestRepository.save(pdfSummaryRequest)
    }

    fun sendSummaryReadyEmail(user: User?, subject: Subject?) {

        mailService.sendEmail(
            user?.email,
            "CONNECT PDF Summary is ready: ${subject?.externalId}",
            "Please login to the CONNECT Management Portal to download the PDF summary",
            false,
            false
        )

    }
    @Scheduled(cron = "0 * * * * ?")
    fun checkIfSummaryIsReady() {
        val s3Client = createS3Client()

        val latestRequests = pdfSummaryRequestRepository.findLatestPerSubject();
        latestRequests.forEach {

            log.info("[PDF-WORKER] Checking for {}", it.summaryId)
            if (!it.emailSent) {

                val subject = it.subject
                val login = subject?.user?.login
                val projectName = subject?.activeProject?.projectName

                var files: List<String> = listOf()

                if (login != null && projectName != null) {
                    files = if(s3Client != null) {
                        listS3JsonFiles(s3Client, bucketName, folderPath, login, projectName)
                    } else {
                        listClasspathJsonFiles(folderPath, login, projectName)
                    }

                } else {
                    log.info("[PDF-WORKER] No login or projectName for {}", it.id)
                }

                if (files.isNotEmpty()) {
                    log.info("[PDF-WORKER] Sending email for {}", subject?.externalId)
                    sendSummaryReadyEmail(it.requestedBy, it.subject)
                    updatePdfSummaryRequestAsCompleted(it);
                } else {
                    log.info("[PDF-WORKER] no files for {}", subject?.externalId)
                }
            }
        }
    }


    fun processJsonFiles(
       Client: S3Client?,
        bucket: String,
        fileKeys: List<String>,
        dataSource: DataSource
    ): DataSummaryResult {
        val jsonMapper = jacksonObjectMapper()
        val monthlyAverages = mutableMapOf<String, MutableMap<String, MutableList<Double>>>()
        val dataSummaryResult = DataSummaryResult(
            data = mutableMapOf(),
            allHistogram = mutableListOf(),
            allSlider = mutableListOf(),
            allPhysical = mutableListOf()
        )
        log.info("filekeys ${fileKeys}")



        for (key in fileKeys) {
            log.info("current key is ${key}")


            if(key.contains(".DS_Store")) {
                continue
            }

            val jsonString = if (Client != null) {
                downloadS3Json(Client, bucket, key)
            } else {
                readClassPathJson(key)
            }


            // gets the JSON from the file and reads it into a variable
            val jsonData: S3JsonData = jsonMapper.readValue(jsonString)
            val month = extractMonthFromFilename(key) // Extract month from filename


            // creates an empty object that will be filled up with datat
            val dataSummaryCategory = DataSummaryCategory(
                physical = mutableMapOf<String,  Double>(),
                questionnaire_total = 0.0,
                questionnaire_slider = mutableMapOf(),
                histogram = HistogramResponse(
                    social = mutableMapOf(),
                    whereabouts = mutableMapOf(),
                    sleep = mutableMapOf()


                ),

            )

            // puts a month in based on the file name
            dataSummaryResult.data
                .getOrPut(month) { dataSummaryCategory }


            // goes through the physical statistics (heart_rate , steps etc) and gets the mean value
            // and saves it
            jsonData.feature_statistics.forEach { (feature, stats) ->
                var mean : Double = 0.0;
                if(stats.mean != null) {
                    mean = stats.mean
                } else if (stats.total_responses != null) {
                    mean = stats.total_responses
                }


                //monthlyAverages not used anymore I think ?
                monthlyAverages
                    .getOrPut(month) { mutableMapOf() }
                    .getOrPut(feature) { mutableListOf() }
                    .add(mean)

                // this is where it puts steps: 3.5 as an exmaple
                dataSummaryCategory.physical
                             .getOrPut(feature){ mean }

            }

            // gets the questionnare_total per month
            dataSummaryCategory.questionnaire_total = jsonData.questionnaire_responses.days_with_responses.toDouble()


            // gets the questionnaire categories ( same principle as for physical ones)
            jsonData.questionnaire_responses.slider.forEach{ (feature, stats) ->
                val totalNumber =  stats.mean;

                dataSummaryCategory.questionnaire_slider
                    .getOrPut(feature){ totalNumber }
            }


            // the next three is hardcoded to get the histograms

            val social = jsonData.questionnaire_responses.histogram.social.get("social_1")
             if (social != null) {

                 social.forEach { (feature, stats) ->

                     val key = feature.toDouble().toInt()
                     var value = dataSummaryCategory.histogram.social.get(key.toString())

                     if (value == null) {
                         dataSummaryCategory.histogram.social.put(key.toString(), stats)
                     } else {
                         value += stats
                         dataSummaryCategory.histogram.social.put(key.toString(), value)
                     }
                 }
             }

            val whereabouts = jsonData.questionnaire_responses.histogram.whereabouts["whereabouts_1"]
            if (whereabouts != null) {

                whereabouts.forEach { (feature, stats) ->
                    val key = feature.toDouble().toInt()
                    var value = dataSummaryCategory.histogram.whereabouts[key.toString()]

                    if (value == null) {
                        dataSummaryCategory.histogram.whereabouts.put(key.toString(), stats)
                    } else {
                        value += stats
                        dataSummaryCategory.histogram.whereabouts.put(key.toString(), value)
                    }
                }
            }

            val sleep = jsonData.questionnaire_responses.histogram.sleep["sleep_5"]
            if (sleep != null) {

                sleep.forEach { (feature, stats) ->

                    var value = dataSummaryCategory.histogram.sleep[feature]

                    if (value == null) {
                        dataSummaryCategory.histogram.sleep.put(feature, stats)
                    } else {
                        value += stats
                        dataSummaryCategory.histogram.sleep.put(feature, value)
                    }
                }
            }

            for ((summaryKey, summaryValue) in dataSummaryResult.data) {

                for((sliderKey, sliderValue) in summaryValue.questionnaire_slider) {
                   if(sliderKey !in dataSummaryResult.allSlider) {
                       dataSummaryResult.allSlider.add(sliderKey)
                   }
                }

                for((sliderKey, sliderValue) in summaryValue.physical) {
                    if(sliderKey !in dataSummaryResult.allPhysical) {
                        dataSummaryResult.allPhysical.add(sliderKey)
                    }
                }
            }
        }

        return dataSummaryResult
    }

    fun extractMonthFromFilename(filename: String): String {
        val regex = """_(\d{4}-\d{2})""".toRegex() // Looks for _YYYY-MM in filename
        return regex.find(filename)?.groupValues?.get(1) ?: "unknown"
    }

    fun downloadS3Json(s3Client: S3Client, bucket: String, key: String): String {
        val request = GetObjectRequest.builder().bucket(bucket).key(key).build()
        s3Client.getObject(request).use { objStream ->
            return BufferedReader(InputStreamReader(objStream)).readText()
        }
    }

    fun readClassPathJson(filePath: String) : String {
        val classLoader = Thread.currentThread().contextClassLoader
        val inputStream = classLoader.getResourceAsStream(filePath)
            ?: throw IllegalArgumentException("File not found: $filePath")

        return inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }


    fun writeManifestToResources(
        subject: Subject?,
        currentUser: User?,
        resourceFolderPath: String,
        createdBy: String = "system",
        local: Boolean = false
    ) : ApiResponse {


        if(subject == null || currentUser == null) {
           throw IllegalArgumentException("Subject or current user is not present")
        }

        val userId = subject.user?.login

        val bucket: String = "connect-uom"

        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val runId = "${today}_${userId}"
        val createdAt = Instant.now().toString()


        // check if

        val existingPdfSummary = pdfSummaryRequestRepository.findBySummaryIdAndSubject(runId, subject);

        if(existingPdfSummary != null) {
            return ApiResponse(success = false, message = "Please wait 24 hours before requesting another summary")

        }

        // Load template from resources
        val templateStream = this::class.java.getResourceAsStream("/manifest-template.yaml")
            ?: throw IllegalStateException("manifest-template.yaml not found in resources")


        log.info("[AWS] we have the stream")
        val template = templateStream.bufferedReader().use { it.readText() }

        // Fill placeholders
        val participantsYaml = "- $userId"
        val yamlContent = template
            .replace("{{RUN_ID}}", runId)
            .replace("{{CREATED_BY}}", createdBy)
            .replace("{{CREATED_AT}}", createdAt)
            .replace("{{PARTICIPANTS}}", participantsYaml)

        val s3Client =  createS3Client()

        if(s3Client == null) {
            val outputDir = File("src/main/resources/$resourceFolderPath")

            log.info("[AWS] making directory")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val file = File(outputDir, "manifest-$runId.yaml")
            file.writeText(yamlContent, StandardCharsets.UTF_8)
            addPdfSummaryTrackerRecord(subject, currentUser, runId)
            return ApiResponse(success = true, message = "Summary requested. You will be notified by email when it is ready")

        } else {
            try {
                val key = "run-specs/tmp/manifest-$runId.yaml"

                val request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/x-yaml")
                    .build()


                log.info("[AWS] request build {}", request)

                s3Client.putObject(
                    request,
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(
                        yamlContent.toByteArray(StandardCharsets.UTF_8)
                    )
                )


                addPdfSummaryTrackerRecord(subject, currentUser, runId)
                return ApiResponse(success = true, message = "Summary requested. You will be notified by email when it is ready")

            } catch(e: Exception) {
                log.error("[AWS] the summary failed creation failed for user {}, and subject: {}, with error {}", currentUser.id, subject.id, e.message)
                throw Exception("Summary file could not be created. Please try again or contact support")

            }
        }


        return ApiResponse(success = false, message = "Something went wrong...")

    }

    private fun addPdfSummaryTrackerRecord(subject: Subject,requestedBy: User, summaryId: String, ) {
        val newPdfSummaryTracker = PdfSummaryRequest()

        newPdfSummaryTracker.summaryId = summaryId
        newPdfSummaryTracker.requestedBy = requestedBy
        newPdfSummaryTracker.requestedOn = ZonedDateTime.now()
        newPdfSummaryTracker.subject =  subject

        pdfSummaryRequestRepository.saveAndFlush(newPdfSummaryTracker)
    }

}
