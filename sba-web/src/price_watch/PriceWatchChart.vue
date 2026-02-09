<template>
  <div class="price-watch-chart">
    <div class="chart-header">
      <div class="title-section">
        <h1>全球价格监控</h1>
        <div class="filter-controls">
          <div class="filter-item">
            <label>平台</label>
            <select v-model="platform" @change="fetchPriceData">
              <option value="AWS">AWS</option>
            </select>
          </div>
          <div class="filter-item">
            <label>通道</label>
            <select v-model="channel" @change="fetchPriceData">
              <option value="SMS">SMS</option>
            </select>
          </div>
          <div class="filter-item">
            <label>天数</label>
            <input type="number" v-model.number="intervalDays" @change="fetchPriceData" min="1" />
          </div>
        </div>
      </div>
      <div class="status-info">
        <button @click="refreshData" :disabled="loading" class="refresh-btn">
          {{ loading ? '刷新' : '手动刷新' }}
        </button>
      </div>
    </div>

    <div class="grafana-legend">
      <div
          v-for="country in allCountries"
          :key="country"
          class="legend-item"
          :class="{
          'is-dimmed': selectedCountry && selectedCountry !== country,
          'is-active': selectedCountry === country
        }"
          @click="handleLegendClick(country)"
      >
        <span class="color-pill" :style="{ backgroundColor: getColor(country) }"></span>
        <span class="country-name">{{ country }}</span>
      </div>
    </div>

    <div ref="chartContainer" class="chart-canvas"></div>

    <div class="footer-info">
      <span v-if="selectedCountry" class="focus-hint">聚焦模式开启</span>
      <span>数据更新于: {{ lastUpdateTime }}</span>
    </div>
  </div>
</template>

<script>
import * as d3 from 'd3';

