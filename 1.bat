@echo off
cd /d "%~dp0"
java -cp "target\Shimeji-ee.jar;lib\*;conf" com.group_finity.mascot.Main
pause