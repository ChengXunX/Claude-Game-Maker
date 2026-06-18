<template>
  <div class="knowledge-graph-page">
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #409eff"><el-icon :size="24"><Share /></el-icon></div>
          <div class="stat-info"><div class="stat-value">{{ stats.nodeCount || 0 }}</div><div class="stat-label">节点总数</div></div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #67c23a"><el-icon :size="24"><Connection /></el-icon></div>
          <div class="stat-info"><div class="stat-value">{{ stats.edgeCount || 0 }}</div><div class="stat-label">关系总数</div></div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #e6a23c"><el-icon :size="24"><Folder /></el-icon></div>
          <div class="stat-info"><div class="stat-value">{{ projectList.length }}</div><div class="stat-label">项目数</div></div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #909399"><el-icon :size="24"><List /></el-icon></div>
          <div class="stat-info"><div class="stat-value">{{ Object.keys(stats.typeCounts || {}).length }}</div><div class="stat-label">节点类型</div></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="section-card">
      <template #header>
        <div class="section-header">
          <span>知识图谱</span>
          <div class="header-actions">
            <el-select v-model="selectedProject" placeholder="选择项目" style="width: 200px" @change="loadGraph">
              <el-option v-for="p in projectList" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
            <el-input v-model="searchKeyword" placeholder="搜索节点..." clearable style="width: 200px" @keyup.enter="handleSearch" @clear="searchResults = []">
              <template #append><el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button></template>
            </el-input>
            <el-button type="primary" @click="handleBuild" :loading="building"><el-icon><Refresh /></el-icon> 构建图谱</el-button>
          </div>
        </div>
      </template>

      <div v-if="graphData.nodes.length > 0" class="graph-wrapper">
        <!-- 图例 -->
        <div class="graph-legend">
          <span v-for="(color, type) in nodeColors" :key="type" class="legend-item">
            <span class="legend-dot" :style="{ background: color }"></span>{{ typeLabels[type] || type }}
          </span>
        </div>
        <!-- SVG 图谱 -->
        <div class="graph-area" ref="graphAreaRef">
          <svg ref="svgRef" :width="svgWidth" :height="svgHeight" class="graph-svg">
            <!-- 边 -->
            <g class="edges">
              <line v-for="edge in renderedEdges" :key="edge.id"
                :x1="edge.x1" :y1="edge.y1" :x2="edge.x2" :y2="edge.y2"
                stroke="#c0c4cc" stroke-width="1.5" stroke-dasharray="4,2" />
            </g>
            <!-- 节点 -->
            <g class="nodes">
              <g v-for="node in renderedNodes" :key="node.id"
                :transform="`translate(${node.x}, ${node.y})`"
                @click="selectNode(node)" class="node-group" :class="{ selected: selectedNode?.id === node.id }">
                <circle :r="node.radius" :fill="nodeColors[node.nodeType] || '#909399'" stroke="#fff" stroke-width="2" />
                <text :y="node.radius + 16" text-anchor="middle" class="node-label">{{ truncate(node.displayName, 10) }}</text>
                <text :y="-node.radius - 6" text-anchor="middle" class="node-type">{{ typeLabels[node.nodeType] || node.nodeType }}</text>
              </g>
            </g>
          </svg>
        </div>

        <!-- 节点详情面板 -->
        <transition name="slide">
          <div v-if="selectedNode" class="node-detail-panel">
            <div class="panel-header">
              <span>{{ selectedNode.displayName }}</span>
              <el-button text @click="selectedNode = null"><el-icon><Close /></el-icon></el-button>
            </div>
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="类型">
                <el-tag :color="nodeColors[selectedNode.nodeType]" effect="dark" size="small">{{ typeLabels[selectedNode.nodeType] || selectedNode.nodeType }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="引用ID">{{ selectedNode.nodeRefId }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ selectedNode.createdAt }}</el-descriptions-item>
            </el-descriptions>
            <div v-if="nodeNeighbors.length > 0" style="margin-top: 12px">
              <div class="panel-subtitle">关联节点</div>
              <div v-for="n in nodeNeighbors" :key="n.id" class="neighbor-item" @click="selectNode(n)">
                <span class="legend-dot" :style="{ background: nodeColors[n.nodeType] }"></span>
                <span>{{ n.displayName }}</span>
              </div>
            </div>
          </div>
        </transition>
      </div>

      <el-empty v-else-if="!loading" description="暂无知识图谱数据">
        <el-button type="primary" @click="handleBuild" :loading="building">构建知识图谱</el-button>
      </el-empty>

      <!-- 节点列表 -->
      <div v-if="displayNodes.length > 0" style="margin-top: 20px">
        <div class="table-header">
          <span>节点列表</span>
          <el-radio-group v-model="filterType" size="small">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button v-for="(_, type) in nodeColors" :key="type" :label="type">{{ typeLabels[type] || type }}</el-radio-button>
          </el-radio-group>
        </div>
        <el-table :data="displayNodes" stripe max-height="350" size="small">
          <el-table-column prop="nodeType" label="类型" width="100">
            <template #default="{ row }"><el-tag :color="nodeColors[row.nodeType]" effect="dark" size="small">{{ typeLabels[row.nodeType] || row.nodeType }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="displayName" label="名称" min-width="180" />
          <el-table-column prop="nodeRefId" label="引用ID" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="创建时间" width="170" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { knowledgeGraphApi, projectApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const building = ref(false)
const selectedProject = ref('')
const searchKeyword = ref('')
const searchResults = ref([])
const projectList = ref([])
const stats = ref({})
const graphData = ref({ nodes: [], edges: [] })
const selectedNode = ref(null)
const nodeNeighbors = ref([])
const filterType = ref('')
const svgRef = ref(null)
const graphAreaRef = ref(null)
const svgWidth = ref(800)
const svgHeight = ref(500)

const nodeColors = { PROJECT: '#409eff', AGENT: '#67c23a', SKILL: '#e6a23c', MILESTONE: '#f56c6c', VERIFICATION: '#b37feb', DOCUMENT: '#909399', TASK: '#36cfc9', KNOWLEDGE: '#ff85c0' }
const typeLabels = { PROJECT: '项目', AGENT: 'Agent', SKILL: '技能', MILESTONE: '里程碑', VERIFICATION: '验证', DOCUMENT: '文档', TASK: '任务', KNOWLEDGE: '知识' }

const displayNodes = computed(() => {
  let nodes = graphData.value.nodes
  if (filterType.value) nodes = nodes.filter(n => n.nodeType === filterType.value)
  if (searchResults.value.length > 0) nodes = searchResults.value
  return nodes
})

// 布局算法：力导向简化版
const renderedNodes = ref([])
const renderedEdges = ref([])

function layoutGraph() {
  const nodes = graphData.value.nodes.map((n, i) => ({
    ...n,
    radius: n.nodeType === 'PROJECT' ? 30 : n.nodeType === 'AGENT' ? 22 : 16,
    x: svgWidth.value / 2 + (Math.random() - 0.5) * 300,
    y: svgHeight.value / 2 + (Math.random() - 0.5) * 200,
    vx: 0, vy: 0
  }))
  const edges = graphData.value.edges
  const nodeMap = {}
  nodes.forEach(n => { nodeMap[n.id] = n })

  // 简单力导向迭代
  for (let iter = 0; iter < 100; iter++) {
    // 排斥力
    for (let i = 0; i < nodes.length; i++) {
      for (let j = i + 1; j < nodes.length; j++) {
        let dx = nodes[j].x - nodes[i].x
        let dy = nodes[j].y - nodes[i].y
        let dist = Math.sqrt(dx * dx + dy * dy) || 1
        let force = 5000 / (dist * dist)
        nodes[i].vx -= dx / dist * force
        nodes[i].vy -= dy / dist * force
        nodes[j].vx += dx / dist * force
        nodes[j].vy += dy / dist * force
      }
    }
    // 吸引力
    edges.forEach(e => {
      const a = nodeMap[e.fromNodeId], b = nodeMap[e.toNodeId]
      if (!a || !b) return
      let dx = b.x - a.x, dy = b.y - a.y
      let dist = Math.sqrt(dx * dx + dy * dy) || 1
      let force = (dist - 100) * 0.01
      a.vx += dx / dist * force
      a.vy += dy / dist * force
      b.vx -= dx / dist * force
      b.vy -= dy / dist * force
    })
    // 中心引力
    nodes.forEach(n => {
      n.vx += (svgWidth.value / 2 - n.x) * 0.001
      n.vy += (svgHeight.value / 2 - n.y) * 0.001
      n.vx *= 0.9; n.vy *= 0.9
      n.x += n.vx; n.y += n.vy
      n.x = Math.max(40, Math.min(svgWidth.value - 40, n.x))
      n.y = Math.max(40, Math.min(svgHeight.value - 40, n.y))
    })
  }

  renderedNodes.value = nodes
  renderedEdges.value = edges.map(e => {
    const a = nodeMap[e.fromNodeId], b = nodeMap[e.toNodeId]
    if (!a || !b) return null
    return { ...e, x1: a.x, y1: a.y, x2: b.x, y2: b.y }
  }).filter(Boolean)
}

onMounted(async () => {
  try { projectList.value = (await projectApi.getAll()) || [] } catch {}
  if (graphAreaRef.value) {
    svgWidth.value = graphAreaRef.value.clientWidth || 800
    svgHeight.value = 500
  }
})

const loadGraph = async () => {
  if (!selectedProject.value) return
  loading.value = true
  try {
    const res = await knowledgeGraphApi.getGraph(selectedProject.value)
    graphData.value = { nodes: res.nodes || [], edges: res.edges || [] }
    stats.value = res.stats || {}
    await nextTick()
    if (graphAreaRef.value) svgWidth.value = graphAreaRef.value.clientWidth || 800
    layoutGraph()
  } catch { ElMessage.error('加载失败') } finally { loading.value = false }
}

const handleBuild = async () => {
  if (!selectedProject.value) { ElMessage.warning('请先选择项目'); return }
  building.value = true
  try {
    const res = await knowledgeGraphApi.build(selectedProject.value)
    ElMessage.success(`构建完成: ${res.nodeCount} 节点, ${res.edgeCount} 边`)
    await loadGraph()
  } catch { ElMessage.error('构建失败') } finally { building.value = false }
}

const handleSearch = async () => {
  if (!selectedProject.value || !searchKeyword.value) return
  try { searchResults.value = await knowledgeGraphApi.search(selectedProject.value, searchKeyword.value) || [] } catch { ElMessage.error('搜索失败') }
}

const selectNode = async (node) => {
  selectedNode.value = node
  try {
    const res = await knowledgeGraphApi.getNeighbors(selectedProject.value, node.id)
    nodeNeighbors.value = (res.nodes || []).filter(n => n.id !== node.id)
  } catch { nodeNeighbors.value = [] }
}

const truncate = (s, n) => s && s.length > n ? s.slice(0, n) + '...' : s || ''
</script>

<style scoped>
.stat-cards { margin-bottom: 16px; }
.stat-card .el-card__body { display: flex; align-items: center; width: 100%; }
.stat-icon { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; margin-right: 12px; }
.stat-info { flex: 1; }
.stat-value { font-size: 24px; font-weight: bold; }
.stat-label { font-size: 12px; color: #909399; }
.section-card { margin-bottom: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 10px; align-items: center; }
.graph-wrapper { position: relative; }
.graph-legend { display: flex; gap: 16px; margin-bottom: 12px; flex-wrap: wrap; }
.legend-item { display: flex; align-items: center; gap: 4px; font-size: 13px; color: #606266; }
.legend-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.graph-area { border: 1px solid #ebeef5; border-radius: 8px; background: #fafafa; overflow: hidden; }
.graph-svg { display: block; }
.node-group { cursor: pointer; }
.node-group:hover circle { stroke-width: 3; filter: brightness(1.1); }
.node-group.selected circle { stroke: #409eff; stroke-width: 3; }
.node-label { font-size: 12px; fill: #303133; font-weight: 500; }
.node-type { font-size: 10px; fill: #909399; }
.node-detail-panel { position: absolute; right: 16px; top: 16px; width: 280px; background: #fff; border-radius: 8px; box-shadow: 0 4px 16px rgba(0,0,0,0.12); padding: 16px; z-index: 20; }
.panel-header { display: flex; justify-content: space-between; align-items: center; font-weight: bold; margin-bottom: 12px; }
.panel-subtitle { font-size: 13px; color: #909399; margin-bottom: 8px; }
.neighbor-item { display: flex; align-items: center; gap: 6px; padding: 4px 0; cursor: pointer; font-size: 13px; }
.neighbor-item:hover { color: #409eff; }
.table-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; font-weight: bold; }
.slide-enter-active, .slide-leave-active { transition: all 0.3s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateX(20px); }
</style>
