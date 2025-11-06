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

        if (this.chart && this.chart.data) {
            this.chart.data.labels.forEach((label, index) => {


            const [abbr, year] = label.split(" ");

            const monthMap = {
            Jan: "January",
            Feb: "February",
            Mar: "March",
            Apr: "April",
            May: "May",
            Jun: "June",
            Jul: "July",
            Aug: "August",
            Sep: "September",
            Oct: "October",
            Nov: "November",
            Dec: "December"
            };

            const fullMonth = monthMap[abbr];
            let data = this.chart.data.datasets[0].data[index]

            this.months.push({name: fullMonth, count: data == 0 ? "No data" : data})
        })
        }


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
