import { Component, OnInit, Output, EventEmitter, Input } from '@angular/core';
import { QueriesService } from '../../queries.service';
import { ContentItem, ModuleGroup } from '../../queries.model';

@Component({
    selector: 'query-modulelink-item',
    templateUrl: './modulelink-item.component.html',
    styleUrls: ['./modulelink-item.component.scss'],
})
export class ModulelinkItemComponent implements OnInit {
    @Output() triggerDeleteItemFunction = new EventEmitter<string>();
    @Input() item: ContentItem;

    moduleGroups: ModuleGroup[] = [];
    selectedModuleId: number;

    constructor(private queryService: QueriesService) {}

    ngOnInit(): void {
        this.queryService
            .getModules()
            .toPromise()
            .then((response) => {
                this.moduleGroups = response as ModuleGroup[];

                if (this.item.resourceId) {
                    this.selectedModuleId = this.item.resourceId;
                }
            });
    }

    changeModule() {
        this.item.resourceId = this.selectedModuleId;
    }

    onDeleteItem() {
        this.triggerDeleteItemFunction.emit();
    }
}
