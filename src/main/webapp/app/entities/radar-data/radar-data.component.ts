import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    OnDestroy,
} from '@angular/core';
import { combineLatest, forkJoin, Observable, Subscription, of, from, BehaviorSubject } from 'rxjs';
import { filter, map, switchMap, tap } from 'rxjs/operators';
import {
    ProjectService,
    OrganizationService,
    Project,
    Organization,
} from 'app/shared';
import { SubjectService } from 'app/shared/subject';
import { Subject as Participant } from 'app/shared/subject';
import { HttpResponse } from '@angular/common/http';
import { ChangeDetectorRef } from '@angular/core';

interface SubjectWithDataLogs extends Participant {
    dataLogs?: { [type: string]: string };
    _loading?: boolean;
}

interface GroupedSubjects {
    organization: Organization;
    projects: {
        project: Project;
        subjects: SubjectWithDataLogs[];
        totalItems: number;
        page: number;
    }[];
}


interface ProjectWithSubjects {
    project: Project;
    subjects: SubjectWithDataLogs[];
    totalItems: number;
    page: number;
}

@Component({
    selector: 'jhi-grouped-subjects',
    templateUrl: './radar-data.component.html',
    styleUrls: ['./radar-data.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RadarDataComponent implements OnInit, OnDestroy {
    groupedSubjects$: Observable<GroupedSubjects[]>;
    groupedSubjectsData: GroupedSubjects[] | null = null;
    selectedProjectName$ = new BehaviorSubject<string | null>(null);

    currentProject: unknown
    projectWithSubjects$: Observable<ProjectWithSubjects>

    allProjects$: Observable<Project[]>
    allProjects: Project[]
    selectedProject: Project | null = null;

    private subscriptions = new Subscription();

    visibleSubjectsCount: { [projectName: string]: number } = {};
    defaultVisibleCount = 20;

    visibleSubjectsMap: { [projectName: string]: SubjectWithDataLogs[] } = {};

    constructor(
        private projectService: ProjectService,
        private subjectService: SubjectService,
        private organizationService: OrganizationService,
        private cdr: ChangeDetectorRef
    ) { }


    ngOnInit(): void {
        this.projectService.fetch().toPromise().then((allProjects) => {
            this.allProjects = allProjects;
        })

        this.allProjects$ = this.projectService.fetch()



        this.projectWithSubjects$ = this.selectedProjectName$.pipe(
            filter(name => !!name),
            switchMap(projectName =>
                this.projectService.find(projectName!).pipe(
                    switchMap(project =>
                        this.subjectService.findAllByProject(project.projectName, {}, { size: 1000 }).pipe(
                            map(res => res.body || []),
                            map(
                                (
                                    subjects: SubjectWithDataLogs[]
                                ) => ({
                                    project,
                                    subjects,
                                    totalItems: subjects.length,
                                    page: 0,
                                })
                            ),
                            tap(rtn => {
                                const visible = rtn.subjects.slice(0, this.defaultVisibleCount);
                                this.visibleSubjectsMap[rtn.project.projectName] = visible;
                                this.visibleSubjectsCount[rtn.project.projectName] = this.defaultVisibleCount;
                                this.loadDataLogsForSubjects(rtn.project.projectName, visible);
                                this.currentProject = rtn
                            })
                        )
                    )
                )
            )
        );




    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    fetchDataLogsForSubjects(
        subjects: SubjectWithDataLogs[]
    ): Observable<SubjectWithDataLogs[]> {
        if (!subjects.length) return of([]);


        const ids = subjects.map((subject) => subject.login)

        const data = from(this.subjectService.findDataLogsForSubjects(ids).toPromise().then((response) => {
            return subjects.map((subject) => {
                if (response.body[subject.login]) {
                    const data = response.body[subject.login]

                    const logs = (data || []).reduce(
                        (acc, cur) => {
                            acc[cur.dataGroupingType] = new Date(
                                cur.time
                            ).toDateString();
                            return acc;
                        },
                        {} as { [type: string]: string }
                    );
                    subject.dataLogs = logs

                    return subject
                }
            })
        }))

        return data
    }

    loadDataLogsForSubjects(
        projectName: string,
        subjects: SubjectWithDataLogs[]
    ): void {
        const subjectsToLoad = subjects.filter(
            (s) => !s.dataLogs && !s._loading
        );
        if (!subjectsToLoad.length) return;

        subjectsToLoad.forEach((s) => (s._loading = true));

        this.fetchDataLogsForSubjects(subjectsToLoad).subscribe(() => {
            subjectsToLoad.forEach((s) => (s._loading = false));
            this.cdr.markForCheck();
        });
    }

    showMore(
        projectName: string,
        totalSubjects: number,
        allSubjects: SubjectWithDataLogs[]
    ): void {
        const current =
            this.visibleSubjectsCount[projectName] ?? this.defaultVisibleCount;
        const next = Math.min(
            current + this.defaultVisibleCount,
            totalSubjects
        );
        this.visibleSubjectsCount[projectName] = next;

        this.visibleSubjectsMap[projectName] = allSubjects.slice(0, next);

        this.loadDataLogsForSubjects(
            projectName,
            this.visibleSubjectsMap[projectName]
        );
    }
}