export default {
  name: 'PriceWatchChart',
  data() {
    return {
      intervalDays: 10,
      platform: 'AWS',
      channel: 'SMS',
      priceData: [],
      loading: false,
      selectedCountry: null,
      allCountries: [],
      lastUpdateTime: '',
      colorScale: d3.scaleOrdinal(d3.schemeCategory10)
    };
  },
  mounted() {
    this.fetchPriceData();
    window.addEventListener('resize', this.renderWithCurrentData);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.renderWithCurrentData);
  },
  methods: {
    handleLegendClick(country) {
      this.selectedCountry = (this.selectedCountry === country) ? null : country;
      this.renderWithCurrentData();
    },
    getColor(country) { return this.colorScale(country); },

    // 稳健的日期解析函数
    parseDateStr(str) {
      if (!str) return new Date();
      // 处理 YYYY-MM-DD
      const parts = str.split(/[-/]/);
      if (parts.length >= 3) {
        const d = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
        return isNaN(d.getTime()) ? new Date() : d;
      }
      const d = new Date(str);
      return isNaN(d.getTime()) ? new Date() : d;
    },

    async fetchPriceData() {
      this.loading = true;
      try {
        const params = new URLSearchParams({
          intervalDays: this.intervalDays,
          platform: this.platform,
          channel: this.channel
        });
        const response = await fetch(`/api/api/price-watch?${params.toString()}`);
        const rawData = await response.json();

        this.priceData = rawData.map(d => ({
          ...d,
          _dateObj: this.parseDateStr(d.date),
          _priceVal: parseFloat(d.price) || 0
        }));

        this.allCountries = [...new Set(this.priceData.map(d => d.countryCode))].sort();
        this.lastUpdateTime = new Date().toLocaleTimeString();
        this.renderWithCurrentData();
      } catch (e) {
        console.error(e);
      } finally {
        this.loading = false;
      }
    },

    renderWithCurrentData() {
      const container = this.$refs.chartContainer;
      if (!container || !this.priceData.length) return;

      const active = this.selectedCountry
          ? this.priceData.filter(d => d.countryCode === this.selectedCountry)
          : this.priceData;

      const grouped = d3.group(active, d => d.countryCode);
      const data = Array.from(grouped, ([country, records]) => ({
        country,
        values: records.map(r => ({ date: r._dateObj, price: r._priceVal })).sort((a,b) => a.date - b.date)
      }));

      this.drawChart(data);
    },

    drawChart(data) {
      const container = this.$refs.chartContainer;
      const margin = { top: 20, right: 40, bottom: 40, left: 60 };
      const width = container.offsetWidth - margin.left - margin.right;
      const height = 450 - margin.top - margin.bottom;

      d3.select(container).selectAll("*").remove();
      const svg = d3.select(container).append("svg")
          .attr("width", width + margin.left + margin.right)
          .attr("height", height + margin.top + margin.bottom)
          .append("g").attr("transform", `translate(${margin.left},${margin.top})`);

      const allPoints = data.flatMap(d => d.values);
      const xExtent = d3.extent(allPoints, v => v.date);
      const isSingleDay = xExtent[0].getTime() === xExtent[1].getTime();

      // 如果只有一天，X轴前后各延展半天
      const xDomain = isSingleDay
          ? [new Date(xExtent[0].getTime() - 43200000), new Date(xExtent[1].getTime() + 43200000)]
          : xExtent;

      const yMin = d3.min(allPoints, v => v.price);
      const yMax = d3.max(allPoints, v => v.price);
      const yDomain = yMin === yMax ? [yMin * 0.9, yMin * 1.1 || 1] : [yMin * 0.98, yMax * 1.02];

      const x = d3.scaleTime().domain(xDomain).range([0, width]);
      const y = d3.scaleLinear().domain(yDomain).range([height, 0]);

      // X轴处理：如果是单日，强制在中间画一个Tick
      const xAxis = d3.axisBottom(x).tickFormat(d3.timeFormat("%m-%d")).tickSize(-height);
      if (isSingleDay) xAxis.tickValues([xExtent[0]]);

      svg.append("g").attr("transform", `translate(0,${height})`).attr("class", "axis-grid").call(xAxis);
      svg.append("g").attr("class", "axis-grid").call(d3.axisLeft(y).ticks(6).tickSize(-width));

      const line = d3.line().x(d => x(d.date)).y(d => y(d.price)).curve(d3.curveMonotoneX);

      const groups = svg.selectAll(".g").data(data).enter().append("g");
      groups.append("path").attr("d", d => line(d.values)).attr("stroke", d => this.getColor(d.country)).attr("stroke-width", 2).attr("fill", "none");

      groups.selectAll("circle").data(d => d.values.map(v => ({ ...v, country: d.country }))).enter().append("circle")
          .attr("cx", d => x(d.date)).attr("cy", d => y(d.price)).attr("r", 5)
          .attr("fill", d => this.getColor(d.country)).attr("stroke", "#fff").attr("stroke-width", 2)
          .on("mouseover", (event, d) => this.showTooltip(event, d))
          .on("mouseout", () => d3.selectAll(".custom-tooltip").remove());
    },

    showTooltip(event, d) {
      d3.selectAll(".custom-tooltip").remove();
      if (!d || !d.date || isNaN(d.date.getTime())) return;

      const format = (v) => String(v).padStart(2, '0');
      const dateStr = `${d.date.getFullYear()}-${format(d.date.getMonth() + 1)}-${format(d.date.getDate())}`;

      d3.select("body").append("div").attr("class", "custom-tooltip")
          .style("left", (event.pageX + 15) + "px").style("top", (event.pageY - 30) + "px")
          .html(`
          <div style="color:${this.getColor(d.country)};font-weight:bold;">${d.country}</div>
          <div style="color:#333;margin:4px 0;">价格: <strong>¥${d.price.toFixed(4)}</strong></div>
          <div style="font-size:11px;color:#666;">日期: ${dateStr}</div>
        `);
    },
    refreshData() { this.selectedCountry = null; this.fetchPriceData(); }
  }
};
</script>

<style scoped>
.price-watch-chart { background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); font-family: sans-serif; }
.chart-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 15px; }
.filter-controls { display: flex; gap: 15px; }
.filter-item label { display: block; font-size: 11px; color: #007bff; font-weight: bold; margin-bottom: 4px; }
.filter-item select, .filter-item input { background: #f8f9fa; border: 1px solid #ddd; padding: 6px; border-radius: 4px; }
.grafana-legend { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 15px; background: #f8f9fa; padding: 10px; border-radius: 4px; }
.legend-item { display: flex; align-items: center; padding: 4px 8px; cursor: pointer; font-size: 12px; border: 1px solid #eee; background: #fff; border-radius: 4px; }
.legend-item.is-active { border-color: #007bff; color: #007bff; }
.legend-item.is-dimmed { opacity: 0.2; filter: grayscale(1); }
.color-pill { width: 12px; height: 4px; margin-right: 6px; }
.refresh-btn { background: #007bff; color: #fff; border: none; padding: 8px 15px; border-radius: 4px; cursor: pointer; }
.chart-canvas { width: 100%; height: 450px; }
.footer-info { display: flex; justify-content: space-between; font-size: 11px; color: #999; margin-top: 10px; }
:deep(.axis-grid) line { stroke: #eee; }
:deep(.axis-grid) text { fill: #666; font-size: 11px; }
:deep(.axis-grid) .domain { display: none; }
</style>

<style>
.custom-tooltip {
  position: absolute; background: #fff; border: 1px solid #ddd; padding: 10px;
  font-size: 12px; pointer-events: none; z-index: 10000; border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
</style>