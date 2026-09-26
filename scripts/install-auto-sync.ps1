$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$watcher = Join-Path $projectRoot 'scripts\auto-commit-watch.ps1'
$taskName = 'CodeClinic-GitHub-AutoSync'
$powerShell = (Get-Command powershell.exe).Source
$arguments = "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$watcher`""

$action = New-ScheduledTaskAction -Execute $powerShell -Argument $arguments -WorkingDirectory $projectRoot
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$settings = New-ScheduledTaskSettingsSet -MultipleInstances IgnoreNew -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1) -ExecutionTimeLimit ([TimeSpan]::Zero)
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -Description 'Test stable CodeClinic changes, then commit and push them to GitHub.' -Force | Out-Null
Start-ScheduledTask -TaskName $taskName
Write-Output "Auto-sync task installed and started: $taskName"
