import { Component, ViewChild, AfterViewInit } from '@angular/core';

import {
    QueryBuilderClassNames,
    QueryBuilderConfig,
} from '@uom-digital-health-software/ngx-angular-query-builder';
import { FormBuilder, FormControl, NgForm } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Location } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { QueriesService } from './queries.service';
import { ContentComponent } from './content/content.component';



import {
    Question,
    QueryDTO,
    QueryNode,
    QueryString,
    ContentType,
    ContentGroup,
    ContentGroupStatus,
} from './queries.model';
import { Observable, forkJoin } from 'rxjs';

const sliderOptions = Array.from({ length: 7 }, (_, i) => {
    const val = String(i + 1);
    return { name: val, value: val };
});

@Component({
    selector: 'jhi-queries',
    templateUrl: './addQuery.component.html',
    styleUrls: ['../../../content/scss/queries.scss'],
})
export class AddQueryComponent {
    public contentGroups: ContentGroup[] = [];

    @ViewChild(ContentComponent) contentComponent!: ContentComponent;

    queryBuilderFormGroup: NgForm;

    public queryCtrl: FormControl;

    public queryGrouName: string;

    public queryGroupDesc: string;

    public isLoaded: Boolean = false;

    public queryGroupId: number | any | null;

    public bootstrapClassNames: QueryBuilderClassNames = {
        removeIcon: 'fa fa-minus',
        addIcon: 'fa fa-plus',
        arrowIcon: 'fa fa-chevron-right px-2',
        button: 'btn',
        buttonGroup: 'btn-group',
        rightAlign: 'order-12 ml-auto',
        switchRow: 'd-flex px-2',
        switchGroup: 'd-flex align-items-center',
        switchRadio: 'custom-control-input',
        switchLabel: 'custom-control-label',
        switchControl: 'custom-control custom-radio custom-control-inline',
        row: 'row p-2 m-1',
        rule: 'border',
        ruleSet: 'border',
        invalidRuleSet: 'alert alert-danger',
        emptyWarning: 'text-danger mx-auto',
        operatorControl: 'query-builder-field',
        operatorControlSize: 'col-auto pr-0',
        fieldControl: 'query-builder-field',
        fieldControlSize: 'col-auto pr-0',
        entityControl: 'query-builder-field',
        entityControlSize: 'col-auto pr-0',
        inputControl: 'query-builder-field',
        inputControlSize: 'col-auto pr-0',
    };

    public query: QueryString | any = {
        condition: 'and',
        rules: [],
    };

    public config: QueryBuilderConfig = {
        entities: {
        },
        fields: {
        },
    };

    public currentConfig: QueryBuilderConfig;
    public allowRuleset: boolean = true;
    public allowCollapse: boolean;
    public questionnare: Question[]
    public delusions: Question[]
    public persistValueOnFieldChange: boolean = true;

    selectedGroupIndex: number | null = null;

    isEditingContent = false;
    currentEditingIndex: number | null = null;
    currentEditingCopy: ContentGroup | null = null;

    queryId = null;

    public readonlyMode = false;
    public groupNameError = false;
    public groupDescError = false;
    public queryBuilderError = false;
    public groupNameDuplicateError = false;
    public contentGroupNameError = false;
    public contentGroupItemsError = false;
    public queryRulesError = false;
    public contentParagraphError = false;
    public contentModuleLinkError = false;

    public isDuplicateMode = false;

    public isEditingMode = false;
    private deletedContentGroupIds: number[] = [];

    constructor(
        private queryService: QueriesService,
        private formBuilder: FormBuilder,
        private http: HttpClient,
        private router: Router,
        private readonly location: Location,
        private route: ActivatedRoute
    ) {
        this.queryCtrl = this.formBuilder.control(this.query);
        this.currentConfig = this.config;

        let physicalTypesPromise = this.queryService.gellAllPhysicalTypes().toPromise()
        let questionnaireItems = this.queryService.getQuestionnaireItems().toPromise()
        let entitiesPromise =  this.queryService.getEntities().toPromise()

        Promise.all([physicalTypesPromise, questionnaireItems, entitiesPromise]).then(allTypes => {
            let physicalTypes = allTypes[0] as any
            let questionnareTypes = allTypes[1]
            let entities = allTypes[2]

            this.questionnare = questionnareTypes["questionnaire"] as Question[]
            this.delusions = questionnareTypes["delusions"] as Question[]

            this.addQuestionnaireItemsToQueryBuilder();
            this.addDelusionsToQueryBuilder();
            this.config.entities = { ...entities }
            this.config.fields = { ...this.config.fields, ...physicalTypes }

            this.isLoaded = true

        })
    }

