import { Component, OnInit } from '@angular/core';
import { QueriesService } from './queries.service';
import { QueryGroup } from './queries.model';
import { Router } from '@angular/router';

@Component({
    selector: 'jhi-archived-query-groups',
    templateUrl: './archivedQueryGroupList.component.html',
})
export class ArchivedQueryGroupListComponent implements OnInit {
    
    archivedQueryGroupList: QueryGroup[] = [];

    constructor(
        private queriesService: QueriesService,
        private router: Router
    ) {}

    ngOnInit() {
        this.loadArchivedGroups();
    }

    loadArchivedGroups() {
        this.queriesService.getQueryGroupList().subscribe((res) => {
            this.archivedQueryGroupList = res.filter(group => group.isArchived);
        });
    }

    unarchiveQueryGroup(id: number) {
        if (confirm('Are you sure you want to unarchive this query group?')) {
            this.queriesService.unarchiveQueryGroup(id).subscribe(() => {
                this.loadArchivedGroups();
            });
        }
    }
}
