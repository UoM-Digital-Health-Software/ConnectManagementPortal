import { Component, OnInit, Input, AfterViewInit } from '@angular/core';
import Chart from 'chart.js/auto';
import { ChartConfiguration, registerables } from 'chart.js';
@Component({
    selector: 'app-single-row-graph',
    templateUrl: './single-row-graph.component.html',
    styleUrls: ['./../data-summary.component.scss'],
})
export class SingleRowGraphComponent implements OnInit {
    constructor() {}


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
        const ctx = document.getElementById(this.chartId)?.getContext('2d');

        const gradient = ctx.createLinearGradient(0, 0, 0, 400);
        gradient.addColorStop(0, this.color);
        gradient.addColorStop(1, 'rgba(255, 255, 255, 0.0)');




        if (this.applyCustomColour) {
            this.chart.data.datasets[0]['borderColor'] =  this.color
            this.chart.data.datasets[0]['backgroundColor'] = gradient
        }

        this.chartGraph = new Chart(this.chartId, this.chart);
    }
    chartGraph: any = {};
    @Input() chartId: string = '';
    ngOnInit(): void {}
}
