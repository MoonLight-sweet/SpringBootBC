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

function Sync-PendingCommits {
    $branch = (git symbolic-ref --quiet --short HEAD 2>$null | Out-String).Trim()
    if (-not $branch) {
        return
    }
    git fetch origin $branch *>> $logFile
    if ($LASTEXITCODE -ne 0) {
        Write-SyncLog 'Could not refresh the remote branch. Push will be retried later.'
        return
    }
    $counts = (git rev-list --left-right --count "HEAD...origin/$branch" | Out-String).Trim() -split '\s+'
    if ($counts.Count -lt 2) {
        return
    }
    $ahead = [int]$counts[0]
    $behind = [int]$counts[1]
    if ($behind -gt 0) {
        Write-SyncLog "Remote branch is ahead by $behind commit(s); automatic push paused."
        return
    }
    if ($ahead -gt 0) {
        git push origin $branch *>> $logFile
        if ($LASTEXITCODE -eq 0) {
            Write-SyncLog "Pushed $ahead pending commit(s) to GitHub."
        } else {
            Write-SyncLog 'Pending push failed and will be retried in one minute.'
        }
    }
}

try {
    if (Test-Path -LiteralPath $lockFile) {
        $existingPid = Get-Content -LiteralPath $lockFile -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($existingPid -and (Get-Process -Id $existingPid -ErrorAction SilentlyContinue)) {
            Write-SyncLog "Watcher is already running. PID=$existingPid"
            exit 0
        }
    }
    Set-Content -LiteralPath $lockFile -Value $PID -Encoding ASCII
    Write-SyncLog "Auto-sync watcher started. PID=$PID"

    $lastState = Get-WorktreeState
    $stableSince = Get-Date
    $nextPushAttempt = Get-Date
    while ($true) {
        Start-Sleep -Seconds $PollSeconds
        if ((Get-Date) -ge $nextPushAttempt) {
            Sync-PendingCommits
            $nextPushAttempt = (Get-Date).AddMinutes(1)
        }
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

        Write-SyncLog 'Stable changes detected. Running tests.'
        & mvn -q test *>> $logFile
        if ($LASTEXITCODE -ne 0) {
            Write-SyncLog 'Tests failed. Commit skipped until files change again.'
            $stableSince = (Get-Date).AddYears(1)
            continue
        }

        $verifiedState = Get-WorktreeState
        if ($verifiedState -ne $currentState) {
            Write-SyncLog 'Files changed during testing. Waiting for a stable state.'
            $lastState = $verifiedState
            $stableSince = Get-Date
            continue
        }

        git add -A
        if ($LASTEXITCODE -ne 0) {
            Write-SyncLog 'Failed to stage changes.'
            $stableSince = (Get-Date).AddMinutes(1)
            continue
        }
        $message = "chore: auto sync $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        git commit -m $message *>> $logFile
        if ($LASTEXITCODE -ne 0) {
            Write-SyncLog 'Automatic commit failed.'
            $stableSince = (Get-Date).AddMinutes(1)
            continue
        }
        Write-SyncLog "Committed and triggered GitHub push: $message"
        $lastState = Get-WorktreeState
        $stableSince = Get-Date
    }
} catch {
    Write-SyncLog "Watcher error: $($_.Exception.Message)"
    throw
} finally {
    if (Test-Path -LiteralPath $lockFile) {
        $lockPid = Get-Content -LiteralPath $lockFile -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($lockPid -eq $PID) {
            Remove-Item -LiteralPath $lockFile -Force
        }
    }
}
