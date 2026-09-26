param(
    [switch]$Full
)

$ErrorActionPreference = 'Stop'
$owner = 'MoonLight-sweet'
$repository = 'SpringBootBC'
$branch = 'main'
$apiRoot = "https://api.github.com/repos/$owner/$repository"

$credential = "protocol=https`nhost=github.com`n`n" | git credential fill 2>$null
$tokenLine = $credential | Where-Object { $_ -like 'password=*' } | Select-Object -First 1
if (-not $tokenLine) {
    throw 'Windows Git凭据管理器中没有GitHub登录凭据。'
}
$token = $tokenLine.Substring('password='.Length)
$headers = @{
    Authorization = "Bearer $token"
    Accept = 'application/vnd.github+json'
    'X-GitHub-Api-Version' = '2022-11-28'
    'User-Agent' = 'CodeClinic-Git-Sync'
}

function Invoke-GitHubApi {
    param([string]$Method, [string]$Uri, $Body)
    $params = @{ Method = $Method; Uri = $Uri; Headers = $headers }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json'
        $params.Body = $Body | ConvertTo-Json -Depth 10 -Compress
    }
    Invoke-RestMethod @params
}

$ref = Invoke-GitHubApi GET "$apiRoot/git/ref/heads/$branch" $null
$parentSha = $ref.object.sha
$parentCommit = Invoke-GitHubApi GET "$apiRoot/git/commits/$parentSha" $null

$changes = @()
if ($Full) {
    git -c core.quotepath=false ls-files | ForEach-Object {
        if ($_ -and (Test-Path -LiteralPath $_ -PathType Leaf)) {
            $changes += [pscustomobject]@{ Status = 'M'; Path = $_ }
        }
    }
} else {
    git -c core.quotepath=false diff-tree --no-commit-id --name-status --no-renames -r HEAD | ForEach-Object {
        if ($_ -match '^([AMD])\s+(.+)$') {
            $changes += [pscustomobject]@{ Status = $matches[1]; Path = $matches[2] }
        }
    }
}

if (-not $changes.Count) {
    Write-Output '没有需要同步的文件。'
    exit 0
}

$entries = foreach ($change in $changes) {
    $repoPath = $change.Path.Replace('\', '/')
    if ($change.Status -eq 'D') {
        [ordered]@{ path = $repoPath; mode = '100644'; type = 'blob'; sha = $null }
        continue
    }

    $bytes = [System.IO.File]::ReadAllBytes((Join-Path (Get-Location) $change.Path))
    $blob = Invoke-GitHubApi POST "$apiRoot/git/blobs" @{
        content = [Convert]::ToBase64String($bytes)
        encoding = 'base64'
    }
    $mode = if ($repoPath -eq '.githooks/post-commit') { '100755' } else { '100644' }
    [ordered]@{ path = $repoPath; mode = $mode; type = 'blob'; sha = $blob.sha }
}

$tree = Invoke-GitHubApi POST "$apiRoot/git/trees" @{
    base_tree = $parentCommit.tree.sha
    tree = @($entries)
}
$commit = Invoke-GitHubApi POST "$apiRoot/git/commits" @{
    message = (git log -1 --pretty=%s)
    tree = $tree.sha
    parents = @($parentSha)
}
Invoke-GitHubApi PATCH "$apiRoot/git/refs/heads/$branch" @{
    sha = $commit.sha
    force = $false
} | Out-Null

Write-Output "GitHub同步完成: $($commit.sha)"
