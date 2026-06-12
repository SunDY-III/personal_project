@echo off
REM === SmartTicket VS Code 补全缺失扩展 ===
echo 已安装：Java 全家桶、Maven、Gradle、Docker、Claude Code
echo 即将安装 7 个缺失扩展...
echo.

code --install-extension vmware.vscode-spring-boot
code --install-extension vscjava.vscode-spring-boot-dashboard
code --install-extension ms-vscode-remote.remote-wsl
code --install-extension eamodio.gitlens
code --install-extension redhat.vscode-xml
code --install-extension redhat.vscode-yaml
code --install-extension ms-ossdata.vscode-postgresql

echo.
echo 全部安装完成！
echo 关闭 VS Code 重新打开，然后双击桌面的 smartticket.code-workspace
pause
