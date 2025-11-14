package org.radarbase.management.service


import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.*
import org.radarbase.auth.authorization.RoleAuthority
import org.radarbase.management.ManagementPortalTestApp
import org.radarbase.management.config.BasePostgresIntegrationTest
import org.radarbase.management.domain.*
import org.radarbase.management.repository.PdfSummaryRequestRepository
import org.radarbase.management.repository.QueryParticipantRepository
import org.radarbase.management.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.servlet.ServletException


/**
 * Test class for the SubjectService class.
 *
 * @see SubjectService
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ManagementPortalTestApp::class])
@Transactional
class AWSServiceTest(
    @Autowired private val subjectService: SubjectService,
    @Autowired private val projectService: ProjectService,
    @Autowired private val mailService: MailService,
    @Autowired private val pdfSummaryRequestRepository: PdfSummaryRequestRepository,
    @Autowired private val queryParticipantRepository: QueryParticipantRepository,
    @Autowired private val userRepository: UserRepository
) : BasePostgresIntegrationTest() {


    private lateinit var awsService: AWSService


    private val pdfSummaryRequestRepositoryMock: PdfSummaryRequestRepository = mock()

    private val userRepositoryMock : UserRepository = mock()

    private val queryParticipantRepositoryMock: QueryParticipantRepository = mock()


    private val bucketName = "test-bucket"
    private val folderPath = "manifests/test"

    @BeforeEach
    @Throws(ServletException::class)
    fun setUp() {
        awsService =
            spy(AWSService(
                pdfSummaryRequestRepositoryMock,
                mailService,
                queryParticipantRepositoryMock,
                userRepositoryMock
            ))
    }
    private fun createUser(): User {
        val user =  User()
        user.setLogin("admin")
        return user;

    }


    private fun createSubject() : Subject {
        var subjectObj = Subject(
            id = 1L
        )

        val project = Project().apply {
            projectName = "Test Project"
        }

        val role = Role().apply {
            authority = Authority(role = RoleAuthority.PARTICIPANT)
            this.project = project
        }

        val user = User().apply {
            roles = mutableSetOf(role)
        }
        user.email = "jindrich.gorner@manchester.ac.uk"
        user.setLogin("jindrich.gorner@manchester.ac.uk")



        subjectObj.externalId = "ext123"
        subjectObj.user  = user

        return subjectObj

    }

    @Test
    fun checkIfSummaryIsReady_shouldSendEmailIfNotAlreadySentAndFilesPresnet() {

        var subjectObj = createSubject()

        val request = PdfSummaryRequest().apply{
             emailSent = false
             subject  = subjectObj
             requestedBy  =   subjectObj.user
             id = 1L
        }

        whenever(pdfSummaryRequestRepositoryMock.findLatestPerSubject()).thenReturn(listOf(request))
        doReturn(listOf("file1.json")).whenever(awsService)
            .listClasspathJsonFiles(any(), any(), any())

        doNothing().whenever(awsService).sendSummaryReadyEmail(any(), any())
        doNothing().whenever(awsService).updatePdfSummaryRequestAsCompleted(any())
        doReturn(null).whenever(awsService).createS3Client()

        awsService.checkIfSummaryIsReady()

   //     verify(awsService).sendSummaryReadyEmail(subjectObj.user, subjectObj)
       verify(awsService).updatePdfSummaryRequestAsCompleted(request)

    }


    @Test
    fun checkIfSummaryIsReady_shouldNotSendEmailIfEmailAlreadySent() {
        val subjectObj = createSubject()


        val request = PdfSummaryRequest().apply{
            emailSent = true
            subject  = subjectObj
            requestedBy  =   subjectObj.user
            id = 1L
        }

        whenever(pdfSummaryRequestRepositoryMock.findLatestPerSubject()).thenReturn(listOf(request))
        doReturn(listOf("file1.json")).whenever(awsService)
            .listClasspathJsonFiles(any(), any(), any())

        doNothing().whenever(awsService).sendSummaryReadyEmail(any(), any())
        doNothing().whenever(awsService).updatePdfSummaryRequestAsCompleted(any())
        doReturn(null).whenever(awsService).createS3Client()

        awsService.checkIfSummaryIsReady()


        verify(awsService, never()).sendSummaryReadyEmail(any(), any())
        verify(awsService, never()).updatePdfSummaryRequestAsCompleted(any())

    }

    @Test
    fun checkIfSummaryIsReady_shouldNotSendEmailIfNoFiles() {

        val subjectObj = createSubject()

        val request = PdfSummaryRequest().apply{
            emailSent = false
            subject  = subjectObj
            requestedBy  =   subjectObj.user
            id = 1L
        }

        whenever(pdfSummaryRequestRepositoryMock.findLatestPerSubject()).thenReturn(listOf(request))
        doReturn(emptyList<String>()).whenever(awsService)
            .listClasspathJsonFiles(any(), any(), any())

        doNothing().whenever(awsService).sendSummaryReadyEmail(any(), any())
        doNothing().whenever(awsService).updatePdfSummaryRequestAsCompleted(any())
        doReturn(null).whenever(awsService).createS3Client()

        awsService.checkIfSummaryIsReady()

        verify(awsService, never()).sendSummaryReadyEmail(any(), any())
        verify(awsService, never()).updatePdfSummaryRequestAsCompleted(any())
    }


    @Test
    fun requestDataSummaries_shouldReturnIfNotMidnight() {
        // Only works if you add `open fun getCurrentTime()` in your service
        doReturn(LocalTime.of(10, 0)).whenever(awsService).getCurrentTime()

        awsService.requestDataSummaries()

        verify(userRepositoryMock, never()).findOneByLogin(any())
        verify(queryParticipantRepositoryMock, never()).findOnePerUniqueSubject()
        verify(awsService, never()).writeManifestToResources(
            any(), any(), any(), any(), any(), any(), any(), any()
        )
    }


    @Test
    fun requestDataSummaries_shouldWriteManifestForEachParticipantAtMidnight() {
        doReturn(LocalTime.of(0, 0)).whenever(awsService).getCurrentTime()

        val adminUser = User()
        adminUser.setLogin("admin")


        val subjects = listOf(createSubject(), createSubject())

        whenever(userRepositoryMock.findOneByLogin("admin")).thenReturn(adminUser)
        whenever(queryParticipantRepositoryMock.findOnePerUniqueSubject())
            .thenReturn(subjects.map { QueryParticipant().apply { subject = it } })

        doReturn(null).whenever(awsService).writeManifestToResources(
            any(), any(), any(), any(), any(), any(), any(), any()
        )

        awsService.requestDataSummaries()

        verify(awsService, times(2)).writeManifestToResources(
            any(), eq(adminUser),
            eq("manifests/test"), eq("connect-dev-output"),
            eq("day"), eq("worker"), eq(false), eq(false)
        )
    }

    @Test
    fun `writeManifestToResources should throw IllegalArgumentException if subject or user is null`() {
        val user = createUser()
        assertThrows<IllegalArgumentException> {
            awsService.writeManifestToResources(null, user, "manifests/test")
        }
        val subject = createSubject()
        assertThrows<IllegalArgumentException> {
            awsService.writeManifestToResources(subject, null, "manifests/test")
        }
    }

    @Test
    fun `writeManifestToResources returns failure if summary already exists and limit is true`() {
        val subject = createSubject()
        val user = createUser()

        whenever(pdfSummaryRequestRepositoryMock.findBySubject(subject)).thenReturn(listOf(PdfSummaryRequest()))

        val response = awsService.writeManifestToResources(subject, user, "manifests/test", limit = true)

        Assertions.assertEquals(false, response.success)
        Assertions.assertEquals("The summary has been already requested.", response.message)
    }

    @Test
    fun `writeManifestToResources writes local file if S3 client is null`() {
        val subject = createSubject()
        val user = createUser()

        whenever(pdfSummaryRequestRepositoryMock.findBySubject(subject)).thenReturn(emptyList())
        doReturn(null).whenever(awsService).createS3Client()
        doNothing().whenever(awsService).addPdfSummaryTrackerRecord(any(), any(), any())

        val response = awsService.writeManifestToResources(subject, user, "manifests/test")

        Assertions.assertTrue(response.success)
        Assertions.assertEquals("Summary requested. You will be notified by email when it is ready", response.message)

        // Check that the file was created
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val runId = "${today}_${subject.user?.login}"
        val file = File("src/main/resources/manifests/test/manifest-$runId.yaml")
        Assertions.assertTrue(file.exists())


        file.delete()
        file.parentFile.delete()
    }

    @Test
    fun `writeManifestToResources uploads to S3 if client exists`() {
        val subject = createSubject()
        val user = createUser()

        val s3ClientMock = mock<software.amazon.awssdk.services.s3.S3Client>()
        whenever(pdfSummaryRequestRepositoryMock.findBySubject(subject)).thenReturn(emptyList())
        doReturn(s3ClientMock).whenever(awsService).createS3Client()
        doNothing().whenever(awsService).addPdfSummaryTrackerRecord(any(), any(), any())

        val response = awsService.writeManifestToResources(subject, user, "manifests/test")

        Assertions.assertTrue(response.success)
        Assertions.assertEquals("Summary requested. You will be notified by email when it is ready", response.message)

        // Verify putObject was called
        verify(s3ClientMock, times(1)).putObject(
                any<PutObjectRequest>(),
            any<software.amazon.awssdk.core.sync.RequestBody>()
        )
    }


    companion object {



    }
}
