# COMP2013J – Library Management System (MySQL)

Java + JDBC + **MySQL** + Swing UI. Standalone project; not related to other assignments.

## 1. Install MySQL

1. Install **MySQL Server** (and MySQL Workbench if you like).
2. Remember your **root password** (or create a dedicated user).

## 2. Create the database (one time)

In MySQL Workbench or command line, run:

```sql
-- file: sql/00_create_database.sql
CREATE DATABASE IF NOT EXISTS library_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Or:

```bat
mysql -u root -p < sql\00_create_database.sql
```

## 3. Configure the app

1. Copy `config/db.properties.example` → `config/db.properties` (if needed).
2. Edit `config/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/library_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=YOUR_REAL_PASSWORD
```

## 4. Add MySQL JDBC driver

1. Download **mysql-connector-j** (.jar) from https://dev.mysql.com/downloads/connector/j/
2. Put `mysql-connector-j-9.x.x.jar` in the `lib/` folder.

## 5. Compile and run

```bat
compile.bat
run.bat
```

**Login:** `admin` / `admin` or `librarian` / `book`

On **first run**, the program automatically runs `sql/schema.sql` and `sql/seed.sql` if tables are empty.

You can also run those SQL files manually in Workbench (good for screenshots in your report).

## What you do vs what the program does

| You (once) | Program (every run) |
|------------|---------------------|
| Install MySQL | Connect via JDBC |
| Create `library_db` | Create tables + sample data if empty |
| Set password in `db.properties` | INSERT/UPDATE/DELETE through DAO classes |
| Optional: run SQL in Workbench for report | Mark overdue loans on startup |

## Manual setup (alternative)

Instead of auto-init, run in Workbench on `library_db`:

1. `sql/schema.sql`
2. `sql/seed.sql`

Then start the app; it will only refresh overdue status.

## Report / ER diagram

Tables: `staff`, `member`, `book`, `author`, `category`, `loan`, `fine`, `book_author`, `book_category`.

Use `sql/schema.sql` for CREATE TABLE screenshots and mapping from your ER diagram.

## Submission zip

Include: `src/`, `sql/`, `config/db.properties.example` (not your real password), `lib/mysql-connector-j-*.jar`, `README.md`, `compile.bat`, `run.bat`.

## AI disclosure

If AI helped with code, state that clearly in your report and explain what the team tested and understood.

## 祁雪菲是小猫

嘻嘻