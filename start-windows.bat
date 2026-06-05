@echo off
setlocal

cd /d "%~dp0"

if not defined JAR set "JAR=leaf-1.21.11-158.jar"

rem Windows Server 2022 with 8 GB total RAM:
rem keep about 2 GB for Windows, Java native memory, plugins, and file cache.
if not defined MIN_MEMORY set "MIN_MEMORY=4G"
if not defined MEMORY set "MEMORY=6G"

if not exist logs mkdir logs

java ^
  -Xms%MIN_MEMORY% ^
  -Xmx%MEMORY% ^
  -XX:+UseG1GC ^
  -XX:+ParallelRefProcEnabled ^
  -XX:MaxGCPauseMillis=200 ^
  -XX:+UnlockExperimentalVMOptions ^
  -XX:+DisableExplicitGC ^
  -XX:+AlwaysPreTouch ^
  -XX:G1NewSizePercent=30 ^
  -XX:G1MaxNewSizePercent=40 ^
  -XX:G1HeapRegionSize=8M ^
  -XX:G1ReservePercent=20 ^
  -XX:G1HeapWastePercent=5 ^
  -XX:G1MixedGCCountTarget=4 ^
  -XX:InitiatingHeapOccupancyPercent=15 ^
  -XX:G1MixedGCLiveThresholdPercent=90 ^
  -XX:G1RSetUpdatingPauseTimePercent=5 ^
  -XX:SurvivorRatio=32 ^
  -XX:+PerfDisableSharedMem ^
  -XX:MaxTenuringThreshold=1 ^
  "-Xlog:gc*:logs/gc.log:time,uptime:filecount=5,filesize=1M" ^
  -Dusing.aikars.flags=https://mcflags.emc.gs ^
  -Daikars.new.flags=true ^
  -jar "%JAR%" --nogui

endlocal
