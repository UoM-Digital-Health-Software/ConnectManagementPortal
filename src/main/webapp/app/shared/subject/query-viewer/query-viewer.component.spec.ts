import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NgbModal, NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { QueryViewerComponent } from './query-viewer.component';
import { QueryParticipantService } from '../query-participant.service';
import { of } from 'rxjs/internal/observable/of';
import { DeleteQueryConfirmDialogComponent } from './delete-query-confirm-dialog.component';

describe('QueryViewerComponent', () => {
    //   let component: QueryViewerComponent;
    //   let fixture: ComponentFixture<QueryViewerComponent>;

    //   beforeEach(async () => {
    //     await TestBed.configureTestingModule({
    //       declarations: [ QueryViewerComponent ]
    //     })
    //     .compileComponents();
    //   });

    //   beforeEach(() => {
    //     fixture = TestBed.createComponent(QueryViewerComponent);
    //     component = fixture.componentInstance;
    //     fixture.detectChanges();
    //   });

    //   it('should create', () => {
    //     expect(component).toBeTruthy();
    //   });


    let component: QueryViewerComponent;
    let fixture: ComponentFixture<QueryViewerComponent>;
    let queryParticipantServiceSpy: jasmine.SpyObj<QueryParticipantService>;
    let modalServiceSpy: jasmine.SpyObj<NgbModal>;

    beforeEach(async () => {
        const qpServiceMock = jasmine.createSpyObj('QueryParticipantService', [
            'getAllAssignedQueries',
            'assignQueryGroup',
            'deleteAssignedQueryGroup',
            'deleteQueryParticipantContent'
        ]);

        const modalMock = jasmine.createSpyObj('NgbModal', ['open']);

        await TestBed.configureTestingModule({
            declarations: [QueryViewerComponent],
            providers: [
                { provide: QueryParticipantService, useValue: qpServiceMock },
                { provide: NgbModal, useValue: modalMock },
                NgbActiveModal
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(QueryViewerComponent);
        component = fixture.componentInstance;
        queryParticipantServiceSpy = TestBed.inject(QueryParticipantService) as jasmine.SpyObj<QueryParticipantService>;
        modalServiceSpy = TestBed.inject(NgbModal) as jasmine.SpyObj<NgbModal>;

        component.subject = { id: 123 } as any;
        component.queryGroupList = [
            { id: 1, isArchived: false },
            { id: 2, isArchived: false },
            { id: 3, isArchived: true }
        ] as any[];


    })

    it('should filter queryGroupList correctly in getAllAssignedGroups()', fakeAsync(() => {
        const assignedQueries = [{ queryGroupId: 1 }, { queryGroupId: 3 }] as any[];
        queryParticipantServiceSpy.getAllAssignedQueries.and.returnValue(of(assignedQueries));

        component.getAllAssignedGroups();
        tick();

        expect(queryParticipantServiceSpy.getAllAssignedQueries).toHaveBeenCalledWith(123);
        expect(component.assignedQueryGroups).toEqual(assignedQueries);

        expect(component.queryGroupList).toEqual([{ id: 2, isArchived: false }]);
        expect(component.queryGroupList.length).toEqual(1);
    }));

    it('should call assignQueryGroup() and update lists', fakeAsync(() => {
        component.selectedGroup = 2;
        component.queryPriticipant = {} as any;
        spyOn(component, 'getAllAssignedGroups');
        spyOn(component, 'removeQueryGroupFromList');

        queryParticipantServiceSpy.assignQueryGroup.and.returnValue(of({}));

        component.assignQueryGroup();
        tick();

        expect(queryParticipantServiceSpy.assignQueryGroup).toHaveBeenCalledWith({
            queryGroupId: 2,
            subjectId: 123
        });
        expect(component.getAllAssignedGroups).toHaveBeenCalled();
        expect(component.removeQueryGroupFromList).toHaveBeenCalledWith(2);
    }));

    it('should remove the query group and set ifDisable', () => {
        component.ifDisable = false;
        component.queryGroupList = [
            { id: 1 },
            { id: 2 },
            { id: 3 }
        ] as any[];

        component.removeQueryGroupFromList(2);

        expect(component.ifDisable).toBeTrue();
        expect(component.queryGroupList).toEqual([{ id: 1 }, { id: 3 }]);
    });

    it('should delete assigned group and related content when confirmed', fakeAsync(() => {
        const modalRefMock = { result: Promise.resolve(true) };
        modalServiceSpy.open.and.returnValue(modalRefMock as any);

        queryParticipantServiceSpy.deleteAssignedQueryGroup.and.returnValue(of({}));
        queryParticipantServiceSpy.deleteQueryParticipantContent.and.returnValue(of({}));
        spyOn(component, 'afterGroupDeleted');

        const group = { id: 10, isArchived: false } as any;

        component.deleteAssignedGroup(group);
        tick();

        expect(modalServiceSpy.open).toHaveBeenCalledWith(DeleteQueryConfirmDialogComponent);
        expect(queryParticipantServiceSpy.deleteAssignedQueryGroup).toHaveBeenCalledWith(10, 123);
        expect(queryParticipantServiceSpy.deleteQueryParticipantContent).toHaveBeenCalledWith(10, 123);
        expect(component.afterGroupDeleted).toHaveBeenCalledWith(group);
    }));

    it('should only delete assigned group when not confirmed', fakeAsync(() => {
        const modalRefMock = { result: Promise.resolve(false) };
        modalServiceSpy.open.and.returnValue(modalRefMock as any);

        queryParticipantServiceSpy.deleteAssignedQueryGroup.and.returnValue(of({}));
        spyOn(component, 'afterGroupDeleted');

        const group = { id: 20, isArchived: true } as any;

        component.deleteAssignedGroup(group);
        tick();

        expect(queryParticipantServiceSpy.deleteAssignedQueryGroup).toHaveBeenCalledWith(20, 123);
        expect(queryParticipantServiceSpy.deleteQueryParticipantContent).not.toHaveBeenCalled();
        expect(component.afterGroupDeleted).toHaveBeenCalledWith(group);
    }));

    it('should refresh list and add group if not archived', () => {
        spyOn(component, 'getAllAssignedGroups');
        component.queryGroupList = [];
        component.selectedGroup = 5;

        const group = { id: 99, isArchived: false } as any;
        component.afterGroupDeleted(group);

        expect(component.getAllAssignedGroups).toHaveBeenCalled();
        expect(component.queryGroupList).toContain(group);
        expect(component.selectedGroup).toBeNull();
    });

    it('should refresh list and NOT add group if archived', () => {
        spyOn(component, 'getAllAssignedGroups');
        component.queryGroupList = [];

        const group = { id: 99, isArchived: true } as any;
        component.afterGroupDeleted(group);

        expect(component.queryGroupList).not.toContain(group);
    });

})
