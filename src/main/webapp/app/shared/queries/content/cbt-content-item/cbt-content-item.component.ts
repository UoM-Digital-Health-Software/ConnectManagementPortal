import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CBT_CONTENT_TYPE, CBTContent, ConditionalResponse, ContentItem } from '../../queries.model';
import { QueriesService } from '../../queries.service';

@Component({
  selector: 'cbt-content-item',
  templateUrl: './cbt-content-item.component.html',
  styleUrls: ['./cbt-content-item.component.scss']
})
export class CbtContentItemComponent implements OnInit {

  @Input() item: ContentItem | undefined
  @Output() triggerDeleteItemFunction = new EventEmitter<string>();


  CBT_CONTENT_TYPE = CBT_CONTENT_TYPE;
  contentTypes = Object.keys(CBT_CONTENT_TYPE).filter(key => isNaN(Number(key)));
  currentCBTContent: CBTContent[] | null = null;
  selectedType: string | null = null;
  selectedItem: ConditionalResponse | null = null;
  selectedHtml = '';
  confirmedItem: ConditionalResponse | null = null;
  randomSelection = false;
  selectionConfirmed = false;

  constructor(private queryService: QueriesService
  ) { }

  ngOnInit(): void {

    if(this.item?.cbtType) {
      this.selectedType = this.item.cbtType

      var content = this.queryService.getCBTContent(this.selectedType).toPromise();

      content.then((data) => {
        this.currentCBTContent = data as CBTContent[]
        if(this.item?.cbtRoute){
          const matchingResponse = this.currentCBTContent[0].conditionalResponses.find(response => response.route === this.item?.cbtRoute);
          if(matchingResponse){
            this.selectItem(matchingResponse)
            this.confirmSelection()
          }
        }
      }
      )
    }
  }

  tooltipVisible = false;
  tooltipHtml = '';
  tooltipResponse: ConditionalResponse | null = null;

  showTooltip(response: ConditionalResponse) {
    const itemsHtml = response.items.map(item => `<div>${item.text}</div>`).join('');
    this.tooltipHtml = `<strong>Route:</strong> ${response.route}<br>${itemsHtml}`;
    this.tooltipResponse = response;
    this.tooltipVisible = true;
  }

  hideTooltip() {
    this.tooltipVisible = false;
    this.tooltipResponse = null;
  }


  selectContentType(type: string) {
    this.selectedType = type;

    var content = this.queryService.getCBTContent(type).toPromise();

    content.then((data) => {
      this.currentCBTContent = data as CBTContent[]
    })
  }

  selectItem(response: ConditionalResponse) {
    this.selectedItem = response;
    const itemsHtml = response.items.map(item => `<div>${item.text}</div>`).join('');
    this.selectedHtml = `${itemsHtml}`;
  }

  pickRandom() {
    this.randomSelection = true;
    this.selectionConfirmed = true;
    this.confirmedItem = null;
    this.selectedItem = null;
    this.selectedHtml = '';
  }

  confirmSelection() {
    if (!this.selectedItem) {
      return;
    }
    this.confirmedItem = this.selectedItem;
    this.selectionConfirmed = true;
    this.randomSelection = false;

    this.initialiseItemObject();
  }

  cancelSelection() {
    this.selectionConfirmed = false;
    this.confirmedItem = null;
    this.randomSelection = false;
    this.selectedItem = null;
    this.selectedHtml = '';

    this.resetItemObject();
  }


  resetItemObject() {
    this.item!.cbtType = undefined
    this.item!.cbtVersion = undefined
    this.item!.cbtRoute = undefined
  }

  initialiseItemObject(){
    if(this.confirmedItem != null){
      this.item!.cbtType = this.selectedType
      this.item!.cbtVersion = this.currentCBTContent![0].questionSourceUUID
      this.item!.cbtRoute = this.confirmedItem.route;
    }
  }

  onDeleteItem() {
        this.triggerDeleteItemFunction.emit()
  }

}
