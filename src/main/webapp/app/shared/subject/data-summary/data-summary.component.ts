import { Component, OnInit } from '@angular/core';


import Chart from 'chart.js/auto';
import { ChartConfiguration, registerables } from 'chart.js';
import ChartDataLabels from 'chartjs-plugin-datalabels';
Chart.register(...registerables, ChartDataLabels);
import { Graph } from './types';


@Component({
  selector: 'app-data-summary',
  templateUrl: './data-summary.component.html',
  styleUrls: ['./data-summary.component.scss']
})



export class DataSummaryComponent implements OnInit {
  title = 'ng-chart';
  chart: any = [];
    chart_heart_rate: any = [];



  chart_heart_rate_variability: any = [];
    chart_type:  { [id: string] : Graph; } = {
        questionnaire: { type: "bar", showScaleY: false, showDataTables: true},
        heart_rate: {type: "line", showScaleY: false, showDataTables: true},
        hrv: { type: "line", showScaleY: false, showDataTables: true},
        sleep_period: { type: "line", showScaleY: false, showDataTables: true },
        steps: {type: "bar", color: 'rgb(230, 211, 243)', showScaleY: false, showDataTables: true} ,
        activity: { type: "line", color: 'rgb(12,132, 143)', showScaleY: false, showDataTables: true},
        respiratory_rate: { type: "line", color: 'rgb(12,132, 143)', showScaleY: false, showDataTables: true },
        negative_emotions_months: { type: "line", showScaleY: false, showDataTables: true },
        negative_emotions_weeks: { type: "line", showScaleY: true, timeframe: "week", showDataTables: false,  color: 'rgb(12,132, 143)' },
        positive_emotions_months: { type: "line", showScaleY: false, showDataTables: true },

    }
   data : any = {
        questionnaire: [10,22,15,16,6,2,30,22,11,4,3,6],
        heart_rate: [75.23, 83.13, 67.34, 79.95, 69.41, 64.01, 75.2, 66.21, 62.51, 72.69,65.44, 70.23],
        hrv: [11, 13, 20, 20, 26, 18, 20, 17, 29, 10, 14, 10],
     //   sleep_period: [6.37, 7.04, 7.85, 8.16, 7.72, 7.14, 7.52, 6.8, 8.07, 7] ,
        steps: [7356.28, 9451.29, 7252.24, 7329.69, 7887.05, 8708.06, 8767.04, 7608.88, 9464.52, 7003.14, 4333, 5673] ,
        activity: [24, 10, 30, 21, 10, 15, 28, 12, 15, 28, 28, 22],
       respiratory_rate: [15, 18, 12, 19, 14, 17, 20, 13, 16, 18, 12, 15],
       negative_emotions_months: [5, 10, 11, 7, 9, 14, 16, 20, 30, 35, 29, 25],
       negative_emotions_weeks: Array.from({ length: 52 }, () => Math.floor(Math.random() * (35 - 5 + 1)) + 5),
       positive_emotions_months: Array.from({ length: 12 }, () => Math.floor(Math.random() * (35 - 5 + 1)) + 5)

   }

    charts: any = {}


    constructor() { }

