# 仓库管理系统（无数据库，小体量）

技术栈：

- 前端：Vue 3 + Vite
- 后端：Spring Boot（REST API）
- 存储：**无数据库**，后端使用内存保存物料与出入库流水（重启会清空并重新生成演示数据）

## 目录结构

- `backend/`：Spring Boot 后端
- `frontend/`：Vue3 前端

## 启动后端

前置：需要 **JDK（不是 JRE）**。如果你运行 `mvn` 时报：

> No compiler is provided in this environment. Perhaps you are running on a JRE rather than a JDK?

请把 `JAVA_HOME` 指向 JDK（8+），并确保 `PATH` 里能找到 `javac`。

在根目录执行：

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8080`

## 启动前端

在根目录执行：

```bash
cd frontend
npm install
npm run dev
```

默认端口：`5173`

前端已配置代理：`/api -> http://localhost:8080`（见 `frontend/vite.config.js`），所以页面里直接请求 `/api/**` 即可联调。

## API 概览

基础路径：`/api`

- `GET /items`：物料列表（可选 `q` 搜索）
- `POST /items`：新增物料
- `PUT /items/{id}`：编辑物料
- `DELETE /items/{id}`：删除物料
- `POST /items/{id}/inbound`：入库（`{ quantity, note }`）
- `POST /items/{id}/outbound`：出库（`{ quantity, note }`）
- `GET /txns`：流水列表（可选 `itemId`）