    private async addQuestionnaireItemsToQueryBuilder() {
        // histogram to include only
        let histogramQuestionsToInclude = [
            'whereabouts_1',
            'sleep_5',
            'social_1',
        ];

        for (const question of this.questionnare) {
            let fieldName = question.field_name;

            const field = {
                name: `${question.field_label} ${question.field_sublabel ? question.field_sublabel : ''
                    }`,
                type: 'category',
                entity: 'QUESTIONNAIRE_SLIDER',
                operators: ['=', '!=', '<', '>', '<=', '>='],
            };
            if (question.field_type == 'slider') {
                field['options'] = sliderOptions;
            } else if (
                histogramQuestionsToInclude.includes(question.field_name)
            ) {
                fieldName = question.field_name.split('_')[0];
                field.name = `${question.field_label}`;
                field.entity = 'QUESTIONNAIRE_HISTOGRAM';
                field.operators = ['IS'];
                let mappedOptions = question.select_choices_or_calculations.map(
                    (item) => {
                        return {
                            name: item.label,
                            value: item.code,
                        };
                    }
                );
                field['options'] = mappedOptions;
            }

            if (!this.config.fields[question.group_name]) {
                const group = {
                    name: `${question.group_name}`,
                    type: 'category',
                    entity: 'QUESTIONNAIRE_GROUP',
                    operators: ['=', '!=', '<', '>', '<=', '>='],
                    options: sliderOptions,
                };
                this.config.fields[question.group_name] = group;
            }
            this.config.fields[fieldName] = field;


        }
    }

    private addDelusionsToQueryBuilder() {
        for (const delusion of this.delusions) {
            const field = {
                name: `${delusion.field_label} ${delusion.field_sublabel ? delusion.field_sublabel : ''
                    }`,
                type: 'category',
                entity: 'QUESTIONNAIRE_DELUSIONS',
                options: sliderOptions,
            };

            this.config.fields[delusion.field_name] = field;
        }
    }

    async ngOnInit() {
        this.route.url.subscribe((urlSegments) => {
            this.isDuplicateMode = urlSegments.some(
                (seg) => seg.path === 'duplicateQuery'
            );
        });

        this.route.params.subscribe((params) => {
            this.queryId = params['query-id'];
            this.queryGroupId = this.queryId;

            if (this.queryId) {
                this.queryService
                    .getQueryGroup(this.queryId)
                    .subscribe((response: any) => {
                        this.query = response;
                        this.queryGrouName = response.queryGroupName;
                        this.queryGroupDesc = response.queryGroupDescription;

                        if (this.isDuplicateMode) {
                            this.queryGrouName += '_duplicate';
                            this.query.canEdit = true;
                        }
                        if (!this.query.canEdit) this.readonlyMode = true;
                    });

                this.refreshContentGroups();
            }
        });
    }

    refreshContentGroups() {
        this.queryService
            .getAllQueryContentsAndGroups(this.queryId)
            .subscribe((response: any) => {
                this.contentGroups = response.map((group: any) => ({
                    name: group.contentGroupName,
                    items: group.queryContentDTOList || [],
                    queryGroupId: group.queryGroupId,
                    id: group.id,
                    status: group.status || 'INACTIVE',
                }));

                if (this.contentGroups.length > 0) {
                    this.selectedGroupIndex = 0;
                }
            });
        //when refreshing, hide add content section
        this.isEditingContent = false;
    }

    changeDisabled(event: Event) {
        (<HTMLInputElement>event.target).checked
            ? this.queryCtrl.disable()
            : this.queryCtrl.enable();
    }

    private _counter = 0;
    formRuleWeakMap = new WeakMap();

