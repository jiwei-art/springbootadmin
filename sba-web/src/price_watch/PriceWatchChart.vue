<template>
  <div class="price-watch-chart">
    <div class="chart-header">
      <div class="title-section">
        <h1>全球价格监控</h1>
        <span class="subtitle">各国价格变动趋势 (近{{ intervalDays }}天)</span>
      </div>
      <div class="controls">
        <button @click="refreshData" :disabled="loading" class="refresh-btn">
          {{ loading ? '加载中...' : '刷新数据' }}
        </button>
      </div>
    </div>

    <div ref="chartContainer" id="price-chart-container"></div>

    <div class="footer-info">数据更新时间: {{ lastUpdateTime }}</div>
  </div>
</template>

<script>
import * as d3 from 'd3';

export default {
  name: 'PriceWatchChart',
  data() {
    return {
      intervalDays: 10,
      priceData: [],
      loading: false,
      lastUpdateTime: '',
      svg: null,
      chartWidth: 0,
      chartHeight: 0
    };
  },
  mounted() {
    this.initChart();
    this.fetchPriceData();
  },
  methods: {
    initChart() {
      const container = this.$refs.chartContainer;
      const margin = { top: 40, right: 100, bottom: 60, left: 80 };
      const width = container.offsetWidth || 1000;
      const height = 500;

      this.chartWidth = width - margin.left - margin.right;
      this.chartHeight = height - margin.top - margin.bottom;

      // 清空容器并创建 SVG
      d3.select(container).selectAll("*").remove();
      this.svg = d3.select(container)
          .append("svg")
          .attr("width", width)
          .attr("height", height)
          .append("g")
          .attr("transform", `translate(${margin.left},${margin.top})`);
    },
    async fetchPriceData() {
      this.loading = true;
      try {
        // 调用后端接口获取价格数据
        const response = await fetch(`http://localhost:8005/api/price-watch?intervalDays=${this.intervalDays}`);
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        this.priceData = await response.json();
        this.lastUpdateTime = new Date().toLocaleString('zh-CN');
        this.processAndRenderData();
      } catch (error) {
        console.error('获取价格数据失败:', error);
        // 模拟数据用于演示
        this.generateMockData();
      } finally {
        this.loading = false;
      }
    },
    generateMockData() {
      // 生成模拟数据用于演示
      const countries = ['US', 'CN', 'JP', 'DE', 'UK', 'FR', 'KR', 'IN'];
      const basePrices = [299.99, 2199.00, 32980, 279.99, 249.99, 269.99, 359900, 19999];
      
      this.priceData = [];
      const now = new Date();
      
      countries.forEach((country, countryIndex) => {
        for (let i = 9; i >= 0; i--) {
          const date = new Date(now);
          date.setDate(date.getDate() - i);
          
          // 模拟价格波动
          const fluctuation = (Math.random() - 0.5) * 0.1; // ±5% 波动
          const price = basePrices[countryIndex] * (1 + fluctuation);
          
          this.priceData.push({
            id: `${country}-${i}`,
            countryCode: country,
            price: price.toFixed(2),
            createTime: date
          });
        }
      });
      
      this.lastUpdateTime = new Date().toLocaleString('zh-CN');
      this.processAndRenderData();
    },
    processAndRenderData() {
      if (!this.svg || this.priceData.length === 0) return;

      // 按国家分组数据
      const groupedData = d3.group(this.priceData, d => d.countryCode);
      
      // 准备绘图数据
      const chartData = Array.from(groupedData, ([country, records]) => ({
        country: country,
        values: records.map(record => ({
          date: new Date(record.createTime),
          price: parseFloat(record.price)
        })).sort((a, b) => a.date - b.date)
      }));

      this.renderChart(chartData);
    },
    renderChart(data) {
      // 清除之前的图表元素
      this.svg.selectAll("*").remove();

      // 创建比例尺
      const x = d3.scaleTime()
          .domain([
            d3.min(data, d => d3.min(d.values, v => v.date)),
            d3.max(data, d => d3.max(d.values, v => v.date))
          ])
          .range([0, this.chartWidth]);

      const y = d3.scaleLinear()
          .domain([
            d3.min(data, d => d3.min(d.values, v => v.price)) * 0.95,
            d3.max(data, d => d3.max(d.values, v => v.price)) * 1.05
          ])
          .range([this.chartHeight, 0]);

      const color = d3.scaleOrdinal(d3.schemeCategory10);

      // 添加坐标轴
      this.svg.append("g")
          .attr("class", "x-axis")
          .attr("transform", `translate(0,${this.chartHeight})`)
          .call(d3.axisBottom(x).tickFormat(d3.timeFormat("%m-%d")));

      this.svg.append("g")
          .attr("class", "y-axis")
          .call(d3.axisLeft(y).tickFormat(d => `¥${d.toLocaleString()}`));

      // 添加标题
      this.svg.append("text")
          .attr("x", this.chartWidth / 2)
          .attr("y", -10)
          .attr("text-anchor", "middle")
          .style("font-size", "16px")
          .style("font-weight", "bold")
          .text("各国价格变动趋势");

      // 绘制线条
      const line = d3.line()
          .x(d => x(d.date))
          .y(d => y(d.price))
          .curve(d3.curveMonotoneX);

      const countries = this.svg.selectAll(".country-line")
          .data(data)
          .enter()
          .append("g")
          .attr("class", "country-line");

      // 添加路径线
      countries.append("path")
          .attr("class", "line")
          .attr("d", d => line(d.values))
          .attr("stroke", (d, i) => color(d.country))
          .attr("stroke-width", 2)
          .attr("fill", "none");

      // 添加圆点
      countries.selectAll(".dot")
          .data(d => d.values.map(v => ({ ...v, country: d.country })))
          .enter()
          .append("circle")
          .attr("class", "dot")
          .attr("cx", d => x(d.date))
          .attr("cy", d => y(d.price))
          .attr("r", 4)
          .attr("fill", d => color(d.country))
          .on("mouseover", (event, d) => {
            d3.select(event.target).attr("r", 6);
            this.showTooltip(event, d);
          })
          .on("mouseout", (event) => {
            d3.select(event.target).attr("r", 4);
            this.hideTooltip();
          });

      // 添加国家标签
      countries.append("text")
          .datum(d => ({ country: d.country, value: d.values[d.values.length - 1] }))
          .attr("transform", d => `translate(${x(d.value.date)},${y(d.value.price)})`)
          .attr("x", 8)
          .attr("dy", "0.35em")
          .style("font-size", "12px")
          .style("font-weight", "bold")
          .text(d => d.country);

      // 添加图例
      const legend = this.svg.append("g")
          .attr("class", "legend")
          .attr("transform", `translate(${this.chartWidth + 20}, 20)`);

      data.forEach((d, i) => {
        const legendRow = legend.append("g")
            .attr("transform", `translate(0, ${i * 20})`);

        legendRow.append("rect")
            .attr("width", 12)
            .attr("height", 12)
            .attr("fill", color(d.country));

        legendRow.append("text")
            .attr("x", 20)
            .attr("y", 10)
            .text(d.country)
            .style("font-size", "12px");
      });
    },
    showTooltip(event, data) {
      const tooltip = d3.select("body")
          .append("div")
          .attr("class", "tooltip")
          .style("position", "absolute")
          .style("background", "rgba(0, 0, 0, 0.8)")
          .style("color", "white")
          .style("padding", "8px")
          .style("border-radius", "4px")
          .style("pointer-events", "none")
          .style("font-size", "12px");

      tooltip.html(`
        <div><strong>国家:</strong> ${data.country}</div>
        <div><strong>价格:</strong> ¥${parseFloat(data.price).toLocaleString()}</div>
        <div><strong>日期:</strong> ${data.date.toLocaleDateString('zh-CN')}</div>
      `);

      tooltip.style("left", (event.pageX + 10) + "px")
             .style("top", (event.pageY - 10) + "px");
    },
    hideTooltip() {
      d3.selectAll(".tooltip").remove();
    },
    refreshData() {
      this.fetchPriceData();
    }
  }
};
</script>

<style scoped>
.price-watch-chart {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.title-section h1 {
  margin: 0;
  color: #333;
  font-size: 1.8rem;
}

.subtitle {
  color: #666;
  font-size: 0.9rem;
}

.refresh-btn {
  background: #007bff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
}

.refresh-btn:hover:not(:disabled) {
  background: #0056b3;
}

.refresh-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

#price-chart-container {
  width: 100%;
  height: 500px;
  margin: 20px 0;
}

.footer-info {
  text-align: right;
  color: #888;
  font-size: 0.8rem;
  margin-top: 10px;
}

/* D3 图表样式 */
:deep(.axis) text {
  fill: #666;
  font-size: 12px;
}

:deep(.axis) path,
:deep(.axis) line {
  stroke: #ddd;
}

:deep(.line) {
  fill: none;
  stroke-width: 2px;
}

:deep(.dot) {
  stroke: white;
  stroke-width: 1px;
}
</style>