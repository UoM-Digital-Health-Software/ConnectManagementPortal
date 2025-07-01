
import { Component, OnInit, AfterViewInit, Output, EventEmitter, Input } from '@angular/core';
import { QueriesService } from '../../queries.service';
import { ContentItem, Module } from '../../queries.model';

@Component({
    selector: 'query-modulelink-item',
    templateUrl: './modulelink-item.component.html',
    styleUrls: ['./modulelink-item.component.scss']
})
export class ModulelinkItemComponent implements OnInit {

       @Output() triggerDeleteItemFunction = new EventEmitter<string>();

    @Input() item: ContentItem
    modules: Module[]
    selectedModule: Module


    constructor(private queryService: QueriesService) { }

    ngOnInit(): void {
        this.queryService.getModules().toPromise().then((response) => {
            this.modules = response as Module[]

            if (this.item.resourceId) {
                const result = this.modules.filter((module) => module.id == this.item.resourceId)
                if (result && result.length > 0) {
                    this.selectedModule = result[0]
                }
            }
        })
    }

    changeModule() {
        this.item.resourceId = this.selectedModule.id
    }
    onDeleteItem() {
        this.triggerDeleteItemFunction.emit()
    }

}
