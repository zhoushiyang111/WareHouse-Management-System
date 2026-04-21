<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createItem,
  deleteItem,
  inbound,
  listItems,
  listTxns,
  outbound,
  updateItem,
} from './api/wms'

const loading = ref(false)
const error = ref('')

const q = ref('')
const items = ref([])
const txns = ref([])

const createForm = reactive({
  sku: '',
  name: '',
  location: '',
  customerName: '',
})

const editId = ref(null)
const editForm = reactive({
  sku: '',
  name: '',
  location: '',
  customerName: '',
})

const qtyById = reactive({})
const noteById = reactive({})

const selectedItem = computed(() => items.value.find((i) => i.id === editId.value) || null)

async function refresh() {
  loading.value = true
  error.value = ''
  try {
    items.value = await listItems(q.value)
    txns.value = await listTxns()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

function startEdit(item) {
  editId.value = item.id
  editForm.sku = item.sku
  editForm.name = item.name
  editForm.location = item.location || ''
  editForm.customerName = item.customerName || ''
}

function cancelEdit() {
  editId.value = null
  editForm.sku = ''
  editForm.name = ''
  editForm.location = ''
  editForm.customerName = ''
}

function getStorageDuration(createdAt) {
  if (!createdAt) return '-'
  const created = new Date(createdAt)
  const now = new Date()
  const diffMs = now - created
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  const diffHours = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60))

  if (diffDays > 0) {
    if (diffHours > 0) {
      return `${diffDays}天${diffHours}小时`
    }
    return `${diffDays}天`
  } else if (diffHours > 0) {
    if (diffMinutes > 0) {
      return `${diffHours}小时${diffMinutes}分钟`
    }
    return `${diffHours}小时`
  } else if (diffMinutes > 0) {
    return `${diffMinutes}分钟`
  } else {
    return '刚刚'
  }
}

function getOutboundSummary(itemId) {
  const itemTxns = txns.value.filter(t => t.itemId === itemId && t.type === 'OUTBOUND')
  if (itemTxns.length === 0) return '-'
  const totalQty = itemTxns.reduce((sum, t) => sum + t.quantity, 0)
  return `出库${itemTxns.length}次，共${totalQty}件`
}

