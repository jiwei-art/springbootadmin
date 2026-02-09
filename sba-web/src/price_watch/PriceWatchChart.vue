<template>
  <div class="price-monitor-app">
    <header class="app-header">
      <div class="brand">
        全球价格监控
        <span class="engine-tag">{{ renderMode.toUpperCase() }} 引擎</span>
      </div>
      <div class="filter-panel">
        <div class="input-item">
          <label>平台</label>
          <select v-model="filters.platform">
            <option value="AWS">AWS</option>
          </select>
        </div>
        <div class="input-item">
          <label>通道</label>
          <select v-model="filters.channel">
            <option value="SMS">SMS</option>
          </select>
        </div>
        <div class="input-item">
          <label>回顾天数</label>
          <input type="number" v-model.number="filters.intervalDays" @keyup.enter="handleFetch" />
        </div>
        <button class="btn-query" :disabled="loading" @click="handleFetch">
          {{ loading ? '查询中...' : '同步数据' }}
        </button>
      </div>
    </header>

    <div class="main-body">
      <aside class="legend-sidebar">
        <div class="legend-header">
          <input v-model="searchQuery" placeholder="搜索国家..." class="search-input" />
          <div class="batch-ops">
            <span @click="toggleAll(true)">全选</span>
            <span @click="toggleAll(false)">清空</span>
          </div>
        </div>

        <div class="vs-viewport" @scroll="onScroll" ref="vsViewport">
          <div class="vs-spacer" :style="{ height: totalListHeight + 'px' }">
            <div class="vs-content" :style="{ transform: `translateY(${offsetY}px)` }">
              <div
                  v-for="c in visibleCountries"
                  :key="c"
                  class="vs-row"
                  :class="{ 'active': selectedCountries.has(c) }"
                  @click="toggleCountry(c)"
              >
                <i class="color-dot" :style="{ background: getColor(c) }"></i>
                <span class="row-label">{{ c }}</span>
                <i class="check-mark" v-if="selectedCountries.has(c)">✓</i>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <section class="chart-section">
        <div class="chart-wrapper" ref="chartContainer"
             @mousemove="handleMouseMove" @mouseleave="handleMouseLeave">

          <svg ref="svgLayer" class="layer svg-layer">
            <g class="main-g">
              <line ref="crosshairX" class="crosshair" y1="0" :y2="innerH"></line>
              <line ref="crosshairY" class="crosshair" x1="0" :x2="innerW"></line>
              <circle ref="hoverDot" r="6" class="hover-point"></circle>
            </g>
          </svg>

          <canvas ref="canvasLayer" class="layer canvas-layer"></canvas>

          <div v-if="hoverInfo" class="fixed-tooltip" :style="tooltipPos">
            <div class="tip-head" :style="{ borderLeftColor: getColor(hoverInfo.countryCode) }">
              {{ hoverInfo.countryCode }}
            </div>
            <div class="tip-body">
              <div class="tip-row"><span>价格:</span> <strong>¥{{ hoverInfo._val.toFixed(4) }}</strong></div>
              <div class="tip-row"><span>日期:</span> {{ hoverInfo.date }}</div>
            </div>
          </div>

          <div v-if="loading" class="loading-mask">
            <div class="spinner"></div>
            <span>正在处理海量点位...</span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import * as d3 from 'd3';

