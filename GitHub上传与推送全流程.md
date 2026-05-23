# 项目上传到 GitHub 全流程（含后续更新推送）

本文说明：从本地新项目到首次推送到 GitHub，以及之后日常如何提交、推送更新。适用于本仓库（**Spring Boot `backend/` + Vue `frontend/` 单体结构**）及同类场景（HTTPS、Windows、可选代理）。

### 本仓库目录约定

**GitHub 仓库**：https://github.com/wushij/wu-admin

```
admin-vue/（本地目录名可与仓库名不同）
├── backend/      # Spring Boot 后端
├── frontend/     # Vue 3 前端
├── sql/          # 数据库脚本（admin_platform.sql、add1.sql）
└── ...
```

绑定远程示例：

```bash
git remote add origin https://github.com/wushij/wu-admin.git
# 若已绑定旧地址：
git remote set-url origin https://github.com/wushij/wu-admin.git
```

提交前请确认 `.gitignore` 已排除 `backend/target/`、`frontend/node_modules/`、`data/` 等目录。

---

## 一、开始前准备

### 1. 安装 Git

从 [Git 官网](https://git-scm.com/download/win) 安装。安装时若勾选 **Git Credential Manager**，推送 HTTPS 时可用浏览器登录 GitHub。

### 2. 配置提交用的名字和邮箱（全局，一次即可）

名字建议用 **GitHub 登录名**；邮箱用已在 GitHub **Settings → Emails** 里添加并验证的邮箱（否则网页上可能无法把提交关联到你的账号）。

```bash
git config --global user.name "你的GitHub登录名"
git config --global user.email "你的邮箱@example.com"
```

仅当前仓库生效（不写 `--global`）：

```bash
cd 项目根目录
git config user.name "你的GitHub登录名"
git config user.email "你的邮箱@example.com"
```

### 3. 在项目根目录准备 `.gitignore`（强烈建议在第一次 `git add` 前完成）

- Git **不会**自动创建 `.gitignore`，需要自行添加。
- 作用：让 `node_modules/`、`target/`、`.idea/`、`.env` 等大文件或敏感文件**不被提交**，减小体积、避免泄密。
- 可从本仓库根目录的 `.gitignore` 复制到其他项目，再按语言增删规则；也可在 GitHub 建库时勾选模板，或参考 [github/gitignore](https://github.com/github/gitignore)。

---

## 二、在 GitHub 上创建空仓库

1. 登录 [GitHub](https://github.com)，右上角 **+** → **New repository**。
2. 填写仓库名（Repository name），选择 Public / Private。
3. **不要**勾选 *Add a README*、*Add .gitignore*、*License*（本地已有项目时，空仓库最省事，避免首次推送冲突）。
4. 创建后复制仓库地址，例如：  
   `https://github.com/你的用户名/仓库名.git`

---

## 三、本地：初始化与首次提交

在**项目根目录**打开终端（本示例路径请改成你的实际路径）：

```bash
cd d:\你的项目路径
git init
git add .
git status
```

检查 `git status`：不应出现本不该进库的大目录（如 `frontend/node_modules`、`backend/target/`、`data/`）。若误加过，可先纠正再提交：

```bash
git rm -r --cached .
git add .
```

然后提交：

```bash
git commit -m "Initial commit"
```

若提示必须配置用户信息，回到「一、2」设置 `user.name` / `user.email` 后再执行 `git commit`。

---

## 四、绑定远程并首次推送

### 1. 添加远程（`origin` 为习惯命名，可改）

```bash
git remote add origin https://github.com/你的用户名/仓库名.git
```

若已加错，可先删除再添加：

```bash
git remote remove origin
git remote add origin https://github.com/你的用户名/仓库名.git
```

查看远程：

```bash
git remote -v
```

### 2. 确认分支名并推送

查看当前分支：

```bash
git branch
```

- 若当前分支是 **main**：

  ```bash
  git push -u origin main
  ```

- 若当前分支是 **master**：

  ```bash
  git push -u origin master
  ```

也可先把本地默认分支改名为 `main` 再推送（与 GitHub 默认习惯一致）：

```bash
git branch -M main
git push -u origin main
```

`-u`（`--set-upstream`）只需在**第一次**推送时使用，之后同一分支可直接 `git push`。

### 3. 身份验证（HTTPS）

- 首次 `git push` 可能提示在**浏览器**中完成登录，按提示操作即可。
- 若要求输入密码：应使用 **Personal Access Token（PAT）**，而不是 GitHub 登录密码。生成路径：**GitHub → Settings → Developer settings → Personal access tokens**，勾选 `repo` 等所需权限。

### 4. 网络无法连接 GitHub（可选：代理）

若出现 `Failed to connect to github.com port 443`、超时等，需先保证本机能访问 GitHub（换网络、VPN、系统代理等）。

使用 **Clash** 等本地 HTTP 代理时（端口以你本机为准，例如 `7897`）：

```bash
git config --global http.proxy http://127.0.0.1:7897
git config --global https.proxy http://127.0.0.1:7897
```

**仅让 GitHub 走代理**（其它 Git 地址不走代理，可按需使用）：

```bash
git config --global http.https://github.com.proxy http://127.0.0.1:7897
git config --global https.https://github.com.proxy http://127.0.0.1:7897
```

取消全局 HTTP 代理：

```bash
git config --global --unset http.proxy
git config --global --unset https.proxy
```

### 5. SSH 方式（可选）

若已配置 SSH 公钥到 GitHub，可将远程改为 SSH 地址：

```bash
git remote set-url origin git@github.com:你的用户名/仓库名.git
git push -u origin main
```

---

## 五、后续日常：修改代码后如何更新到 GitHub

在已关联 `origin`、且已用 `-u` 跟踪过分支的情况下，典型流程如下。

### 1. 拉取远程最新（多人协作或本机多设备时建议先做）

```bash
cd 项目根目录
git pull
```

若远程分支与本地不一致，可能需：

```bash
git pull origin main
```

（将 `main` 换成你实际跟踪的分支名。）

### 2. 保存修改并推送

```bash
git add .
git status
git commit -m "简要说明本次修改"
git push
```

仅提交部分文件时：

```bash
git add path/to/file1 path/to/file2
git commit -m "说明"
git push
```

### 3. 查看提交历史与远程状态

```bash
git log --oneline -5
git status
git remote -v
```

---

## 六、常见问题简表

| 现象 | 处理方向 |
|------|----------|
| `remote origin already exists` | `git remote remove origin` 后重新 `git remote add`。 |
| `rejected` 且远程已有 README 等提交 | 先 `git pull origin 分支名 --rebase` 再 `git push`，或新建空仓库重来。 |
| 误把 `node_modules` 等提交进库 | 补充 `.gitignore` → `git rm -r --cached .` → `git add .` → `git commit -m "chore: apply gitignore"` → `git push`。 |
| 连不上 `github.com:443` | 检查网络/代理；Clash 开启且端口与 `git config` 一致。 |
| 推送成功但网页上看不到 | 确认浏览器打开的是对应用户/仓库 URL，分支是否选对了 `main`/`master`。 |

---

## 七、流程速查（复制用）

**首次上传**

1. 根目录放好 `.gitignore`  
2. `git init` → `git add .` → `git commit -m "Initial commit"`  
3. GitHub 建**空**仓库，复制 HTTPS 地址  
4. `git remote add origin <地址>`  
5. `git push -u origin main`（或 `master`）  
6. 按提示完成浏览器登录或 PAT  

**日常更新**

1. （可选）`git pull`  
2. `git add .`  
3. `git commit -m "说明"`  
4. `git push`  

---
