<template>
  <div class="kline-extension">
    <div class="kline-header">
      <div class="stock-info">
        <h1 :class="statusClass">雪人集团 (002639)</h1>
        <span class="market-status">实时行情监控</span>
      </div>
      <div class="price-section" :class="statusClass">
        <span class="price-now">{{ currentPrice }}</span>
        <span class="price-change">{{ changePercent }}%</span>
      </div>
    </div>

    <div ref="chartContainer" id="kline-canvas"></div>

    <div class="footer-hint">数据来源：腾讯财经 (每5秒自动刷新)</div>
  </div>
</template>

<script>
import * as d3 from 'd3'; // 如果是构建环境，需要引入 d3

export default {
  name: 'CustomKline',
  data() {
    return {
      currentPrice: '--',
      changePercent: '0.00',
      statusClass: '',
      stockData: [],
      timer: null,
      svg: null,
      chartWidth: 0,
      chartHeight: 0
    };
  },
  mounted() {
    // 初始化画布尺寸
    this.initChart();
    // 立即执行一次获取数据
    this.fetchData();
    // 开启定时任务
    this.timer = setInterval(this.fetchData, 5000);

    // 响应式监听：窗口缩放时重新调整图表
    window.addEventListener('resize', this.handleResize);
  },
  beforeUnmount() {
    // 销毁组件前清理定时器和监听器，防止内存泄漏
    if (this.timer) clearInterval(this.timer);
    window.removeEventListener('resize', this.handleResize);
  },
  methods: {
    initChart() {
      const container = this.$refs.chartContainer;
      const margin = { top: 20, right: 50, bottom: 40, left: 50 };
      const width = container.offsetWidth || 800;
      const height = 400;

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
    async fetchData() {
      try {
        const response = await fetch(`https://qt.gtimg.cn/q=sz002639`);
        const text = await response.text();
        const p = text.split('~');

        const newData = {
          time: new Date().toLocaleTimeString('zh-CN', { hour12: false }).slice(0, 5),
          open: +p[5],
          close: +p[3],
          high: +p[33],
          low: +p[34]
        };

        this.currentPrice = newData.close.toFixed(2);
        this.changePercent = p[32];
        this.statusClass = newData.close >= newData.open ? 'up' : 'down';

        // 数据队列逻辑
        const lastIndex = this.stockData.length - 1;
        if (this.stockData.length > 0 && this.stockData[lastIndex].time === newData.time) {
          this.stockData[lastIndex] = newData;
        } else {
          this.stockData.push(newData);
          if (this.stockData.length > 40) this.stockData.shift();
        }
        this.render();
      } catch (e) {
        console.error("行情数据抓取失败:", e);
      }
    },
    render() {
      if (!this.svg) return;

      const x = d3.scaleBand()
          .domain(this.stockData.map(d => d.time))
          .range([0, this.chartWidth])
          .padding(0.4);

      const y = d3.scaleLinear()
          .domain([
            d3.min(this.stockData, d => d.low) * 0.995,
            d3.max(this.stockData, d => d.high) * 1.005
          ])
          .range([this.chartHeight, 0]);

      // 坐标轴更新
      this.svg.selectAll(".axis").remove();
      this.svg.append("g")
          .attr("class", "axis x-axis")
          .attr("transform", `translate(0,${this.chartHeight})`)
          .call(d3.axisBottom(x).tickSizeOuter(0));

      this.svg.append("g")
          .attr("class", "axis y-axis")
          .call(d3.axisLeft(y).ticks(5).tickFormat(d => d.toFixed(2)));

      // K线绘制
      const candles = this.svg.selectAll(".candle").data(this.stockData, d => d.time);

      candles.join("rect")
          .attr("class", "candle")
          .transition().duration(500)
          .attr("x", d => x(d.time))
          .attr("y", d => y(Math.max(d.open, d.close)))
          .attr("width", x.bandwidth())
          .attr("height", d => Math.max(Math.abs(y(d.open) - y(d.close)), 2))
          .attr("fill", d => d.close >= d.open ? "#ff5252" : "#26a69a")
          .attr("rx", 1);
    },
    handleResize() {
      this.initChart();
      this.render();
    }
  }
};
</script>

<style scoped>
.kline-extension {
  background: #161922;
  color: #e0e0e0;
  padding: 20px;
  border-radius: 8px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto;
}

.kline-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #2d323e;
}

.stock-info h1 {
  margin: 0;
  font-size: 1.5rem;
  letter-spacing: 1px;
}

.market-status {
  font-size: 0.8rem;
  color: #888;
}

.price-section {
  text-align: right;
}

.price-now {
  font-size: 2rem;
  font-weight: bold;
  display: block;
  line-height: 1;
}

.price-change {
  font-size: 1rem;
}

.up { color: #ff5252; }
.down { color: #26a69a; }

#kline-canvas {
  width: 100%;
  height: 400px;
  margin-top: 10px;
}

.footer-hint {
  font-size: 0.7rem;
  color: #555;
  margin-top: 10px;
  text-align: right;
}

/* D3 样式覆盖 */
:deep(.axis) text {
  fill: #777;
}
:deep(.axis) path, :deep(.axis) line {
  stroke: #333;
}
</style>