export default {
  name: 'GlobalPriceChart',
  data() {
    return {
      filters: { platform: 'AWS', intervalDays: 30, channel: 'SMS' },
      priceData: [],
      allCountries: [],
      selectedCountries: new Set(),
      searchQuery: '',
      loading: false,

      // 虚拟滚动配置
      rowHeight: 36,
      scrollTop: 0,
      viewportHeight: 600,

      // 绘图引擎状态
      renderMode: 'canvas',
      margin: { top: 30, right: 40, bottom: 40, left: 60 },
      innerW: 0,
      innerH: 0,

      // 交互状态
      hoverInfo: null,
      tooltipPos: { left: 0, top: 0 },
      quadtree: null,
      scales: { x: null, y: null },
      colorScale: d3.scaleOrdinal(d3.schemeTableau10)
    };
  },

  computed: {
    filteredCountries() {
      return this.allCountries.filter(c => c.toLowerCase().includes(this.searchQuery.toLowerCase()));
    },
    totalListHeight() { return this.filteredCountries.length * this.rowHeight; },
    visibleCountries() {
      const start = Math.floor(this.scrollTop / this.rowHeight);
      const end = start + Math.ceil(this.viewportHeight / this.rowHeight) + 5;
      return this.filteredCountries.slice(start, end);
    },
    offsetY() { return Math.floor(this.scrollTop / this.rowHeight) * this.rowHeight; }
  },

  mounted() {
    this.initContexts();
    this.resizeObserver = new ResizeObserver(() => this.draw());
    this.resizeObserver.observe(this.$refs.chartContainer);
    this.handleFetch(); // 初始查询
  },

  beforeUnmount() {
    if (this.resizeObserver) this.resizeObserver.disconnect();
  },

  methods: {
    getColor(name) { return this.colorScale(name); },
    onScroll(e) { this.scrollTop = e.target.scrollTop; },

    initContexts() {
      this.ctx = this.$refs.canvasLayer.getContext('2d');
      this.svg = d3.select(this.$refs.svgLayer);
    },

    toggleCountry(name) {
      const newSet = new Set(this.selectedCountries);
      if (newSet.has(name)) newSet.delete(name);
      else newSet.add(name);
      this.selectedCountries = newSet;
      this.draw();
    },

    toggleAll(select) {
      this.selectedCountries = select ? new Set(this.allCountries) : new Set();
      this.draw();
    },

    async handleFetch() {
      this.loading = true;
      try {
        const query = new URLSearchParams(this.filters).toString();
        const res = await fetch(`/api/api/price-watch?${query}`);
        const data = await res.json();

        this.priceData = data.map(d => ({
          ...d,
          _date: new Date(d.date),
          _val: parseFloat(d.price) || 0
        })).sort((a,b) => a._date - b._date);

        this.allCountries = [...new Set(this.priceData.map(d => d.countryCode))].sort();
        if (this.selectedCountries.size === 0) {
          this.selectedCountries = new Set(this.allCountries.slice(0, 15));
        }

        // 自动切换引擎：点数 > 3000 强制 Canvas
        this.renderMode = this.priceData.length > 3000 ? 'canvas' : 'svg';
        this.draw();
      } catch (e) {
        console.error("Fetch failed", e);
      } finally {
        this.loading = false;
      }
    },

    draw() {
      const container = this.$refs.chartContainer;
      if (!container || !this.priceData.length) return;

      const { offsetWidth: w, offsetHeight: h } = container;
      this.innerW = w - this.margin.left - this.margin.right;
      this.innerH = h - this.margin.top - this.margin.bottom;

      // 1. 比例尺更新
      this.scales.x = d3.scaleTime().domain(d3.extent(this.priceData, d => d._date)).range([0, this.innerW]);
      const activeData = this.priceData.filter(d => this.selectedCountries.has(d.countryCode));
      const maxVal = d3.max(activeData, d => d._val) || 1;
      this.scales.y = d3.scaleLinear().domain([0, maxVal * 1.1]).range([this.innerH, 0]).nice();

      // 2. 坐标轴更新 (SVG)
      this.renderAxes(w, h);

      // 3. 渲染核心路径与点位 (Canvas)
      const groups = d3.groups(activeData, d => d.countryCode);
      this.renderCanvas(w, h, groups);

      // 4. 构建空间索引 (Quadtree)
      this.quadtree = d3.quadtree()
          .x(d => this.scales.x(d._date))
          .y(d => this.scales.y(d._val))
          .addAll(activeData);
    },

    renderAxes(w, h) {
      this.svg.attr('width', w).attr('height', h);
      const g = this.svg.selectAll('.main-g').attr('transform', `translate(${this.margin.left},${this.margin.top})`);

      g.selectAll('.x-axis').data([null]).join('g').attr('class', 'x-axis')
          .attr('transform', `translate(0,${this.innerH})`).call(d3.axisBottom(this.scales.x).ticks(w/100));

      g.selectAll('.y-axis').data([null]).join('g').attr('class', 'y-axis')
          .call(d3.axisLeft(this.scales.y).ticks(6).tickFormat(d => `¥${d.toFixed(2)}`));
    },

    renderCanvas(w, h, groups) {
      const dpr = window.devicePixelRatio || 1;
      this.$refs.canvasLayer.width = w * dpr;
      this.$refs.canvasLayer.height = h * dpr;
      this.ctx.scale(dpr, dpr);
      this.ctx.clearRect(0, 0, w, h);

      this.ctx.save();
      this.ctx.translate(this.margin.left, this.margin.top);

      // 1. 创建适配 Canvas 上下文的曲线生成器
      const lineGen = d3.line()
          .x(d => this.scales.x(d._date))
          .y(d => this.scales.y(d._val))
          .curve(d3.curveMonotoneX) // 关键：使用单调 X 轴曲线插值
          .context(this.ctx);       // 关键：将 D3 逻辑注入 Canvas 绘图环境

      groups.forEach(([country, values]) => {
        const color = this.getColor(country);

        // 2. 绘制平滑曲线
        this.ctx.beginPath();
        lineGen(values);          // D3 会自动调用 ctx.moveTo, ctx.lineTo, ctx.bezierCurveTo
        this.ctx.strokeStyle = color;
        this.ctx.lineWidth = 2;
        this.ctx.lineJoin = 'round';
        this.ctx.lineCap = 'round';
        this.ctx.stroke();

        // 3. 绘制坐标点
        // 注意：如果点太多导致视觉拥挤，可以增加过滤逻辑，例如 values.filter((d, i) => i % 2 === 0)
        values.forEach(d => {
          this.ctx.beginPath();
          this.ctx.fillStyle = "#fff";
          this.ctx.strokeStyle = color;
          this.ctx.lineWidth = 1;
          this.ctx.arc(this.scales.x(d._date), this.scales.y(d._val), 2.5, 0, Math.PI * 2);
          this.ctx.fill();
          this.ctx.stroke();
        });
      });

      this.ctx.restore();
    },

    handleMouseMove(e) {
      if (!this.quadtree) return;
      const rect = this.$refs.chartContainer.getBoundingClientRect();
      const mx = e.clientX - rect.left - this.margin.left;
      const my = e.clientY - rect.top - this.margin.top;

      const closest = this.quadtree.find(mx, my, 40); // 40px 感应半径

      if (closest) {
        this.hoverInfo = closest;
        const x = this.scales.x(closest._date);
        const y = this.scales.y(closest._val);

        // 更新交互辅助线
        d3.select(this.$refs.hoverDot).style('display', 'block')
            .attr('cx', x).attr('cy', y).attr('stroke', this.getColor(closest.countryCode));
        d3.select(this.$refs.crosshairX).style('display', 'block').attr('x1', x).attr('x2', x);
        d3.select(this.$refs.crosshairY).style('display', 'block').attr('y1', y).attr('y2', y);

        this.tooltipPos = { left: (x + this.margin.left + 15) + 'px', top: (y + this.margin.top - 40) + 'px' };
      } else {
        this.handleMouseLeave();
      }
    },

    handleMouseLeave() {
      this.hoverInfo = null;
      d3.select(this.$refs.hoverDot).style('display', 'none');
      d3.select(this.$refs.crosshairX).style('display', 'none');
      d3.select(this.$refs.crosshairY).style('display', 'none');
    }
  }
};
</script>

