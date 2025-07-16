import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ContentItem } from './queries.model';
import { QueryGroup } from './queries.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class QueriesService {
    private baseURL = 'api/query-builder';

    constructor(private http: HttpClient) {}

    saveContentGroup(contentGroupData: {
        queryGroupId: number;
        contentGroupName: string;
        queryContentDTOList: ContentItem[];
    }) {
        return this.http.post(
            this.baseURL + '/querycontentgroup',
            contentGroupData
        );
    }

    getQueryGroup(queryId: number) {
        return this.http.get(this.baseURL + '/querygroups/' + queryId);
    }

    getAllQueryContentsAndGroups(queryId: number) {
        return this.http.get(
            this.baseURL + '/querycontent/querygroup/' + queryId
        );
    }

    saveNewQueryGroup(queryGroup: QueryGroup) {
        return this.http.post(this.baseURL + '/querygroups', queryGroup);
    }

    updateQueryGroup(queryGroup: QueryGroup, queryGroupId: number) {
        return this.http.put(
            this.baseURL + '/querygroups/' + queryGroupId,
            queryGroup
        );
    }

    saveQueryLogic(query_logic: any) {
        return this.http.post(this.baseURL + '/querylogic', query_logic);
    }

    updateQueryLogic(query_logic: any) {
        return this.http.put(this.baseURL + '/querylogic', query_logic);
    }

    deleteQueryGroup(groupId: number) {
        return this.http.delete(this.baseURL + `/querygroups/${groupId}`);
    }

    getQueryGroupList(): Observable<QueryGroup[]> {
        return this.http.get<QueryGroup[]>(this.baseURL + '/querygroups');
    }

    deleteContentGroupByID(queryContentGroupId: any) {
        return this.http.delete(
            this.baseURL + `/querycontentgroup/${queryContentGroupId}`
        );
    }

    checkDuplicateQueryGroupName(name: string, currentId: number | null) {
        return this.http.get<boolean>(this.baseURL+ `/querygroups/check-name?name=${encodeURIComponent(name)}&excludeId=${currentId || ''}`);
    }

    archiveQueryGroup(id: number) {
        return this.http.patch(
            this.baseURL + `/querygroups/${id}/archive`,
            null
        );
    }

    unarchiveQueryGroup(id: number) {
        return this.http.patch(
            this.baseURL + `/querygroups/${id}/unarchive`,
            null
        );
    }

    getModules() {
        return this.http.get(this.baseURL + '/modules');
    }

    gellAllPhysicalTypes() {
        return this.http.get(this.baseURL + '/querybuilder/physical-types');
    }

    getQuestionnaireItems() {
         return this.http.get(this.baseURL + '/querybuilder/questionnaire-types');
    }

    getEntities() {
         return this.http.get(this.baseURL + '/querybuilder/entities-types');



    }
}
