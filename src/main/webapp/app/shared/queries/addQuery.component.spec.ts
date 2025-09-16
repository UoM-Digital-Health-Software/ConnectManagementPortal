// @ts-nocheck
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing'; // ✅ import this

import { AddQueryComponent } from './addQuery.component';
import { QueriesService } from './queries.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { AlertService } from '../util/alert.service';
import { ActivatedRoute } from '@angular/router';

import { Router } from '@angular/router';

class MockQueriesService {
    gellAllPhysicalTypes = jasmine.createSpy().and.returnValue(of({}));
    getQuestionnaireItems = jasmine.createSpy().and.returnValue(of({ questionnaire: [], delusions: [] }));
    getEntities = jasmine.createSpy().and.returnValue(of({}));
}


class MockAlertService {
    clear = jasmine.createSpy();
    success = jasmine.createSpy();
    error = jasmine.createSpy();
}

// @ts-nocheck
describe('AddQueryComponent', () => {
    let component: AddQueryComponent;
    let fixture: ComponentFixture<AddQueryComponent>;
    let mockRoute: any;
    let mockAlertService: any;
    let mockQueryService: any;
    let mockRouter: any;




    const urlSubject = new BehaviorSubject([{ path: 'duplicateQuery' }]);
    const paramsSubject = new BehaviorSubject({ 'query-id': '123' });

    afterEach(() => {
        TestBed.resetTestingModule();
    });

    beforeEach(async () => {

        mockRoute = {
            url: urlSubject.asObservable(),
            params: paramsSubject.asObservable(),
        };

        mockAlertService = jasmine.createSpyObj('AlertService', ['clear', 'error', 'success']);
        mockQueryService = jasmine.createSpyObj('QueryService', ['getQueryGroup', 'gellAllPhysicalTypes', 'getQuestionnaireItems', 'getEntities', 'getAllQueryContentsAndGroups', 'saveContentGroup', 'deleteContentGroupByID', 'cancelEditContent', 'saveNewQueryGroup', 'updateQueryGroup', 'saveQueryLogic', 'updateQueryLogic']);
        mockQueryService.getQueryGroup.and.returnValue(
            of({
                queryGroupName: 'Test Group',
                queryGroupDescription: 'Test Desc',
                canEdit: false,
            })
        );

        mockQueryService.gellAllPhysicalTypes.and.returnValue(of([]));
        mockQueryService.getQuestionnaireItems.and.returnValue(of([]));
        mockQueryService.getEntities.and.returnValue(of([]));



        mockQueryService.saveContentGroup.and.returnValue(of([]));
        const responseMock = [
            {
                contentGroupName: 'Group A',
                queryContentDTOList: [{ id: 1, value: 'item1' }],
                queryGroupId: 'group-123',
                id: 10,
                status: 'ACTIVE',
            },
            {
                contentGroupName: 'Group B',
                queryContentDTOList: null,
                queryGroupId: 'group-123',
                id: 11,
            },
        ];

        mockQueryService.getAllQueryContentsAndGroups.and.returnValue(of(responseMock));



        mockRouter = jasmine.createSpyObj('Router', ['navigate']);



        await TestBed.configureTestingModule({
            imports: [
                HttpClientTestingModule,   // ✅ this fixes NullInjectorError
                ReactiveFormsModule,       // ✅ provides FormBuilder
                RouterTestingModule
            ],
            declarations: [AddQueryComponent],
            providers: [
                { provide: ActivatedRoute, useValue: mockRoute },
                { provide: AlertService, useValue: mockAlertService },
                { provide: QueriesService, useValue: mockQueryService },
                { provide: Router, useValue: mockRouter },

                FormBuilder]
        }).compileComponents();

        fixture = TestBed.createComponent(AddQueryComponent);
        component = fixture.componentInstance;
        //  fixture.detectChanges();

        //   spyOn(window, 'confirm').and.returnValue(true); // or false if you want “Cancel”



    });


    it('should initialize properties from constructor promises', async () => {
        const physicalTypesMock = { field1: 'type1' };
        const questionnaireMock = { questionnaire: [{ id: 1, text: 'Q1' }], delusions: [] };
        const entitiesMock = { entity1: 'E1' };

        mockQueryService.gellAllPhysicalTypes.and.returnValue(of(physicalTypesMock));
        mockQueryService.getQuestionnaireItems.and.returnValue(of(questionnaireMock));
        mockQueryService.getEntities.and.returnValue(of(entitiesMock));

        spyOn(AddQueryComponent.prototype, 'addQuestionnaireItemsToQueryBuilder');
        spyOn(AddQueryComponent.prototype, 'addDelusionsToQueryBuilder');

        const comp = new AddQueryComponent(
            mockAlertService,
            mockQueryService,
            new FormBuilder(),
            null as any,
            null as any,
            null as any,
            mockRoute
        );

        // Wait for all promises in constructor to resolve
        await Promise.resolve(); // microtask queue
        await Promise.resolve();

        expect(comp.questionnare).toEqual(questionnaireMock.questionnaire);
        expect(comp.delusions).toEqual(questionnaireMock.delusions);
        expect(comp.config.entities).toEqual(entitiesMock);
        expect(comp.config.fields).toEqual(jasmine.objectContaining(physicalTypesMock));

        expect(comp.addQuestionnaireItemsToQueryBuilder).toHaveBeenCalled();
        expect(comp.addDelusionsToQueryBuilder).toHaveBeenCalled();
        expect(comp.isLoaded).toBeTrue();
    });


    it('should create', () => {
        expect(component).toBeTruthy();
    });


    it('[ngOnInit] should set isDuplicateMode, queryId from route and should be editable  if it exists', async () => {
        spyOn(component, 'refreshContentGroups');

        urlSubject.next([{ path: 'duplicateQuery' }]);
        paramsSubject.next({ 'query-id': '123' });

        await component.ngOnInit();

        expect(component.isDuplicateMode).toBeTrue();
        expect(component.queryId).toBe('123');
        expect(component.queryGroupId).toBe('123');

        //  expect(mockQueryService.getQueryGroup).toHaveBeenCalledWith('123');

        expect(component.queryGrouName).toBe('Test Group_duplicate');
        expect(component.queryGroupDesc).toBe('Test Desc');
        expect(component.readonlyMode).toBeFalse()

        expect(component.refreshContentGroups).toHaveBeenCalled();
    });

    it('[ngOnInit] should be editable and not set isDuplicateMode if no route exits ', async () => {


        urlSubject.next([{ path: 'query' }]);
        paramsSubject.next({ 'query-id': null });

        await component.ngOnInit();

        expect(component.isDuplicateMode).toBeFalse();
        expect(component.queryId).toBe(null);
        expect(component.queryGroupId).toBe(null);

        expect(component.queryGrouName).toBe(undefined);
        expect(component.queryGroupDesc).toBe(undefined);
        expect(component.readonlyMode).toBeFalse()
    });

    it('[ngOnInit] should not be editable and should load the query if route exits and canEdit for the query is false', async () => {


        urlSubject.next([{ path: 'query' }]);
        paramsSubject.next({ 'query-id': '123' });

        await component.ngOnInit();

        expect(component.isDuplicateMode).toBeFalse();
        expect(component.queryId).toBe('123');
        expect(component.queryGroupId).toBe('123');

        expect(mockQueryService.getQueryGroup).toHaveBeenCalledWith('123');

        expect(component.queryGrouName).toBe('Test Group');
        expect(component.queryGroupDesc).toBe('Test Desc');
        expect(component.readonlyMode).toBeTrue()
    });


    it('[ngOnInit] should be editable and should load the query if route exits and canEdit for the query is true', async () => {
        spyOn(component, 'refreshContentGroups');

        mockQueryService.getQueryGroup.and.returnValue(
            of({
                queryGroupName: 'Test Group',
                queryGroupDescription: 'Test Desc',
                canEdit: true,
            })
        );

        urlSubject.next([{ path: 'query' }]);
        paramsSubject.next({ 'query-id': '123' });

        await component.ngOnInit();

        expect(component.isDuplicateMode).toBeFalse();
        expect(component.queryId).toBe('123');
        expect(component.queryGroupId).toBe('123');

        expect(mockQueryService.getQueryGroup).toHaveBeenCalledWith('123');

        expect(component.queryGrouName).toBe('Test Group');
        expect(component.queryGroupDesc).toBe('Test Desc');
        expect(component.readonlyMode).toBeFalse()

        expect(component.refreshContentGroups).toHaveBeenCalled();

    });

    it('[saveCurrentEditingGroupToDB] should set contentGroupNameError if name is missing', async () => {
        await component.ngOnInit();

        component.currentEditingCopy = { name: '', items: [{}] } as any;

        await component.saveCurrentEditingGroupToDB();

        expect(component.contentGroupNameError).toBeTrue();
        expect(mockQueryService.saveContentGroup).not.toHaveBeenCalled();
    });

    it('[saveCurrentEditingGroupToDB] should set contentGroupItemsError if items are empty', async () => {
        component.currentEditingCopy = { name: 'Test', items: [] } as any;

        await component.saveCurrentEditingGroupToDB();

        expect(component.contentGroupItemsError).toBeTrue();
        expect(mockQueryService.saveContentGroup).not.toHaveBeenCalled();
    });

    it('[saveCurrentEditingGroupToDB] should set contentParagraphError if paragraph is invalid', async () => {
        component.currentEditingCopy = {
            name: 'Test',
            items: [{ type: 'PARAGRAPH', heading: '', value: '' }],
        } as any;

        await component.saveCurrentEditingGroupToDB();

        expect(component.contentParagraphError).toBeTrue();
        expect(mockQueryService.saveContentGroup).not.toHaveBeenCalled();
    });


    it('[saveCurrentEditingGroupToDB] should set contentModuleLinkError if module link is invalid', async () => {
        component.currentEditingCopy = {
            name: 'Test',
            items: [{ type: 'MODULE_LINK', resourceId: null }],
        } as any;

        await component.saveCurrentEditingGroupToDB();

        expect(component.contentModuleLinkError).toBeTrue();
        expect(mockQueryService.saveContentGroup).not.toHaveBeenCalled();
    });


    it('[saveCurrentEditingGroupToDB] should save valid content group and update contentGroups', async () => {
        spyOn(component, 'cancelEditContent');

        component.currentEditingIndex = 0;
        component.currentEditingCopy = {
            id: null,
            name: 'Test',
            status: 'ACTIVE',
            items: [{ type: 'PARAGRAPH', heading: 'h', value: 'v' }],
        } as any;

        component.contentGroups.push(component.currentEditingCopy);

        mockQueryService.saveContentGroup.and.returnValue(of(99));

        await component.saveCurrentEditingGroupToDB();

        expect(mockQueryService.saveContentGroup).toHaveBeenCalled();
        expect(component.contentGroups[0].id).toBe(99);
        expect(mockAlertService.success).toHaveBeenCalledWith(
            'Content group saved!',
            null,
            null,
            'content group'
        );
        expect(component.cancelEditContent).toHaveBeenCalled();
    });

    it('[deleteContentGroup] should do nothing if user cancels', async () => {
        const group = { id: 1, name: 'Test' } as any;
        spyOn(window, 'confirm').and.returnValue(false); // or false if you want “Cancel”

        await component.deleteContentGroup(group);

        expect(component.contentGroups).toEqual([]);
        expect(mockQueryService.deleteContentGroupByID).not.toHaveBeenCalled();
        expect(mockAlertService.success).not.toHaveBeenCalled();
    });

    it('[deleteContentGroup] should remove a group without id', async () => {
        const group = { id: null, name: 'New' } as any;
        component.contentGroups.push(group);
        spyOn(window, 'confirm').and.returnValue(true); // or false if you want “Cancel”

        await component.deleteContentGroup(group);

        expect(component.contentGroups).not.toContain(group);
        expect(mockQueryService.deleteContentGroupByID).not.toHaveBeenCalled();
        expect(mockAlertService.success).not.toHaveBeenCalled();
    });

    it('[deleteContentGroup] should delete a persisted group and show success', async () => {
        spyOn(component, 'cancelEditContent');
        const group = { id: 123, name: 'Persisted' } as any;
        component.contentGroups.push(group);
        spyOn(window, 'confirm').and.returnValue(true); // or false if you want “Cancel”

        mockQueryService.deleteContentGroupByID.and.returnValue(of(null));

        await component.deleteContentGroup(group);

        expect(mockQueryService.deleteContentGroupByID).toHaveBeenCalledWith(123);
        expect(component.contentGroups).not.toContain(group);
        expect(mockAlertService.success).toHaveBeenCalledWith(
            'Content group deleted!',
            null,
            null,
            'content-group'
        );
        expect(component.cancelEditContent).toHaveBeenCalled();
    });


    it('should call alertService.error on delete failure', async () => {
        const group = { id: 123, name: 'FailGroup' } as any;
        component.contentGroups.push(group);
        spyOn(window, 'confirm').and.returnValue(true); // or false if you want “Cancel”

        mockQueryService.deleteContentGroupByID.and.returnValue(
            throwError(() => new Error('Delete failed'))
        );

        await component.deleteContentGroup(group);

        expect(mockAlertService.error).toHaveBeenCalledWith(
            'Failed to delete content group',
            null,
            'content-group'
        );
        expect(component.contentGroups).toContain(group);
    });


    it('[convertQuery] should convert a leaf query to QueryNode', () => {

        spyOn(component, 'convertComparisonOperator').and.callFake((op) => op);
        spyOn(component, 'convertTimeFrame').and.callFake((tf) => tf);
        const leafQuery: any = {
            field: 'age',
            operator: '>',
            timeFame: 'LAST_7_DAYS',
            value: 30,
            entity: 'user',
            rules: []
        };

        const result = component.convertQuery(leafQuery) as any;

        expect(result.query).toBeDefined();
        expect(result.query.field).toBe('AGE'); // uppercased
        expect(result.query.operator).toBe('>');
        expect(result.query.timeFrame).toBe('LAST_7_DAYS');
        expect(result.query.value).toBe(30);
        expect(result.query.entity).toBe('user');
    });

    it('[convertQuery] should convert a compound query with children', () => {

        spyOn(component, 'convertComparisonOperator').and.callFake((op) => op);
        spyOn(component, 'convertTimeFrame').and.callFake((tf) => tf);
        const compoundQuery: any = {
            condition: 'or',
            rules: [
                { field: 'age', operator: '>', value: 18, rules: [] },
                { field: 'score', operator: '<', value: 50, rules: [] }
            ]
        };

        const result = component.convertQuery(compoundQuery);

        expect(result.logic_operator).toBe('OR');
        expect(result.children.length).toBe(2);
        expect(result.children[0].query.field).toBe('AGE');
        expect(result.children[1].query.field).toBe('SCORE');
    });

    it('[convertQuery] should default to AND if condition is missing in compound query', () => {
        const query: any = {
            rules: [
                { field: 'age', operator: '=', value: 25, rules: [] }
            ]
        };

        const result = component.convertQuery(query);

        expect(result.logic_operator).toBe('AND');
    });




    it('[refreshContentGroups] should populate contentGroups with mapped data', async () => {
        paramsSubject.next({ 'query-id': 'group-123' });

        component.queryGroupId = 'group-123';

        component.refreshContentGroups();

        expect(component.contentGroups.length).toBe(2);

        expect(component.contentGroups[0]).toEqual({
            name: 'Group A',
            items: [{ id: 1, value: 'item1' }],
            queryGroupId: 'group-123',
            id: 10,
            status: 'ACTIVE',
        });

        expect(component.contentGroups[1]).toEqual({
            name: 'Group B',
            items: [],
            queryGroupId: 'group-123',
            id: 11,
            status: 'INACTIVE',
        });

        expect(component.isEditingContent).toBeFalse()
    })


    it('[saveQueryGroupToDB] should set error flags and stop if name is missing', async () => {
        component.queryGrouName = '   '; // empty
        await component.saveQueryGroupToDB();

        expect(component.groupNameError).toBeTrue();
        expect(mockQueryService.saveNewQueryGroup).not.toHaveBeenCalled();
    });

    it('[saveQueryGroupToDB] should set error flags and stop if description is missing', async () => {
        component.queryGroupDesc = '';
        await component.saveQueryGroupToDB();

        expect(component.groupDescError).toBeTrue();
        expect(mockQueryService.saveNewQueryGroup).not.toHaveBeenCalled();
    });

    it('[saveQueryGroupToDB] should set error if query is invalid', async () => {
        component.query = { rules: [] } as any;
        await component.saveQueryGroupToDB();

        expect(component.queryBuilderError).toBeTrue();
        expect(mockQueryService.saveNewQueryGroup).not.toHaveBeenCalled();
    });

    it('[saveQueryGroupToDB] should set error if validateQueryRules returns false', async () => {

        spyOn(component, 'validateQueryRules').and.returnValue(false);

        await component.saveQueryGroupToDB();

        expect(component.queryRulesError).toBeTrue();
        expect(mockQueryService.saveNewQueryGroup).not.toHaveBeenCalled();
    });


    it('(saveQueryGroupToDB) should call updateQueryGroup in editing mode', async () => {
        spyOn(component, 'validateQueryRules').and.returnValue(true);
        spyOn(component, 'updateIndividualQueries').and.returnValue({
            toPromise: () => Promise.resolve(null)
        });

        component.queryGroupId = '123';
        component.isDuplicateMode = false;
        component.isEditingMode = true;
        component.queryGrouName = 'Valid Name';
        component.queryGroupDesc = 'Valid Desc';
        component.query = { rules: [{ field: 'age', operator: '>', value: 18 }] } as any;



        mockQueryService.updateQueryGroup.and.returnValue(of(null));

        await component.saveQueryGroupToDB();

        expect(mockQueryService.updateQueryGroup).toHaveBeenCalledWith(
            { name: 'Valid Name', description: 'Valid Desc' },
            '123'
        );
        expect(component.alertService.success).toHaveBeenCalled();
    });

    it('(saveQueryGroupToDB) should call saveNewQueryGroup in create mode', async () => {
        spyOn(component, 'validateQueryRules').and.returnValue(true);
        spyOn(component, 'saveIndividualQueries').and.returnValue({
            toPromise: () => Promise.resolve(null)
        });
        component.queryGroupId = null;
        component.isDuplicateMode = false;
        component.isEditingMode = false;
        component.queryGrouName = 'Valid Name';
        component.queryGroupDesc = 'Valid Desc';
        component.query = { rules: [{ field: 'age', operator: '>', value: 18 }] } as any;

        mockQueryService.saveNewQueryGroup.and.returnValue(of('new-id'));

        await component.saveQueryGroupToDB();

        expect(mockQueryService.saveNewQueryGroup).toHaveBeenCalledWith({
            name: 'Valid Name',
            description: 'Valid Desc'
        });
        expect(component.alertService.success).toHaveBeenCalled();
        expect(component.queryGroupId).toBe('new-id');
    });


    it('[saveQueryGroupToDB] should call copyContentToDuplicatedQueryGroup and navigate in duplicate mode', async () => {

        spyOn(component, 'copyContentToDuplicatedQueryGroup').and.returnValue({
            toPromise: () => Promise.resolve(true)
        });
        spyOn(component, 'validateQueryRules').and.returnValue(true);
        spyOn(component, 'saveIndividualQueries').and.returnValue({
            toPromise: () => Promise.resolve(null)
        });

        component.queryGroupId = null;
        component.isDuplicateMode = true;
        component.queryGrouName = 'Valid Name';
        component.queryGroupDesc = 'Valid Desc';
        component.query = { rules: [{ field: 'age', operator: '>', value: 18 }] } as any;

        mockQueryService.saveNewQueryGroup.and.returnValue(of('dup-id'));

        await component.saveQueryGroupToDB();

        expect(component.copyContentToDuplicatedQueryGroup).toHaveBeenCalled();
        expect(mockRouter.navigate).toHaveBeenCalledWith(['edit-query', 'dup-id']);
    });


    it('[saveQueryGroupToDB] should handle duplicate name error (409)', async () => {

        spyOn(component, 'copyContentToDuplicatedQueryGroup').and.returnValue({
            toPromise: () => Promise.resolve(true)
        });
        spyOn(component, 'validateQueryRules').and.returnValue(true);
        spyOn(component, 'saveIndividualQueries').and.returnValue({
            toPromise: () => Promise.resolve(null)
        });
        component.queryGrouName = 'Valid Name';
        component.queryGroupDesc = 'Valid Desc';
        component.query = { rules: [{ field: 'age', operator: '>', value: 18 }] } as any;
        mockQueryService.saveNewQueryGroup.and.returnValue(
            throwError({ status: 409 })
        );

        component.queryGroupId = null;
        await component.saveQueryGroupToDB();

        expect(component.groupNameDuplicateError).toBeTrue();
        expect(component.queryGroupId).toBeNull();
    });

    it('[saveQueryGroupToDB] should handle duplicate name error (already exists string)', async () => {
        spyOn(component, 'copyContentToDuplicatedQueryGroup').and.returnValue({
            toPromise: () => Promise.resolve(true)
        });
        spyOn(component, 'validateQueryRules').and.returnValue(true);
        spyOn(component, 'saveIndividualQueries').and.returnValue({
            toPromise: () => Promise.resolve(null)
        });

        component.queryGrouName = 'Valid Name';
        component.queryGroupDesc = 'Valid Desc';
        component.query = { rules: [{ field: 'age', operator: '>', value: 18 }] } as any;

        mockQueryService.saveNewQueryGroup.and.returnValue(
            throwError({ message: 'Query group already exists' })
        );

        component.queryGroupId = null;
        await component.saveQueryGroupToDB();

        expect(component.groupNameDuplicateError).toBeTrue();
    });

    it('[saveIndividualQueries] should call saveQueryLogic with correct payload', () => {
        mockQueryService.saveQueryLogic.and.returnValue(of('saved'));


        spyOn(component, 'convertQuery').and.callFake((query) => ({
            logic_operator: query.condition?.toUpperCase() || 'AND',
            children: []
        }));

        component.queryGroupId = 'group-123';
        component.query = {
            rules: [
                { field: 'age', operator: '>', value: 18 }
            ],
            condition: 'and'
        };


        component.saveIndividualQueries()

        expect(mockQueryService.saveQueryLogic).toHaveBeenCalledWith({
            queryGroupId: 'group-123',
            logic_operator: 'AND',
            children: []
        });
    });


    it('[updateIndividualQueries] should call updateQueryLogic with correct payload', () => {
        mockQueryService.updateQueryLogic.and.returnValue(of('saved'));

        spyOn(component, 'convertQuery').and.callFake((query) => ({
            logic_operator: query.condition?.toUpperCase() || 'AND',
            children: []
        }));

        component.queryGroupId = 'group-123';
        component.query = {
            rules: [
                { field: 'age', operator: '>', value: 18 }
            ],
            condition: 'and'
        };

        component.updateIndividualQueries()


        expect(mockQueryService.updateQueryLogic).toHaveBeenCalledWith({
            queryGroupId: 'group-123',
            logic_operator: 'AND',
            children: []
        });
    });



});
