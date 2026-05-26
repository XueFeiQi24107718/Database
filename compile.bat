@echo off
setlocal
cd /d "%~dp0"

if not exist lib mkdir lib

set MYSQL_JAR=
for %%f in (lib\mysql-connector-j-*.jar lib\mysql-connector-java-*.jar) do set MYSQL_JAR=%%f

if "%MYSQL_JAR%"=="" (
  echo ERROR: Place mysql-connector-j-*.jar in lib\
  echo Download: https://dev.mysql.com/downloads/connector/j/
  exit /b 1
)

if not exist out mkdir out

javac -encoding UTF-8 -d out -cp "%MYSQL_JAR%" -sourcepath src src\com\ecommerce\app\EcommerceApp.java
if errorlevel 1 exit /b 1
echo Compile OK.

