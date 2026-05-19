@echo off
setlocal
cd /d "%~dp0"

set MYSQL_JAR=
for %%f in (lib\mysql-connector-j-*.jar lib\mysql-connector-java-*.jar) do set MYSQL_JAR=%%f

if "%MYSQL_JAR%"=="" (
  echo ERROR: Place mysql-connector-j-*.jar in lib\
  exit /b 1
)

if not exist out\com\library\app\LibraryApp.class (
  call compile.bat
  if errorlevel 1 exit /b 1
)

java -cp "out;%MYSQL_JAR%" com.library.app.LibraryApp
