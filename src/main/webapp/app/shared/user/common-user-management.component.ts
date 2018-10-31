import {
    Component, OnInit, OnDestroy, Input, SimpleChanges, SimpleChange,
    OnChanges
} from '@angular/core';
import { Response } from '@angular/http';
import { EventManager, ParseLinks, AlertService, JhiLanguageService } from 'ng-jhipster';

import { ITEMS_PER_PAGE, User, UserService } from '..';
import {Project} from "../../entities/project";

@Component({
    selector: 'common-user-mgmt',
    templateUrl: './common-user-management.component.html'
})
export class CommonUserMgmtComponent implements OnInit, OnChanges{

    currentAccount: any;
    users: User[];
    error: any;
    success: any;
    totalItems: any;
    queryCount: any;
    itemsPerPage: any;
    page: any;
    predicate: any;
    previousPage: any;
    reverse: any;

    @Input() project : Project;
    @Input() authority : String;

    constructor(
        private jhiLanguageService: JhiLanguageService,
        private userService: UserService,
        private parseLinks: ParseLinks,
        private alertService: AlertService,
        private eventManager: EventManager,
    ) {
        this.itemsPerPage = ITEMS_PER_PAGE;
        this.jhiLanguageService.addLocation('user-management');
    }

    ngOnInit() {
        this.loadAll();
        this.registerChangeInUsers();
    }


    registerChangeInUsers() {
        this.eventManager.subscribe('userListModification', () => this.loadAll());
    }

    ngOnChanges(changes: SimpleChanges) {
        const project: SimpleChange = changes.project? changes.project: null;
        if(project){
            this.project = project.currentValue;
            this.loadAll();
        }
    }

    loadAll() {
       if(this.project && this.authority) {
           this.userService.findByProjectAndAuthority(
               {
                   projectName: this.project.projectName ,
                   authority: this.authority,
               }
               ).subscribe(
               (res: Response) => this.users = res.json());
       }
    }

    trackIdentity(index, item: User) {
        return item.id;
    }

    sort() {
        const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')];
        if (this.predicate !== 'id') {
            result.push('id');
        }
        return result;
    }

    loadPage(page: number) {
        if (page !== this.previousPage) {
            this.previousPage = page;
            this.transition();
        }
    }

    transition() {
        this.loadAll();
    }
}
