param(
    [int]$PollSeconds = 5,
    [int]$StableSeconds = 20
)

$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$outputDir = Join-Path $projectRoot 'output\auto-sync'
$logFile = Join-Path $outputDir 'auto-sync.log'
$lockFile = Join-Path $outputDir 'watcher.lock'
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
Set-Location -LiteralPath $projectRoot

function Write-SyncLog {
    param([string]$Message)
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Message"
    Add-Content -LiteralPath $logFile -Value $line -Encoding UTF8
}

function Get-WorktreeState {
    return (git -c core.quotepath=false status --porcelain=v1 --untracked-files=all | Out-String).Trim()
}

try {
    if (Test-Path -LiteralPath $lockFile) {
        $existingPid = Get-Content -LiteralPath $lockFile -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($existingPid -and (Get-Process -Id $existingPid -ErrorAction SilentlyContinue)) {
            Write-SyncLog "监控程序已在运行，PID=$existingPid"
            exit 0
        }
    }
    Set-Content -LiteralPath $lockFile -Value $PID -Encoding ASCII
    Write-SyncLog "自动提交监控已启动，PID=$PID"

    $lastState = Get-WorktreeState
    $stableSince = Get-Date
    while ($true) {
        Start-Sleep -Seconds $PollSeconds
        $currentState = Get-WorktreeState
        if ($currentState -ne $lastState) {
            $lastState = $currentState
            $stableSince = Get-Date
            continue
        }
        if (-not $currentState) {
            continue
        }
        if (((Get-Date) - $stableSince).TotalSeconds -lt $StableSeconds) {
            continue
        }

        Write-SyncLog '检测到稳定修改，开始执行测试。'
        & mvn -q test *>> $logFile
        if ($LASTEXITCODE -ne 0) {
            Write-SyncLog '测试失败，本次不提交；文件再次变化后会重新测试。'
            $stableSince = (Get-Date).AddYears(1)
            continue
        }

        $verifiedState = Get-WorktreeState
        if ($verifiedState -ne $currentState) {
            Write-SyncLog '测试期间文件再次变化，等待下一次稳定后重试。'
            $lastState = $verifiedState
            $stableSince = Get-Date
            continue
        }

        git add -A
        if ($LASTEXITCODE -ne 0) {
            Write-SyncLog '暂存修改失败。'
            $stableSince = (Get-Date).AddMinutes(1)
            continue
        }
        $message = "chore: auto sync $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        git commit -m $message *>> $logFile
        if ($LASTEXITCODE -ne 0) {
            Write-SyncLog '自动提交失败。'
            $stableSince = (Get-Date).AddMinutes(1)
            continue
        }
        Write-SyncLog "已提交并触发GitHub推送：$message"
        $lastState = Get-WorktreeState
        $stableSince = Get-Date
    }
} catch {
    Write-SyncLog "监控异常：$($_.Exception.Message)"
    throw
} finally {
    if (Test-Path -LiteralPath $lockFile) {
        $lockPid = Get-Content -LiteralPath $lockFile -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($lockPid -eq $PID) {
            Remove-Item -LiteralPath $lockFile -Force
        }
    }
}
