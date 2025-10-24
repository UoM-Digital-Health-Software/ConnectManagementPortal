package org.radarbase.management.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.*
import org.radarbase.auth.authorization.RoleAuthority
import org.radarbase.management.ManagementPortalTestApp
import org.radarbase.management.domain.*
import org.radarbase.management.repository.PdfSummaryRequestRepository
import org.radarbase.management.repository.filters.UserFilter
import org.radarbase.management.security.Constants
import org.radarbase.management.service.dto.ProjectDTO
import org.radarbase.management.service.dto.SubjectDTO
import org.radarbase.management.service.dto.SubjectDTO.SubjectStatus
import org.radarbase.management.service.dto.UserDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import java.io.File
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
    @Autowired private val pdfSummaryRequestRepository: PdfSummaryRequestRepository
) {


    private lateinit var awsService: AWSService


    private val pdfSummaryRequestRepositoryMock: PdfSummaryRequestRepository = mock()
    private val bucketName = "test-bucket"
    private val folderPath = "manifests/test"

    @BeforeEach
    @Throws(ServletException::class)
    fun setUp() {
        awsService =
            spy(AWSService(
                pdfSummaryRequestRepositoryMock,
                mailService
            ))
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

        verify(awsService).sendSummaryReadyEmail(subjectObj.user, subjectObj)
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

    companion object {



    }
}
