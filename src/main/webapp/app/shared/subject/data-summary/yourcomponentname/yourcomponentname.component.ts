import { Component, OnInit, Input, AfterViewInit } from '@angular/core';
import Chart from 'chart.js/auto';
import { ChartConfiguration, registerables } from 'chart.js';
@Component({
  selector: 'app-single-graph',
  templateUrl: './yourcomponentname.component.html',
  styleUrls: ['./../data-summary.component.scss']
})
export class YOURCOMPONENTNAMEComponent implements OnInit {

    constructor() {}

    months = [
        { name: "January", count: 24 },
        { name: "February", count: 24 },
        { name: "March", count: 24 },
        { name: "April", count: 24 },
        {name: "May", count: 24},
        {name: "June", count: 24},
        {name: "July", count: 24},
        {name: "August", count: 24},
        {name: "September", count: 24},
        {name: "October", count: 24},
        {name: "November", count: 24},
        {name: "December", count: 24},


    ];
    @Input() description1: string = ''
    @Input() description2: string = ''

    @Input() title: string = '';

    @Input() chart: any = {};

    @Input() showTotal: boolean = false;

    @Input() total: Number = 244000;

    @Input() totalValue: Number = 3;

    @Input() totalAverage: Number = 3;

    @Input() showTotalAverage: boolean = true;

    @Input() isSingleRow: boolean = true;

    @Input() logoClass = '';

    @Input() color: string = '';

    @Input() css: string = '';

    @Input() applyCustomColour = false;

    @Input() longHeading: boolean = false;

    @Input() showMonthlyAverage: boolean = true;

    @Input() totalAverageText = "Total"


    ngAfterViewInit() {
        //@ts-ignore
        // const ctx = document.getElementById(this.charxtId).getContext('2d');

        if (this.applyCustomColour) {
            this.chart.data.datasets[0]['backgroundColor'] = [
                this.color.replace('0.9', '0.6'),
            ];
        }

    }
    chartGraph: any = {};
    @Input() chartId: string = '';
    ngOnInit(): void {}

}