    getUniqueName(prefix: string, rule: any) {
        if (!this.formRuleWeakMap.has(rule)) {
            this.formRuleWeakMap.set(rule, `${prefix}-${++this._counter}`);
        }

        return this.formRuleWeakMap.get(rule);
    }

    convertComparisonOperator(value?: string) {
        switch (value) {
            case '>=':
                return 'GREATER_THAN_OR_EQUALS';
            case '=':
                return 'EQUALS';
            case '!=':
                return 'NOT_EQUALS';
            case '>':
                return 'GREATER_THAN';
            case '<':
                return 'LESS_THAN';
            case '<=':
                return 'LESS_THAN_OR_EQUALS';
            case 'IS':
                return 'IS';

            default:
                return null;
        }
    }

    convertTimeFrame(value: string) {
   
        switch (value) {
            case '6_months':
                return 'PAST_6_MONTH';
            case '1_months':
                return 'PAST_MONTH';
            case '1_years':
                return 'PAST_YEAR';
            case '1_weeks':
                return 'PAST_WEEK';
            default:
                return null;
        }
    }

    convertQuery(query: QueryString): QueryNode {
        if (query.rules && query.rules.length > 0) {
            return {
                logic_operator: query.condition?.toUpperCase() || 'AND',
                children: query.rules.map((rule) => this.convertQuery(rule)),
            };
        } else {
            const queryDTO: QueryDTO = {
                field: query.field?.toUpperCase() || '',
                operator: this.convertComparisonOperator(query.operator),
                timeFrame: this.convertTimeFrame(query.timeFame),
                value: query.value,
                entity: query.entity,
            };

            return {
                query: queryDTO,
            };
        }
    }

    validateQueryRules(rules: any[]): boolean {
        for (const rule of rules) {
            if (rule.rules && Array.isArray(rule.rules)) {
                if (!this.validateQueryRules(rule.rules)) {
                    return false;
                }
            } else {
                if (
                    rule.value === undefined ||
                    rule.value === null ||
                    rule.value.toString().trim() === '' ||
                    rule.timeFame === undefined ||
                    rule.timeFame === null
                ) {
                    return false;
                }
            }
        }
        return true;
    }

    async saveQueryGroupToDB() {
        this.groupNameError = false;
        this.groupDescError = false;
        this.queryBuilderError = false;
        this.groupNameDuplicateError = false;
        this.queryRulesError = false;

        let hasError = false;

        this.isEditingMode = this.queryGroupId && !this.isDuplicateMode;

        if (!this.queryGrouName?.trim()) {
            this.groupNameError = true;
            hasError = true;
        }

        if (!this.queryGroupDesc?.trim()) {
            this.groupDescError = true;
            hasError = true;
        }

        if (
            !this.query ||
            !Array.isArray(this.query.rules) ||
            this.query.rules.length === 0
        ) {
            this.queryBuilderError = true;
            hasError = true;
        }

        if (!this.validateQueryRules(this.query.rules)) {
            this.queryRulesError = true;
            hasError = true;
        }

        if (hasError) return;

        try {
            if (this.isEditingMode) {
                await this.queryService
                    .updateQueryGroup(
                        {
                            name: this.queryGrouName,
                            description: this.queryGroupDesc,
                        },
                        this.queryGroupId
                    )
                    .toPromise();
                await this.updateIndividualQueries().toPromise();
            } else {
                this.queryGroupId = await this.queryService
                    .saveNewQueryGroup({
                        name: this.queryGrouName,
                        description: this.queryGroupDesc,
                    })
                    .toPromise();
                await this.saveIndividualQueries().toPromise();
            }

            await this.submitContentChanges().toPromise();
            this.deletedContentGroupIds = [];

            this.router.navigate(['querygroups']);
        } catch (err: any) {
            if (
                err?.status === 409 ||
                err?.message?.includes('already exists')
            ) {
                this.groupNameDuplicateError = true;
                if (!this.isEditingMode) {
                    this.queryGroupId = null;
                }
            } else {
                console.error('Unexpected error saving query group:', err);
            }

            return;
        }
    }

