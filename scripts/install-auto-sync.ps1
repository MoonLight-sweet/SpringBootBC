$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$watcher = Join-Path $projectRoot 'scripts\auto-commit-watch.ps1'
$taskName = 'CodeClinic-GitHub-AutoSync'
$powerShell = (Get-Command powershell.exe).Source
$arguments = "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$watcher`""

$action = New-ScheduledTaskAction -Execute $powerShell -Argument $arguments -WorkingDirectory $projectRoot
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$settings = New-ScheduledTaskSettingsSet -MultipleInstances IgnoreNew -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1) -ExecutionTimeLimit ([TimeSpan]::Zero)
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -Description '检测CodeClinic项目修改，通过测试后自动提交并推送到GitHub。' -Force | Out-Null
Start-ScheduledTask -TaskName $taskName
Write-Output "自动同步任务已安装并启动：$taskName"
