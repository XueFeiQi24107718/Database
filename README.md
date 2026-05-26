# COMP2013J – Mini E‑Commerce Management System (MySQL)

Java + JDBC + **MySQL** + Swing UI（不使用 ORM / 复杂 MVC 框架）。

## 1) 安装 MySQL
安装 MySQL Server（可选 Workbench）。记住 root 密码，或创建专用用户。

## 2) 创建数据库（只需一次）
在 Workbench 或命令行执行：

```sql
-- file: sql/00_create_database.sql
CREATE DATABASE IF NOT EXISTS ecommerce_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

或：

```bat
mysql -u root -p < sql\00_create_database.sql
```

## 3) 配置数据库连接
1. 复制 `config/db.properties.example` → `config/db.properties`
2. 修改 `config/db.properties`：

```properties
db.url=jdbc:mysql://localhost:3306/ecommerce_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=YOUR_REAL_PASSWORD
```

## 4) 放置 MySQL JDBC Driver
1. 下载 **mysql-connector-j** (.jar): https://dev.mysql.com/downloads/connector/j/
2. 把 `mysql-connector-j-9.x.x.jar` 放入 `lib/` 目录

## 5) 编译与运行

```bat
compile.bat
run.bat
```

首次运行：程序会自动执行 `sql/schema.sql` 和 `sql/seed.sql`（当 `users` 表为空时）。

## 6) 登录账号（seed 数据）
- 管理员：admin / admin  （Role = ADMIN）
- 顾客：customer / customer  （Role = CUSTOMER）

## 7) 功能概览
- 管理员（ADMIN）
  - 分类管理（增删改查）
  - 商品管理（增删改查、上/下架、库存调整）
  - 订单管理（查看所有订单、更新订单状态）
- 顾客（CUSTOMER）
  - 浏览/搜索商品、加入购物车
  - 购物车增删改数量
  - 结算下单（自动扣减库存、生成订单与订单明细）
  - 查看自己的订单与订单明细

## 提交建议（zip）
包含：`src/`, `sql/`, `config/db.properties.example`（不要提交真实密码）, `README.md`, `compile.bat`, `run.bat`

