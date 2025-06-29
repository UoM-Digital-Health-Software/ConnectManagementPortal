import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Subject } from './subject.model';
import { createRequestOption } from '../model/request.utils';

@Injectable({ providedIn: 'root' })
export class SubjectService {
    private resourceUrl = 'api/subjects';
    private projectResourceUrl = 'api/projects';

    constructor(private http: HttpClient) {}

    create(subject: Subject): Observable<Subject> {
        const copy: Subject = Object.assign({}, subject);
        return this.http.post<Subject>(this.resourceUrl, copy);
    }

    update(subject: Subject): Observable<Subject> {
        const copy: Subject = Object.assign({}, subject);
        return this.http.put<Subject>(this.resourceUrl, copy);
    }

    discontinue(subject: Subject): Observable<Subject> {
        const copy: Subject = Object.assign({}, subject);
        return this.http.put<Subject>(`${this.resourceUrl}/discontinue`, copy);
    }

    find(login: string): Observable<Subject> {
        return this.http.get<Subject>(
            `${this.resourceUrl}/${encodeURIComponent(login)}`
        );
    }

    findForRevision(login: string, revisionNb: number): Observable<Subject> {
        return this.http.get<Subject>(
            `${this.resourceUrl}/${encodeURIComponent(
                login
            )}/revisions/${revisionNb}`
        );
    }

    findRevisions(login: string, req?: any): Observable<HttpResponse<any>> {
        const params = createRequestOption(req);
        return this.http.get(
            `${this.resourceUrl}/${encodeURIComponent(login)}/revisions`,
            { params, observe: 'response' }
        );
    }

    findDataLogs(login: string, req?: any): Observable<HttpResponse<any>> {
        const params = createRequestOption(req);
        return this.http.get(
            `${this.resourceUrl}/${encodeURIComponent(login)}/datalogs`,
            { params, observe: 'response' }
        );

    }

    findDataLogsForSubjects(ids: any[]) {
        const params = new HttpParams().set('ids', ids.join(','));
        return this.http.get(
            `api/datalogs`,
            { params, observe: 'response' }
        );
    }

    findDataSummary(login: string, req?: any): Observable<HttpResponse<any>> {
        const params = createRequestOption(req);
        return this.http.get(
            `${this.resourceUrl}/${encodeURIComponent(login)}/datasummary`,
            { params, observe: 'response' }
        );
    }

    makeReportReady(login: string, req?: any): Observable<any> {
        const params = createRequestOption(req);
        return this.http.post(
            `${this.resourceUrl}/${encodeURIComponent(login)}/reportready`,
            { params, observe: 'response' }
        );
    }

    findAllExternalIds(): Observable<HttpResponse<any>> {
        return this.http.get(this.resourceUrl + '/externalId', {
            observe: 'response',
        });
    }

    query(
        filterParams?: SubjectFilterParams,
        paginationParams?: SubjectPaginationParams
    ): Observable<HttpResponse<any>> {
        return this.http.get(this.resourceUrl, {
            params: createRequestOption({
                ...paginationParams,
                ...filterParams,
            }),
            observe: 'response',
        });
    }

    delete(login: string): Observable<any> {
        return this.http.delete(
            `${this.resourceUrl}/${encodeURIComponent(login)}`
        );
    }

    findAllByProject(
        projectName: string,
        filterParams: SubjectFilterParams,
        paginationParams: SubjectPaginationParams
    ): Observable<HttpResponse<Subject[]>> {
        let url = `${this.projectResourceUrl}/${projectName}/subjects`;
        return this.http.get<Subject[]>(url, {
            params: createRequestOption({
                ...paginationParams,
                ...filterParams,
            }),
            observe: 'response',
        });
    }
}

export interface SubjectFilterParams {
    login?: string;
    externalId?: string;
    dateOfBirth?: SubjectFilterRange;
    enrollmentDate?: SubjectFilterRange;
    groupId?: string | number;
    humanReadableIdentifier?: string;
    personName?: string;
    humanReadableId?: string;
}

export interface SubjectFilterRange {
    from?: string;
    to?: string;
    is?: string;
}

export interface SubjectPaginationParams {
    last?: SubjectLastParams;
    size?: number;
    sort?: [string];
    page?: number;
}

export interface SubjectLastParams {
    id?: number;
    login?: string;
    externalId?: string;
}
