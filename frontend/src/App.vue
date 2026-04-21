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
})

const editId = ref(null)
const editForm = reactive({
  sku: '',
  name: '',
  location: '',
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
}

function cancelEdit() {
  editId.value = null
  editForm.sku = ''
  editForm.name = ''
  editForm.location = ''
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
      stock: 0,
    })
    createForm.sku = ''
    createForm.name = ''
    createForm.location = ''
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
      <div class="row">
        <input v-model="createForm.sku" class="input" placeholder="SKU（唯一）" />
        <input v-model="createForm.name" class="input" placeholder="名称" />
        <input v-model="createForm.location" class="input" placeholder="库位（可选）" />
        <button class="btn primary" :disabled="loading" @click="onCreate">新增</button>
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
              <th class="wStock">库存</th>
              <th class="wOps">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="it in items" :key="it.id" :class="{ active: editId === it.id }">
              <td>{{ it.id }}</td>
              <td class="mono">{{ it.sku }}</td>
              <td>{{ it.name }}</td>
              <td>{{ it.location || '-' }}</td>
              <td class="stock">{{ it.stock }}</td>
              <td>
                <div class="ops">
                  <button class="btn sm" :disabled="loading" @click="startEdit(it)">编辑</button>
                  <button class="btn sm danger" :disabled="loading" @click="onDelete(it)">删除</button>
                </div>
                <div class="ops2">
                  <input v-model="qtyById[it.id]" class="input sm" placeholder="数量" />
                  <input v-model="noteById[it.id]" class="input sm" placeholder="备注（可选）" />
                  <button class="btn sm" :disabled="loading" @click="onInbound(it)">入库</button>
                  <button class="btn sm" :disabled="loading" @click="onOutbound(it)">出库</button>
                </div>
              </td>
            </tr>
            <tr v-if="!items.length">
              <td colspan="6" class="empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="card" v-if="editId">
      <div class="cardTitle">编辑物料 #{{ editId }}</div>
      <div class="row">
        <input v-model="editForm.sku" class="input" placeholder="SKU" />
        <input v-model="editForm.name" class="input" placeholder="名称" />
        <input v-model="editForm.location" class="input" placeholder="库位（可选）" />
        <button class="btn primary" :disabled="loading" @click="onSaveEdit">保存</button>
        <button class="btn" :disabled="loading" @click="cancelEdit">取消</button>
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
  --bg: #0b1020;
  --card: rgba(255, 255, 255, 0.08);
  --card2: rgba(255, 255, 255, 0.06);
  --border: rgba(255, 255, 255, 0.12);
  --text: rgba(255, 255, 255, 0.92);
  --muted: rgba(255, 255, 255, 0.65);
  --accent: #6ae4ff;
  --danger: #ff5f6d;
}

body {
  margin: 0;
  font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, "Apple Color Emoji",
    "Segoe UI Emoji";
  background: radial-gradient(1200px 800px at 20% 0%, #18224a 0%, var(--bg) 55%, #060913 100%);
  color: var(--text);
}

.page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 16px 60px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.title {
  font-weight: 800;
  font-size: 22px;
  letter-spacing: 0.4px;
}

.subtitle {
  margin-top: 2px;
  font-size: 13px;
  color: var(--muted);
}

.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
}

.card {
  background: linear-gradient(180deg, var(--card) 0%, var(--card2) 100%);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 14px;
  margin-top: 12px;
  backdrop-filter: blur(10px);
}

.cardTitle {
  font-weight: 700;
  margin-bottom: 10px;
}

.row {
  display: grid;
  grid-template-columns: 1.1fr 1.3fr 1.1fr auto;
  gap: 10px;
}

.input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.06);
  color: var(--text);
  outline: none;
}

.input::placeholder {
  color: rgba(255, 255, 255, 0.45);
}

.btn {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.08);
  color: var(--text);
  cursor: pointer;
}

.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn.primary {
  border-color: rgba(106, 228, 255, 0.35);
  background: rgba(106, 228, 255, 0.14);
}

.btn.danger {
  border-color: rgba(255, 95, 109, 0.35);
  background: rgba(255, 95, 109, 0.14);
}

.btn.sm {
  padding: 7px 10px;
  border-radius: 9px;
}

.input.sm {
  padding: 7px 10px;
  border-radius: 9px;
}

.alert {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(255, 95, 109, 0.35);
  background: rgba(255, 95, 109, 0.12);
}

.tableWrap {
  overflow: auto;
  border-radius: 12px;
  border: 1px solid var(--border);
}

.table {
  width: 100%;
  border-collapse: collapse;
  min-width: 880px;
}

.table th,
.table td {
  text-align: left;
  padding: 10px 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  vertical-align: top;
}

.table thead th {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
  background: rgba(0, 0, 0, 0.18);
}

.table tbody tr.active {
  outline: 1px solid rgba(106, 228, 255, 0.35);
  background: rgba(106, 228, 255, 0.06);
}

.ops {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.ops2 {
  display: grid;
  grid-template-columns: 90px 1fr auto auto;
  gap: 8px;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}

.stock {
  font-weight: 800;
}

.wId {
  width: 70px;
}

.wStock {
  width: 80px;
}

.wOps {
  width: 420px;
}

.empty {
  padding: 18px 10px;
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
}

.inbound {
  color: #7dffb3;
  font-weight: 700;
}

.outbound {
  color: #ffc36a;
  font-weight: 700;
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
}
</style>
