@echo off
echo 执行命令 java -jar arcade.jar 300 6
pushd %~dp0
cmd /k java -jar arcade.jar 300 6
pause