<style scoped>
.price-monitor-app { display: flex; flex-direction: column; height: 100vh; background: #f8fafc; font-family: Inter, system-ui, sans-serif; }

/* Header */
.app-header { height: 64px; background: #fff; padding: 0 24px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #e2e8f0; }
.brand { font-weight: 700; font-size: 18px; color: #0f172a; }
.engine-tag { font-size: 10px; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; margin-left: 8px; color: #64748b; }
.filter-panel { display: flex; gap: 16px; align-items: center; }
.input-item label { font-size: 12px; color: #64748b; margin-right: 8px; }
.input-item input, .input-item select { padding: 6px 10px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 14px; }
.btn-query { background: #2563eb; color: #fff; border: none; padding: 7px 16px; border-radius: 6px; cursor: pointer; font-weight: 600; }

.main-body { flex: 1; display: flex; overflow: hidden; padding: 16px; gap: 16px; }

/* Virtual Scroll Sidebar */
.legend-sidebar { width: 280px; background: #fff; border-radius: 12px; display: flex; flex-direction: column; border: 1px solid #e2e8f0; }
.legend-header { padding: 16px; border-bottom: 1px solid #f1f5f9; }
.search-input { width: 100%; padding: 8px 12px; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 13px; margin-bottom: 12px; }
.batch-ops { display: flex; gap: 12px; font-size: 12px; color: #2563eb; cursor: pointer; }

.vs-viewport { flex: 1; overflow-y: auto; position: relative; }
.vs-row { height: 36px; display: flex; align-items: center; padding: 0 16px; cursor: pointer; transition: 0.1s; border-left: 3px solid transparent; }
.vs-row:hover { background: #f8fafc; }
.vs-row.active { background: #eff6ff; border-left-color: #2563eb; }
.color-dot { width: 8px; height: 8px; border-radius: 50%; margin-right: 12px; }
.row-label { font-size: 13px; color: #334155; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.check-mark { color: #2563eb; font-size: 12px; }

/* Chart Section */
.chart-section { flex: 1; background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; position: relative; }
.chart-wrapper { position: absolute; inset: 16px; cursor: crosshair; }
.layer { position: absolute; inset: 0; }
.canvas-layer { pointer-events: none; }

/* Tooltip & Interactivity */
.fixed-tooltip { position: absolute; background: rgba(255,255,255,0.95); border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); pointer-events: none; z-index: 50; min-width: 140px; }
.tip-head { font-weight: 700; font-size: 13px; margin-bottom: 8px; border-left: 4px solid #ddd; padding-left: 8px; }
.tip-row { font-size: 12px; display: flex; justify-content: space-between; margin-bottom: 4px; }
.tip-row span { color: #64748b; }

.hover-point { fill: #fff; stroke-width: 3px; display: none; pointer-events: none; }
.crosshair { stroke: #e2e8f0; stroke-width: 1px; stroke-dasharray: 4,4; display: none; }

.loading-mask { position: absolute; inset: 0; background: rgba(255,255,255,0.7); display: flex; flex-direction: column; align-items: center; justify-content: center; z-index: 100; font-size: 14px; color: #64748b; gap: 12px; }
.spinner { width: 24px; height: 24px; border: 3px solid #e2e8f0; border-top-color: #2563eb; border-radius: 50%; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

:deep(.axis) text { font-size: 11px; fill: #94a3b8; }
:deep(.axis) line, :deep(.axis) path { stroke: #f1f5f9; }
</style>