async function onCreate() {
  if (!createForm.sku.trim() || !createForm.name.trim()) return
  loading.value = true
  error.value = ''
  try {
    await createItem({
      sku: createForm.sku.trim(),
      name: createForm.name.trim(),
      location: createForm.location.trim(),
      customerName: createForm.customerName.trim(),
      stock: 0,
    })
    createForm.sku = ''
    createForm.name = ''
    createForm.location = ''
    createForm.customerName = ''
    await refresh()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

async function onSaveEdit() {
  if (!editId.value) return
  loading.value = true
  error.value = ''
  try {
    await updateItem(editId.value, {
      sku: editForm.sku.trim(),
      name: editForm.name.trim(),
      location: editForm.location.trim(),
      customerName: editForm.customerName.trim(),
      stock: selectedItem.value?.stock ?? 0,
    })
    cancelEdit()
    await refresh()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

async function onDelete(item) {
  if (!confirm(`确认删除物料：${item.sku} / ${item.name} ?`)) return
  loading.value = true
  error.value = ''
  try {
    await deleteItem(item.id)
    if (editId.value === item.id) cancelEdit()
    await refresh()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

function getQty(itemId) {
  const n = Number(qtyById[itemId] ?? 0)
  return Number.isFinite(n) ? Math.floor(n) : 0
}

function getNote(itemId) {
  return String(noteById[itemId] ?? '').trim()
}

async function onInbound(item) {
  const qty = getQty(item.id)
  if (qty <= 0) return
  loading.value = true
  error.value = ''
  try {
    await inbound(item.id, { quantity: qty, note: getNote(item.id) })
    qtyById[item.id] = ''
    noteById[item.id] = ''
    await refresh()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

async function onOutbound(item) {
  const qty = getQty(item.id)
  if (qty <= 0) return
  loading.value = true
  error.value = ''
  try {
    await outbound(item.id, { quantity: qty, note: getNote(item.id) })
    qtyById[item.id] = ''
    noteById[item.id] = ''
    await refresh()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

onMounted(refresh)
</script>

<template>
  <div class="page">
    <header class="header">
      <div>
        <div class="title">仓库管理系统</div>
        <div class="subtitle">Vue3 + Spring Boot（无数据库 / 内存数据）</div>
      </div>
      <div class="toolbar">
        <input v-model="q" class="input" placeholder="搜索 SKU / 名称 / 库位" @keyup.enter="refresh" />
        <button class="btn" :disabled="loading" @click="refresh">刷新</button>
      </div>
    </header>

    <div v-if="error" class="alert">{{ error }}</div>

    <section class="card">
      <div class="cardTitle">新增物料</div>
      <div class="cardBody">
        <div class="row">
          <input v-model="createForm.sku" class="input" placeholder="SKU（唯一）" />
          <input v-model="createForm.name" class="input" placeholder="名称" />
          <input v-model="createForm.location" class="input" placeholder="库位（可选）" />
          <input v-model="createForm.customerName" class="input" placeholder="所属客户（可选）" />
          <button class="btn primary" :disabled="loading" @click="onCreate">新增</button>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="cardTitle">物料列表</div>
      <div class="tableWrap">
        <table class="table">
          <thead>
            <tr>
              <th class="wId">ID</th>
              <th>SKU</th>
              <th>名称</th>
              <th>库位</th>
              <th>所属客户</th>
              <th>堆放时长</th>
              <th class="wStock">库存</th>
              <th>出库记录</th>
              <th class="wOps">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="it in items" :key="it.id" :class="{ active: editId === it.id }">
              <td>{{ it.id }}</td>
              <td class="mono">{{ it.sku }}</td>
              <td>{{ it.name }}</td>
              <td>{{ it.location || '-' }}</td>
              <td>{{ it.customerName || '-' }}</td>
              <td class="mono">{{ getStorageDuration(it.createdAt) }}</td>
              <td class="stock">{{ it.stock }}</td>
              <td>{{ getOutboundSummary(it.id) }}</td>
              <td>
                <div class="ops">
                  <button class="btn sm" :disabled="loading" @click="startEdit(it)">编辑</button>
                  <button class="btn sm danger" :disabled="loading" @click="onDelete(it)">删除</button>
                </div>
                <div class="ops2">
                  <input v-model="qtyById[it.id]" class="input sm" placeholder="数量" />
                  <input v-model="noteById[it.id]" class="input sm" placeholder="备注（可选）" />
                  <button class="btn sm success" :disabled="loading" @click="onInbound(it)">入库</button>
                  <button class="btn sm warning" :disabled="loading" @click="onOutbound(it)">出库</button>
                </div>
              </td>
            </tr>
            <tr v-if="!items.length">
              <td colspan="9" class="empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="card" v-if="editId">
      <div class="cardTitle">编辑物料 #{{ editId }}</div>
      <div class="cardBody">
        <div class="row full">
          <input v-model="editForm.sku" class="input" placeholder="SKU" />
          <input v-model="editForm.name" class="input" placeholder="名称" />
          <input v-model="editForm.location" class="input" placeholder="库位（可选）" />
          <input v-model="editForm.customerName" class="input" placeholder="所属客户（可选）" />
          <button class="btn primary" :disabled="loading" @click="onSaveEdit">保存</button>
          <button class="btn" :disabled="loading" @click="cancelEdit">取消</button>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="cardTitle">最近出入库流水</div>
      <div class="tableWrap">
        <table class="table">
          <thead>
            <tr>
              <th class="wId">ID</th>
              <th class="wId">物料ID</th>
              <th class="wId">类型</th>
              <th class="wStock">数量</th>
              <th>备注</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in txns.slice(0, 30)" :key="t.id">
              <td>{{ t.id }}</td>
              <td>{{ t.itemId }}</td>
              <td :class="{ inbound: t.type === 'INBOUND', outbound: t.type === 'OUTBOUND' }">
                {{ t.type === 'INBOUND' ? '入库' : '出库' }}
              </td>
              <td class="stock">{{ t.quantity }}</td>
              <td>{{ t.note || '-' }}</td>
              <td class="mono">{{ t.createdAt }}</td>
            </tr>
            <tr v-if="!txns.length">
              <td colspan="6" class="empty">暂无流水</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style>
:root {
  color-scheme: light;
  --bg: #f5f7fa;
  --card: #ffffff;
  --card-header: #fafbfc;
  --border: #e5e7eb;
  --border-light: #f0f2f5;
  --text: #1f2937;
  --text-secondary: #6b7280;
  --text-muted: #9ca3af;
  --accent: #1890ff;
  --accent-hover: #40a9ff;
  --accent-light: #e6f7ff;
  --danger: #f5222d;
  --danger-hover: #ff4d4f;
  --danger-light: #fff2f0;
  --success: #52c41a;
  --success-light: #f6ffed;
  --warning: #faad14;
  --warning-light: #fffbe6;
  --shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
  --shadow-hover: 0 4px 12px rgba(0, 0, 0, 0.1);
}

body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 
    'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
    'Noto Color Emoji';
  background: var(--bg);
  color: var(--text);
  font-size: 14px;
  line-height: 1.5715;
}

.page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 24px 40px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
  padding: 0 4px;
}

.title {
  font-weight: 600;
  font-size: 20px;
  color: var(--text);
  letter-spacing: 0.3px;
}

.subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
}

.card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 6px;
  margin-top: 16px;
  box-shadow: var(--shadow);
  overflow: hidden;
}

.cardTitle {
  font-weight: 600;
  font-size: 15px;
  padding: 14px 20px;
  background: var(--card-header);
  border-bottom: 1px solid var(--border-light);
  color: var(--text);
}

.card .cardBody {
  padding: 16px 20px;
}

.row {
  display: grid;
  grid-template-columns: 1fr 1.2fr 1fr 1fr auto;
  gap: 12px;
  align-items: center;
}

.row.full {
  grid-template-columns: 1fr 1.2fr 1fr 1fr auto auto;
}

.input {
  width: 100%;
  box-sizing: border-box;
  padding: 7px 12px;
  border-radius: 4px;
  border: 1px solid var(--border);
  background: var(--card);
  color: var(--text);
  outline: none;
  font-size: 14px;
  transition: all 0.2s ease;
  height: 32px;
  line-height: 1.5715;
}

.input:hover:not(:disabled) {
  border-color: #c9cdd4;
}

.input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.15);
  border-color: var(--accent);
}

.input:disabled {
  background-color: #f5f7fa;
  color: var(--text-muted);
  cursor: not-allowed;
}

.input::placeholder {
  color: var(--text-muted);
}

.btn {
  padding: 7px 16px;
  border-radius: 4px;
  border: 1px solid var(--border);
  background: var(--card);
  color: var(--text);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
  white-space: nowrap;
  height: 32px;
  line-height: 1.5715;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn:hover:not(:disabled) {
  color: var(--accent);
  border-color: var(--accent);
}

.btn:active:not(:disabled) {
  color: #096dd9;
  border-color: #096dd9;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  color: var(--text-muted);
  background: #f5f7fa;
  border-color: #d9d9d9;
}

.btn.primary {
  border-color: var(--accent);
  background: var(--accent);
  color: #fff;
  box-shadow: 0 2px 0 rgba(0, 0, 0, 0.045);
}

.btn.primary:hover:not(:disabled) {
  background: var(--accent-hover);
  border-color: var(--accent-hover);
}

.btn.primary:active:not(:disabled) {
  background: #096dd9;
  border-color: #096dd9;
}

.btn.danger {
  border-color: var(--danger);
  background: #fff;
  color: var(--danger);
}

.btn.danger:hover:not(:disabled) {
  background: var(--danger);
  border-color: var(--danger);
  color: #fff;
}

.btn.danger:active:not(:disabled) {
  background: #cf1322;
  border-color: #cf1322;
  color: #fff;
}

.btn.success {
  border-color: var(--success);
  background: #fff;
  color: var(--success);
}

.btn.success:hover:not(:disabled) {
  background: var(--success);
  border-color: var(--success);
  color: #fff;
}

.btn.success:active:not(:disabled) {
  background: #389e0d;
  border-color: #389e0d;
  color: #fff;
}

.btn.warning {
  border-color: var(--warning);
  background: #fff;
  color: var(--warning);
}

.btn.warning:hover:not(:disabled) {
  background: var(--warning);
  border-color: var(--warning);
  color: #fff;
}

.btn.warning:active:not(:disabled) {
  background: #d48806;
  border-color: #d48806;
  color: #fff;
}

.btn.sm {
  padding: 4px 12px;
  font-size: 13px;
  border-radius: 4px;
  height: 28px;
}

.input.sm {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 13px;
  height: 28px;
}

.alert {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 4px;
  border: 1px solid #ffccc7;
  background: var(--danger-light);
  color: var(--danger);
  font-size: 14px;
}

.tableWrap {
  overflow: auto;
  border: 1px solid var(--border);
  border-top: none;
}

.table {
  width: 100%;
  border-collapse: collapse;
  min-width: 1200px;
}

.table th,
.table td {
  text-align: left;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
  vertical-align: middle;
}

.table thead th {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--card-header);
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}

.table tbody tr:hover {
  background: #fafafa;
}

.table tbody tr.active {
  background: var(--accent-light);
}

.ops {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  align-items: center;
}

.ops2 {
  display: grid;
  grid-template-columns: 100px 1fr auto auto;
  gap: 8px;
  align-items: center;
}

.ops2 .input {
  margin: 0;
}

.ops2 .btn {
  margin: 0;
}

.mono {
  font-family: 'SF Mono', SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
}

.stock {
  font-weight: 600;
  color: var(--accent);
}

.wId {
  width: 80px;
}

.wStock {
  width: 90px;
}

.wOps {
  width: 440px;
}

.empty {
  padding: 32px 16px;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
}

.inbound {
  color: var(--success);
  font-weight: 500;
  padding: 2px 8px;
  background: var(--success-light);
  border-radius: 4px;
  font-size: 12px;
  display: inline-block;
}

.outbound {
  color: var(--warning);
  font-weight: 500;
  padding: 2px 8px;
  background: var(--warning-light);
  border-radius: 4px;
  font-size: 12px;
  display: inline-block;
}

@media (max-width: 900px) {
  .header {
    flex-direction: column;
    align-items: stretch;
  }
  .row {
    grid-template-columns: 1fr;
  }
  .toolbar {
    width: 100%;
  }
  .ops2 {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