    submitContentChanges(): Observable<any> {
        const saveRequests = this.contentGroups.map((group) => {
            let payload = null;
            if (this.isDuplicateMode) {
                // if is dupilicating query, need to create new content groups
                payload = {
                    queryGroupId: this.queryGroupId,
                    contentGroupName: group.name,
                    queryContentDTOList: group.items,
                    status: group.status,
                };
            } else {
                payload = {
                    id: group.id,
                    queryGroupId: this.queryGroupId,
                    contentGroupName: group.name,
                    queryContentDTOList: group.items,
                    status: group.status,
                };
            }
            return this.queryService.saveContentGroup(payload);
        });

        const deleteRequests = this.deletedContentGroupIds.map((id) =>
            this.queryService.deleteContentGroupByID(id)
        );

        return forkJoin([...saveRequests, ...deleteRequests]);
    }

    addContentGroup() {
        this.currentEditingIndex = this.contentGroups.length;
        this.currentEditingCopy = {
            name: '',
            items: [
                {
                    type: ContentType.TITLE,
                    value: 'this is title',
                },
            ],
            queryGroupId: null,
            id: null,
            status: ContentGroupStatus.INACTIVE,
        };
        this.isEditingContent = true;
    }

    deleteContentGroup(id: number) {
        const confirmDelete = confirm(
            "Are you sure you want to delete this? This will also delete the content from the participants' phones."
        );
        if (!confirmDelete) return;

        this.deletedContentGroupIds.push(id);
        this.contentGroups = this.contentGroups.filter(
            (group) => group.id !== id
        );
    }

    selectGroup(index: number) {
        this.selectedGroupIndex = index;

        this.currentEditingIndex = index;
        const original = this.contentGroups[index];
        this.currentEditingCopy = {
            name: original.name,
            items: original.items.map((item) => ({ ...item })),
            queryGroupId: original.queryGroupId,
            id: original.id,
            status: original.status,
        };
        this.isEditingContent = true;
    }

    saveCurrentEditingGroup() {
        let hasError = false;
        this.contentGroupNameError = false;
        this.contentGroupItemsError = false;
        this.contentParagraphError = false;
        this.contentModuleLinkError = false;

        if (
            !this.currentEditingCopy?.name ||
            !this.currentEditingCopy.name.trim()
        ) {
            this.contentGroupNameError = true;
            hasError = true;

            return;
        }
        if (
            !this.currentEditingCopy.items ||
            this.currentEditingCopy.items.length === 0
        ) {
            this.contentGroupItemsError = true;
            hasError = true;

            return;
        }

        for (const item of this.currentEditingCopy.items) {
            if (item.type === 'PARAGRAPH') {
                const headingValid = item.heading && item.heading.trim() !== '';
                const valueValid =
                    typeof item.value === 'string'
                        ? item.value.trim() !== ''
                        : item.value !== undefined && item.value !== null;

                if (!headingValid || !valueValid) {
                    this.contentParagraphError = true;
                    hasError = true;
                    break;
                }
            }
            if (item.type === 'MODULE_LINK') {
                if (item.resourceId === null || item.resourceId === undefined) {
                    this.contentModuleLinkError = true;
                    hasError = true;
                    break;
                }
            }
        }

        if (hasError) {
            return;
        }

        if (this.currentEditingIndex !== null && this.currentEditingCopy) {
            this.contentGroups[this.currentEditingIndex] =
                this.currentEditingCopy;
            this.isEditingContent = false;
            this.currentEditingIndex = null;
            this.currentEditingCopy = null;
        }
    }

    cancelEditContent() {
        this.isEditingContent = false;
        this.currentEditingIndex = null;
        this.currentEditingCopy = null;
    }

    saveIndividualQueries() {
        const query_logic = {
            queryGroupId: this.queryGroupId,
            ...this.convertQuery(this.query),
        };
        return this.queryService.saveQueryLogic(query_logic);
    }

    updateIndividualQueries() {
        const query_logic = {
            queryGroupId: this.queryGroupId,
            ...this.convertQuery(this.query),
        };
        return this.queryService.updateQueryLogic(query_logic);
    }

    onToggleStatus(contentGroup: any) {
        const newStatus =
            contentGroup.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
        if (
            newStatus === 'INACTIVE' &&
            !confirm(
                "Are you sure? This will prevent it from displaying on participants' phones."
            )
        ) {
            return;
        }

        contentGroup.status = newStatus;
    }
}