    createMonthHistogram() {
        // Sample data for each month
        const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
        const binLabels = ["0-2", "2-4", "4-6", "6-8", "8-10", "10-12", "12-14", "14+"];

        // Define colors for each bin
        const binColors = [
            'rgba(255, 99, 132, 0.6)',  // 0-2
            'rgba(54, 162, 235, 0.6)',  // 2-4
            'rgba(255, 206, 86, 0.6)',  // 4-6
            'rgba(75, 192, 192, 0.6)',  // 6-8
            'rgba(153, 102, 255, 0.6)', // 8-10
            'rgba(255, 159, 64, 0.6)',  // 10-12
            'rgba(199, 199, 199, 0.6)', // 12-14
            'rgba(83, 102, 255, 0.6)'   // 14+
        ];

        // Generate random data for each month
        const data = binLabels.map(() => Array.from({ length: 12 }, () => Math.floor(Math.random() * 10 + 1)));
//@ts-ignore
        // Create histogram chart
        const ctx = document.getElementById('histogramChart').getContext('2d');
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: months,
                datasets: binLabels.map((label, index) => ({
                    label: label,
                    data: data[index],
                    backgroundColor: binColors[index],
                    borderColor: binColors[index].replace('0.6', '1'),
                    borderWidth: 1
                }))
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                },
                plugins: {
                    datalabels: {
                        display: false
                    }
                }
            }
        });
    }

    createHistogram() {

      // Sample data
      const rawData = [12, 19, 3, 5, 2, 3, 10, 15, 18, 14, 7, 9, 12, 17, 22, 25, 18, 10, 8, 6];

      // Define bins
      const binSize = 5;
      const minValue = Math.min(...rawData);
      const maxValue = Math.max(...rawData);
      //const bins = ["On my own", "With strangers", "With people I know", "with people I am close to ", "Connecting with people online or using social media"];

    const bins = []
        console.log("bins", bins)

        for (let i = minValue; i <= maxValue; i += binSize) {
                          //@ts-ignore
            bins.push({ range: `${i}-${i + binSize - 1}`, count: 0 });
        }

      // Populate bins
      rawData.forEach(value => {
          const index = Math.floor((value - minValue) / binSize);
              //@ts-ignore
          bins[index].count++;
      });

    console.log("raw data", rawData)


    //@ts-ignore
    const labels = ["On my own", "With strangers", "With people I know", "with people I am close to ", "Connecting with people online or using social media"];


        const data = [5,9,15,2,5]


    //@ts-ignore
        const ctx = document.getElementById('histogramChart').getContext('2d');

        this.charts["histogramChart"] =       new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Frequency',
                    data: data,
                    backgroundColor: 'rgba(75, 192, 192, 0.6)',
                    borderColor: 'rgba(75, 192, 192, 1)',
                    borderWidth: 1
                }]
            },
            options: {
                plugins: {

                    legend: {
                        display: false,
                        position: 'bottom',
                    },
                },
                responsive: false,
                maintainAspectRatio: true,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });

    }

  createLineChart(id: string, labels: string[], data: number[], showScaleY: boolean, showDataLables: boolean,  color: string = 'rgba(110, 37, 147, 0.9)') {
    //@ts-ignore
    const ctx = document.getElementById(id)?.getContext('2d');

    const gradient = ctx.createLinearGradient(0, 0, 0, 400);
    gradient.addColorStop(0, color);
      gradient.addColorStop(1, 'rgba(255, 255, 255, 0.0)');

      let graphObject  = {
        type: 'line',
        data: {
          labels: labels,
          datasets: [
            {
              fill: true, // Enables the fill under the line
              tension: 0.2,
              data: data,
              borderWidth: 2,
              borderColor: color, // Line color
              backgroundColor: gradient,
            },
          ],
        },
        options: {
              layout: {
                  padding: {
                      left: 30,
                      right: 30,
                      top: 30,
                      bottom: 30
                  }
              },
          responsive: false,
          maintainAspectRatio: true,
          scales: {
            y: {
              beginAtZero: true,
              display: showScaleY,
            },
          },
          plugins: {
            title: {
              display: false,
            },
            legend: {
              display: false,
              position: 'top',
              },
              datalabels : {
                display:showDataLables ,
                align: 'top',
                // Alignment relative to the bar (top, middle, bottom)
                formatter: (value) => value, // Format the value (e.g., add units or rounding)
                color: '#000', // Text color
                font: {
                  weight: 'bold',
                  size: 12,
                },
                }
            },
        },
      }


      if (showDataLables) {
        graphObject["options"]["datalabels"] = {

            align: 'top',
            // Alignment relative to the bar (top, middle, bottom)
            formatter: (value) => value, // Format the value (e.g., add units or rounding)
            color: '#000', // Text color
            font: {
              weight: 'bold',
              size: 12,
            },
            }
      } else {
        graphObject["options"]["datalabels"] = {
            display: false,
        }
      }

    let chart = new Chart(id, graphObject as ChartConfiguration );



      return chart;
  }

  createBarChart(id: string, labels: string[], data: number[], barColor: string = 'rgba(191,	236,	235	, 1)' ) {
    return new Chart(id, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            data: data,
            borderWidth: 1,
            borderRadius: 5,
            barThickness: 30, // Fixed bar width,
            backgroundColor: [barColor],
          },
        ],
      },
      options: {
        responsive: false,
        maintainAspectRatio: true,
        scales: {
          y: {
            beginAtZero: true,
            display: false,
          },
        },
        plugins: {
          title: {
            display: false,
          },
          legend: {
            display: false,
            position: 'top',
          },
          datalabels: {
            anchor: 'end', // Positioning of the label (start, center, end)
            // Alignment relative to the bar (top, middle, bottom)
            formatter: (value) => value, // Format the value (e.g., add units or rounding)
            color: '#000', // Text color
            font: {
              weight: 'bold',
              size: 8,
            },
            offset: -20,
            align: 'top',
          },
        },
      },
    });
  }

  ngOnInit() {
    let lables = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec',
    ];


    let weekLabels = [] as string[]
    for(let i=1; i<=52; i++) {
        weekLabels.push("Week " + i);
    }

    console.log("weekLabels", weekLabels)
    for (const [key, value] of Object.entries(this.data)) {
        console.log(`Key: ${key}, Value: ${value}`);

        let values = value as number[];
        let chartType = this.chart_type[key]
        if (chartType.type == "line") {
            let labeles = chartType.timeframe == "week" ? weekLabels : lables;
            this.charts[key] = this.createLineChart(key, labeles, values, chartType.showScaleY, chartType.showDataTables, chartType.color);
        } else {
            this.charts[key] = this.createBarChart(key, lables, values, chartType.color );
        }

        console.log("this charts", this.charts)
    }

      this.createMonthHistogram();
  }
}
