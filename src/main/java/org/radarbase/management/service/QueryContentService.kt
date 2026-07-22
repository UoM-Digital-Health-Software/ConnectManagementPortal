package org.radarbase.management.service


import org.radarbase.management.domain.*
import org.radarbase.management.domain.enumeration.CbtRouteSelectionMode
import org.radarbase.management.domain.enumeration.ContentGroupStatus
import org.radarbase.management.domain.enumeration.ContentType
import org.radarbase.management.repository.*
import org.radarbase.management.service.dto.ModuleGroupDTO
import org.radarbase.management.service.dto.NotificationDTO
import org.radarbase.management.service.dto.QueryContentDTO
import org.radarbase.management.service.dto.QueryContentGroupDTO

import org.radarbase.management.service.mapper.QueryContentGroupMapper
import org.radarbase.management.service.mapper.QueryContentMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*


@Service
@Transactional
class QueryContentService(
    private val queryContentRepository: QueryContentRepository,
    private val queryGroupRepository: QueryGroupRepository,
    private val queryContentMapper: QueryContentMapper,
    private val queryContentGroupRepository: QueryContentGroupRepository,
    private val queryParticipantRepository: QueryParticipantRepository,
    private val subjectRepository: SubjectRepository,
    private val queryEvaluationRepository: QueryEvaluationRepository,
    private val queryParticipantContentRepository: QueryParticipantContentRepository,
    private val queryContentGroupMapper: QueryContentGroupMapper,
    private val moduleRepository: ModuleRepository,
    private val notificationService: NotificationService,
    private val contentNotificationRepository: ContentNotificationRepository,
    private val cbtContentService: CBTContentService
) {

    fun convertImgStringToByteArray(imgString: String): ByteArray {
        val decoder = Base64.getDecoder()
        val partSeparator = ","
        var encodedImg = imgString
        if (encodedImg.contains(partSeparator)) {
            encodedImg = encodedImg.split(partSeparator)[10]
        }
        return decoder.decode(encodedImg.toByteArray(StandardCharsets.UTF_8))
    }

    @Transactional
    fun saveAllOrUpdate(contentGroupDTO: QueryContentGroupDTO): Long? {
        val decoder = Base64.getDecoder()

        val queryGroup = queryGroupRepository.findById(
            contentGroupDTO.queryGroupId
                ?: throw Exception("Missing queryGroupId")
        ).orElseThrow { Exception("Query group not found") }

        val contentGroup = if (contentGroupDTO.id != null) {
            val existingGroup = queryContentGroupRepository.findById(contentGroupDTO.id)
                .orElseThrow { Exception("Content group with id ${contentGroupDTO.id} does not exist") }

            var updated = false

            if (existingGroup.contentGroupName != contentGroupDTO.contentGroupName) {
                existingGroup.contentGroupName = contentGroupDTO.contentGroupName
                updated = true
            }


            if (contentGroupDTO.status != null && existingGroup.status != contentGroupDTO.status) {
                updateContentGroupStatus(contentGroupDTO.status!!, existingGroup)
                updated = true
            }

            if (updated) {
                existingGroup.updatedDate = ZonedDateTime.now()
                queryContentGroupRepository.saveAndFlush(existingGroup)
            }

            existingGroup.contentItems.clear()
            queryContentGroupRepository.save(existingGroup)




            val oldContentsAgain = queryContentRepository.findAllByQueryContentGroupId(existingGroup.id!!)

            existingGroup
        } else {
            queryContentGroupRepository.save(
                QueryContentGroup().apply {
                    this.queryGroup = queryGroup
                    this.contentGroupName = contentGroupDTO.contentGroupName
                    this.status = contentGroupDTO.status ?: ContentGroupStatus.INACTIVE // default if not provided
                    this.createdDate = ZonedDateTime.now()
                    this.updatedDate = ZonedDateTime.now()
                }
            )
        }

        contentGroupDTO.queryContentDTOList?.forEach { dto ->
            val queryContent = QueryContent().apply {
                this.queryGroup = queryGroup
                this.queryContentGroup = contentGroup
                this.type = dto.type

                if (this.type == ContentType.IMAGE) {
                    this.imageBlob = decoder.decode(dto.imageBlob)
                } else if (this.type == ContentType.CBT_CONTENT) {
                    this.cbtRoute = dto.cbtRoute;
                    this.cbtType = dto.cbtType?.name;
                    this.cbtVersion = dto.cbtVersion
                    if(dto.cbtRouteSelectionMode != null) {
                        this.cbtRouteSelectionMode =   CbtRouteSelectionMode.valueOf(dto.cbtRouteSelectionMode!!)
                    }
                }
                else {
                    this.value = dto.value
                    this.heading = dto.heading
                }
                this.resourceId = dto.resourceId
            }

            queryContentRepository.save(queryContent)
        }

        return contentGroup.id
   }


    fun findAllContentsByQueryGroupId(queryGroupId: Long): List<QueryContentDTO> {
        val queryContentList = queryContentRepository.findAllByQueryGroupId(queryGroupId)
        return queryContentList.mapNotNull { queryContentMapper.queryContentToQueryContentDTO(it) }
    }

    fun getAllContentGroupsWithContentsQueryGroupId(queryGroupId: Long): List<QueryContentGroupDTO> {
        val contentGroups = queryContentGroupRepository.findAllByQueryGroupIdAndIsArchivedFalse(queryGroupId)

        return contentGroups.map { group ->
            val queryContents = queryContentRepository.findAllByQueryContentGroupId(group.id!!)
            val contentDTOs = queryContents.mapNotNull {
                queryContentMapper.queryContentToQueryContentDTO(it)
            }
            QueryContentGroupDTO().apply {
                contentGroupName = group.contentGroupName
                this.queryGroupId = queryGroupId
                queryContentDTOList = contentDTOs
                id = group.id
                status = group.status
            }
        }
    }


    fun archiveQueryContentGroup(queryContentGroupId: Long) {
        val queryContentGroup = queryContentGroupRepository.findById(queryContentGroupId).get()
        queryContentGroup.isArchived = true

        val participantContentGroupList = queryParticipantContentRepository.findByQueryContentGroupIdAndIsArchivedFalse(queryContentGroupId)

        for(participantContent in participantContentGroupList) {
            participantContent.isArchived = true
            queryParticipantContentRepository.save(participantContent)
        }
        queryContentGroupRepository.save(queryContentGroup)

        queryContentGroupRepository.flush()
        queryParticipantContentRepository.flush()
    }


    fun scheduleNotification(contentGroup: QueryContentGroup?, latestEvaluation: QueryEvaluation, subject: Subject) {


        contentGroup?.let {
            val user = subject.user ?: throw IllegalArgumentException("[QueryContentService][SendNotification]User is not present")
            val contentGroupName = contentGroup?.contentGroupName

            val notificationDTO = NotificationDTO().apply {
                title = contentGroupName
                body = "Click here to read more"
                route = "/queryContent/" + contentGroup.id
            }
            notificationService.sendNotification(listOf(user), notificationDTO)

            latestEvaluation.notificationScheduled = true
            queryEvaluationRepository.save(latestEvaluation)
        }
    }

     fun shouldSendNotification(evaluations : List<QueryEvaluation>, latestNotificationDate: ZonedDateTime?): Boolean {


         if(evaluations.isEmpty()) return false

         if(evaluations.first().result != true) return false

         val hasPreviousTrue = evaluations.drop(1).any { it.result == true }

         if (evaluations.first().result == true && !hasPreviousTrue) return true

         var consecutiveFailed = 0

         for(i in 1 until evaluations.size) {

             if(evaluations[i].result == false) {
                 consecutiveFailed++
             } else {
                break
             }
         }


         if(consecutiveFailed >= QueryEvaluationOptions.resetThresholdDays) {
            return true
         }

         if (latestNotificationDate != null) {
             val today = ZonedDateTime.now()
            val daysSinceLast = ChronoUnit.DAYS.between(latestNotificationDate, today)

             if (daysSinceLast >= QueryEvaluationOptions.minNotificationIntervalDays) {
                return true
             }
         }

         return false
    }

    private fun saveParticipantContentGroup(queryGroup: QueryGroup, queryContentGroup: QueryContentGroup, subject: Subject) : QueryParticipantContent {
        val participantContentGroup = QueryParticipantContent()
        participantContentGroup.queryContentGroup = queryContentGroup
        participantContentGroup.queryGroup = queryGroup
        participantContentGroup.subject = subject
        participantContentGroup.createdDate = ZonedDateTime.now();
        participantContentGroup.isArchived = false;

        return queryParticipantContentRepository.saveAndFlush(participantContentGroup);
    }




    fun getRandomAlreadyAssignedContent(queryGroup: QueryGroup, subject: Subject): QueryContentGroup? {
        val queryGroupId = queryGroup.id ?: return null

        val assignedContentGroups = queryParticipantContentRepository.findBySubjectAndQueryGroupAndIsArchivedFalse(subject, queryGroup).map { it.queryContentGroup }



        return assignedContentGroups.randomOrNull()
    }


    fun tryAssignNewContent(queryGroup: QueryGroup, subject: Subject) : QueryContentGroup? {
        val queryGroupId = queryGroup.id ?: return null

        val allContentGroups = queryContentGroupRepository.findAllByQueryGroupIdAndStatusAndIsArchivedIsFalse(queryGroupId);
        val assignedContentGroups = queryParticipantContentRepository.findBySubjectAndQueryGroupAndIsArchivedFalse(subject, queryGroup).map { it.queryContentGroup }
        val assignedContentGroupIds = assignedContentGroups.map { it?.id }.toSet()

        val uniqueContent = allContentGroups.filter { it.id !in assignedContentGroupIds }


        if(uniqueContent.isNotEmpty()){
            log.info("[TEEST] there is unique content size {}", uniqueContent.size)
            val newContent = uniqueContent.random();
            val participantContentGroup = saveParticipantContentGroup(queryGroup, newContent, subject)

            assignCBTContent(participantContentGroup, newContent,subject)


            return newContent
        }

        return null
    }

    fun assignCBTContent(participantContentGroup:  QueryParticipantContent, contentGroup: QueryContentGroup, subject:Subject) {
        if(contentGroup.id != null) {
            val contentItems = queryContentRepository.findAllByQueryContentGroupId(contentGroup.id!!)

            for(contentItem in contentItems){
                if(contentItem.type == ContentType.CBT_CONTENT) {
                    cbtContentService.createNewCBTAssignmentForParticipant(contentItem, participantContentGroup, subject)
                }
            }
        }
    }


    fun getContentItemsForSubjectAndContentGroup(subjectId: Long, contentGroupId: Long) : List<QueryContentDTO> {
        var result : List<QueryContentDTO> = emptyList()


        val contentGroupOpt = queryContentGroupRepository.findById(contentGroupId)
        log.info("[TEEEST] content group {}", contentGroupOpt.isPresent)
        log.info("[TEEEST] content group id  {}", contentGroupId)
        val subjectOpt = subjectRepository.findById(subjectId)
        log.info("[TEEEST] subjectOpt {}", subjectOpt.isPresent)


        if(contentGroupOpt.isPresent && subjectOpt.isPresent) {
            log.info("[TEEEST] both present ")
            val contentGroup = contentGroupOpt.get()
            val subject = subjectOpt.get()

            val all = queryParticipantContentRepository.findAll()
            val participantContentGroupList = queryParticipantContentRepository.findByQueryContentGroupAndSubjectAndIsArchivedFalse(contentGroup, subject);
            log.info("[TEEEST] all size {}", all.size)
            log.info("[TEEEST] participantContentGroupList {}", participantContentGroupList.size)
            if(participantContentGroupList.isNotEmpty()) {
                log.info("[TEEEST] list is not empty  ")

                result = findAllByContentGroupId(contentGroup.id!!)

                injectCBTContentIntoQueryContentDTO(participantContentGroupList, result)
            }

        }

        return result
    }

    fun injectCBTContentIntoQueryContentDTO(participantContentGroupList: List<QueryParticipantContent>, result :  List<QueryContentDTO>){
        val participantContentGroup = participantContentGroupList.first()
        for(queryContentDTO in result){
            if(queryContentDTO.type == ContentType.CBT_CONTENT){
                val assignedCBTContent  = cbtContentService.getAssignedCBTContent(queryContentDTO.id!!, participantContentGroup.id!! )
                assignedCBTContent?.cbtType?.let { type ->
                    queryContentDTO.cbtType = CBTContentType.valueOf(type)
                    queryContentDTO.cbtRoute = assignedCBTContent.assignedCbtRoute
                    queryContentDTO.cbtVersion = assignedCBTContent.cbtVersion
                }
            }
        }
    }

    fun findAllByContentGroupId(contentGroupId: Long) : List<QueryContentDTO> {
        val queryContentList = queryContentRepository.findAllByQueryContentGroupId(contentGroupId);

        val queryContentListDTO =  queryContentList.mapNotNull { queryContentMapper.queryContentToQueryContentDTO(it) }
        return queryContentListDTO
    }

    fun getAllContentGroupsForParticipant(subjectId: Long): Map<String, List<QueryContentGroupDTO>>  {
        val result = mutableMapOf<String, List<QueryContentGroupDTO>>()

        val subjectOpt = subjectRepository.findById(subjectId)

        if(subjectOpt.isPresent) {
            val subject = subjectOpt.get();
            val allAssignedParticipantContent = queryParticipantContentRepository.findBySubjectAndIsArchivedFalse(subject)


            for(participantContent in allAssignedParticipantContent) {
                val queryGroup = participantContent.queryGroup ?: continue
                val queryContentGroupDTO = queryContentGroupMapper.queryContentGroupToQueryContentGroupDTO(participantContent.queryContentGroup)
                val key = queryGroup.name ?: continue

                if(queryContentGroupDTO != null) {
                    result[key] = result.getOrDefault(key, mutableListOf()) + queryContentGroupDTO
                }
            }
        }
        return result
    }


    fun processCompletedQueriesForParticipant(participantId: Long): Boolean {

        val queryParticipantList = queryParticipantRepository.findBySubjectId(participantId)
        if (queryParticipantList.isEmpty()) return false

        val subjectOpt = subjectRepository.findById(participantId)
        if (!subjectOpt.isPresent) return false

        val subject = subjectOpt.get()

        for (queryParticipant in queryParticipantList) {
            if(queryParticipant.queryGroup != null) {
                val evaluations = queryEvaluationRepository.findTop2BySubjectAndQueryGroupOrderByCreatedDateDesc(
                    subject,
                    queryParticipant.queryGroup!!
                )

                if(evaluations.isEmpty()) {
                    continue
                }

                val latestEvaluation = evaluations[0]
                val queryGroup = latestEvaluation.queryGroup ?: continue

                val latestNotificationDate = queryEvaluationRepository.findFirstBySubjectAndQueryGroupAndNotificationScheduledIsTrueOrderByCreatedDateDesc(subject, queryGroup)?.createdDate

                val shouldSendNotification = shouldSendNotification(evaluations, latestNotificationDate)

                if(shouldSendNotification) {

                    var content = tryAssignNewContent(queryGroup, subject)

                    if(content == null) {
                        content = getRandomAlreadyAssignedContent(queryGroup, subject)
                    }

                    notificationService.scheduleNotification(content, latestEvaluation, subject)

                    latestEvaluation.notificationScheduled = true
                    queryEvaluationRepository.save(latestEvaluation)
                }
            }
        }

        return true;


    }

    fun updateContentGroupStatus(status: ContentGroupStatus, contentGroup: QueryContentGroup){
        contentGroup.status = status

        if (status == ContentGroupStatus.INACTIVE && contentGroup.id != null) {
            // archive the query groups instead of deleting them for audit purposes
            val participantContents = queryParticipantContentRepository.findByQueryContentGroupIdAndIsArchivedFalse(contentGroup.id!!)
            for(participantContent in participantContents){
                participantContent.isArchived = false
                queryParticipantContentRepository.saveAndFlush(participantContent)
            }
        }
    }

    fun getModuleById(moduleId: Long) : Module? {
        val moduleOpt = moduleRepository.findById(moduleId)

        if(moduleOpt.isPresent) {
            return moduleOpt.get()
        }
        return null;
    }


    fun getModulesGroupedByGroupName(): List<ModuleGroupDTO> {
        val groupNames = moduleRepository.findDistinctGroupNames()

        return groupNames.map { groupName ->
            val modules = moduleRepository.findAllByGroupName(groupName)
            ModuleGroupDTO(
                groupName = groupName,
                modules = modules
            )
        }
    }
    open fun getCurrentTime(zone: ZoneId = ZoneId.of("Europe/London")): LocalTime = LocalTime.now(zone)

    @Scheduled(cron = "0 0 16 * * ?", zone = "Europe/London")
    fun sendScheduledNotifications() {
        val now = getCurrentTime(ZoneId.of("Europe/London"))
        if (now.hour != 16) {
            return
        }

        log.info("[WORKER] running send Scheduled Notifications ")

       val allUnsentNotifications =  contentNotificationRepository.findAllBySentIsFalse()

        for(notification in allUnsentNotifications) {

            log.info("[WORKER] trying to send this notificatio {} ", notification)


            val user = notification.subject?.user ?: throw IllegalArgumentException("[QueryContentService][sendScheduledNotifications] User is not present")
            val contentGroup = notification.contentGroup ?:  throw IllegalArgumentException("[QueryContentService][sendScheduledNotifications] Content Group is not present")

            val notificationDTO = NotificationDTO().apply {
                title = contentGroup.contentGroupName
                body = "Click here to read more"
                route = "/queryContent/" + contentGroup.id
            }

            val isSuccess = notificationService.sendNotification(listOf(user), notificationDTO)

            notification.sent = isSuccess

            if(isSuccess) {
                notification.sentOn = ZonedDateTime.now()
            }

            contentNotificationRepository.saveAndFlush(notification)
        }

    }


    fun isContentGroupAssigned(queryContentGroupId: Long) : Boolean {
        val assignedContentGroups = queryParticipantContentRepository.findByQueryContentGroupIdAndIsArchivedFalse(queryContentGroupId);
        return assignedContentGroups.isNotEmpty()
    }

    companion object {
        private val log = LoggerFactory.getLogger(QueryContentService::class.java)
    }
}
