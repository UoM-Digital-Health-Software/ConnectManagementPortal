import { Subject } from '../subject';
import { User } from '../user/user.model';

type optionalString = string | null | undefined;

export interface CBTContent {
  name: string;
  questionSourceUUID: string;
  conditionalResponses: ConditionalResponse[];
}

export interface ConditionalResponse {
  route: string;
  items: CBTContentItem[];
}

export interface CBTContentItem {
  index: string;
  type: string;
  text: string;
}

export enum CBT_CONTENT_TYPE {
    CANNABIS,
    FEELING_CRITICISED,
    SLEEP,
    SUSPICIOUS_THOUGHTS,
    VOICES,
    GETTING_OUT
}




export interface QueryGroup {
    id?: any;
    name?: string;
    description?: string;
    createdDate?: Date;
    updatedDate?: Date;
    createdBy?: Subject;
    updatedBy?: Subject;
    isArchived?: boolean;
    canEdit?: boolean;
    queryGroupId?: any;
}

export interface QueryParticipant {
    id?: any;
    queryGroupId?: number;
    subjectId?: number;
    createdBy?: User;
}

export type QueryNode =
    | {
          query: QueryDTO;
      }
    | {
          logic_operator: string;
          children: QueryNode[];
      };

export interface QueryDTO {
    field?: optionalString;
    operator?: optionalString;
    timeFrame?: optionalString;
    value?: optionalString;
    logic_operator?: optionalString;
    children?: QueryDTO[];
    entity?: optionalString;
    referenceType?: optionalString;
    rollingWindow?: optionalString
}


export interface QueryParticipant {
    id?: any;
    queryGroupId?: number;
    subjectId?: number;
    createdBy?: User;
    queryGroupName?: string;
    assignedDate?: Date;
}

export interface QueryString {
    field?: string;
    operator?: string;
    timeFame?: any;
    value?: any;
    rules?: QueryString[];
    condition?: string;
    entity?: string;
    referenceType?: string;
    rollingWindow?: number;
}

export enum ContentType {
    TITLE = 'TITLE',
    PARAGRAPH = 'PARAGRAPH',
    IMAGE = 'IMAGE',
    VIDEO = 'VIDEO',
    MODULE_LINK = 'MODULE_LINK',
    CBT_CONTENT = "CBT_CONTENT"
}

export interface ContentItem {
    id?: number;
    heading?: string;
    type: ContentType;
    value?: string | Number;
    imageValue?: string;
    imageBlob?: string;
    isValidImage?: boolean;
    queryGroupId?: number;
    resourceId?: number;
    cbtType?: string | undefined | null;
    cbtVersion?: string;
    cbtRoute?: string;
    cbtRouteSelectionMode?: "RANDOM" | "FIXED"
}

export interface ContentGroup {
    name: string;
    items: ContentItem[];
    queryGroupId: number;
    id: number;
    status: ContentGroupStatus;
}

export enum ContentGroupStatus {
    ACTIVE,
    INACTIVE,
}

export interface Module {
    id?: number;
    name?: string;
}

export interface ModuleGroup {
    groupName: string;
    modules: Module[];
}

export interface RadioQuestionType {
    code: string;
    label: string;
}

export interface Question {
    field_name?: string;
    form_name?: string;
    field_type?: 'slider' | 'radio';
    field_label?: string;
    field_sublabel?: string;
    identifier?: string;
    group_name?: string;
    branching_logic?: string;
    select_choices_or_calculations?: RadioQuestionType[];
    section_header?: string;
    field_note?: string;
    text_validation_type_or_show_slider_number?: string;
    text_validation_min?: string;
    text_validation_max?: string;
    required_field?: string;
    custom_alignment?: string;
    question_number?: string;
    matrix_group_name?: string;
    matrix_ranking?: string;
    field_annotation?: string;
    evaluated_logic?: string;
    left_label?: string;
    right_label?: string;
    min_slider_value?: string;
    max_slider_value?: string;
    step?: string;
}
