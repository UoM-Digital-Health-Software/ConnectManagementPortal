import { Component, OnDestroy, OnInit } from '@angular/core';
import { QueriesService } from './queries.service';
import { QueryGroup } from './queries.model';
import { Router } from '@angular/router';

@Component({
    selector: 'jhi-queries',
    templateUrl: './queryGroupList.component.html',
    styleUrls: ['./queryGroupList.component.scss'],
})
export class QueryGroupListComponent implements OnInit, OnDestroy {
    public queryGroupList: any[] = [];

    constructor(
        private queriesService: QueriesService,
        private router: Router
    ) {}

    getQueryGroupList() {
        this.queriesService
            .getQueryGroupList()
            .subscribe((result: QueryGroup[]) => {
                this.queryGroupList = result.filter(
                    (group) => !group.isArchived
                );
            });
    }

    ngOnInit() {
        this.getQueryGroupList();
    }

    archiveQueryGroup(id: number) {
        if (confirm('Are you sure you want to archive this query group?')) {
            this.queriesService.archiveQueryGroup(id).subscribe({
                next: () => this.getQueryGroupList(),
                error: (err) => {
                    alert(
                        err.error.message ||
                            'Failed to archive query group, the query group has been assigned.'
                    );
                },
            });
        }
    }

    async duplicateQueryGroup(id: number) {
        this.router.navigate(['duplicateQuery', id]);
    }

    ngOnDestroy() {}
}
