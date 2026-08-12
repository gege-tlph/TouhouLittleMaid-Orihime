<#
    mcp-up.ps1 —— 在开新 Claude Code 会话之前，把 MCP 依赖的进程全部拉起并等到真正可连。

    为什么需要它：
      HTTP 类 MCP（minecraft-java 8765 / minecraft-java-client 8766）在 Claude Code 进程
      启动后约 1.3 秒发起连接，1 次首连 + 3 次重试（退避约 0.5/1.5/4 秒），总窗口只有
      ~6 秒，之后就把 server 标记为失败。MC 启动要几分钟，差两个数量级。
      所以「先开会话、再开服」= 整场会话都没有这两个 server。

      2026-07-29 探针实测修正了此处两处旧说法：
        · 「不会重试」——错，有 3 次重试，只是窗口才 6 秒，结论不变；
        · 「没有任何报错」——错，连接失败现在会经 ToolSearch 结果报给模型，不再全静默。
      同轮还实测：阻塞式 SessionStart hook **挡不住**，它比首连还晚 0.46 秒触发，
      hook 睡满 12 秒结束后一次连接都没有。补救只能靠 /mcp 面板手动重连，或本脚本。

    用法：
      powershell -File docs/tools/mcp-up.ps1              # 只起专服（8765）
      powershell -File docs/tools/mcp-up.ps1 -WithClient  # 专服 + 游戏客户端（8766）
      powershell -File docs/tools/mcp-up.ps1 -CheckOnly   # 不启动，只体检当前四个 server

    脚本全绿之后再开 Claude Code 会话。
#>
[CmdletBinding()]
param(
    [switch]$WithClient,
    [switch]$CheckOnly,
    [int]$TimeoutSeconds = 300
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$repo = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$gradlew = Join-Path $repo 'gradlew.bat'
$failures = New-Object System.Collections.Generic.List[string]

function Write-Step($text) { Write-Host "`n== $text" -ForegroundColor Cyan }
function Write-Ok($text)   { Write-Host "  [OK]   $text" -ForegroundColor Green }
function Write-Bad($text)  { Write-Host "  [FAIL] $text" -ForegroundColor Red; $script:failures.Add($text) }
function Write-Skip($text) { Write-Host "  [--]   $text" -ForegroundColor DarkGray }

# ---------------------------------------------------------------- HTTP MCP 探针
# 只有 initialize 握手成功才算「可连」。端口在听不等于 MCP 起来了：
# Minecraft 先绑端口、后注册 MCP handler，中间有几十秒的窗口期。
function Test-McpEndpoint([int]$Port) {
    $body = '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"mcp-up","version":"1"}}}'
    try {
        $r = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/mcp" -Method POST `
            -ContentType 'application/json' `
            -Headers @{ Accept = 'application/json, text/event-stream' } `
            -Body $body -TimeoutSec 10 -UseBasicParsing
        if ($r.StatusCode -ne 200) { return $null }
        $payload = $r.Content
        if ($payload -match '(?m)^data:\s*(.+)$') { $payload = $Matches[1] }
        $json = $payload | ConvertFrom-Json
        if ($null -eq $json.result) { return $null }
        return $json.result.serverInfo.name
    } catch {
        return $null
    }
}

function Wait-ForMcp([int]$Port, [string]$Label) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $name = Test-McpEndpoint -Port $Port
        if ($name) {
            Write-Ok "$Label ($Port) 握手成功 -> $name"
            return $true
        }
        Start-Sleep -Seconds 3
    }
    Write-Bad "$Label ($Port) 在 ${TimeoutSeconds}s 内没有完成 MCP 握手"
    return $false
}

# ---------------------------------------------------------------- 端口归属
# 「握手不通」不等于「没人在用」。两种情况都会长这样：
#   · 世界还在加载 —— 端口已 LISTEN 但 handler 未注册（先绑端口、后注册）；
#   · 别的会话（并行 Claude / Codex）正在跑测试。
# 2026-07-29 实测：8765/8766 被 npct\MaidRestaurant 的 runClient 占着，世界里同样有
# touhou_little_maid:maid 实体，很容易误以为验的是本仓库的构建。此时再 Start-Process
# 会抢端口、打断别人的测试。所以启动前查的是「端口有没有人占」，不是「握手通没通」。
function Get-PortOwner([int]$Port) {
    $c = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
         Select-Object -First 1
    if (-not $c) { return $null }
    $cmd = (Get-CimInstance Win32_Process -Filter "ProcessId=$($c.OwningProcess)" `
            -ErrorAction SilentlyContinue).CommandLine
    $hint = $null
    if ($cmd) {
        $hint = ($cmd -split '\s+' | Where-Object { $_ -match 'argFiles|gameDir' } | Select-Object -First 1)
    }
    return [PSCustomObject]@{
        OwnerPid = $c.OwningProcess
        Mine     = ($cmd -and $cmd -like "*$repo*")
        Hint     = $hint
    }
}

# 握手通了之后调用：确认在听的那个实例是不是本仓库的。
function Show-PortOwner([int]$Port) {
    $o = Get-PortOwner $Port
    if (-not $o) { return }
    if ($o.Mine) {
        Write-Host "         owner: PID $($o.OwnerPid) — 本仓库" -ForegroundColor DarkGray
    } else {
        Write-Host "         [WARN] 在听的实例不是本仓库！PID $($o.OwnerPid)" -ForegroundColor Yellow
        if ($o.Hint) { Write-Host "                $($o.Hint)" -ForegroundColor Yellow }
        Write-Host '                用它验出来的行为不代表本分支的构建。' -ForegroundColor Yellow
    }
}

# 启动前调用：端口被占就绝不启动第二个实例。
function Test-CanLaunch([int]$Port, [string]$Label) {
    $o = Get-PortOwner $Port
    if (-not $o) { return $true }
    Write-Bad "$Label ($Port) 端口已被 PID $($o.OwnerPid) 占用但握手不通 —— 不启动第二个实例"
    if ($o.Hint) { Write-Host "         $($o.Hint)" -ForegroundColor Yellow }
    Write-Host '         可能是世界还在加载，也可能是别的会话正在用。等它，不要去杀。' -ForegroundColor Yellow
    return $false
}

# ---------------------------------------------------------------- 1. 专服 8765
Write-Step '专服 minecraft-java (8765)'
if (Test-McpEndpoint -Port 8765) {
    Write-Ok '已在运行，跳过启动'
    Show-PortOwner 8765
} elseif ($CheckOnly) {
    Write-Bad '专服 minecraft-java (8765) 未运行（-CheckOnly 不会启动它）'
} elseif (Test-CanLaunch -Port 8765 -Label '专服 minecraft-java') {
    Write-Host '  启动 runServer（独立窗口，本脚本不会杀它）...' -ForegroundColor DarkGray
    Start-Process -FilePath $gradlew -ArgumentList 'runServer' -WorkingDirectory $repo | Out-Null
    if (Wait-ForMcp -Port 8765 -Label '专服 minecraft-java') { Show-PortOwner 8765 }
}

# ---------------------------------------------------------------- 2. 客户端 8766
Write-Step '游戏客户端 minecraft-java-client (8766)'
if (Test-McpEndpoint -Port 8766) {
    Write-Ok '已在运行，跳过启动'
    Show-PortOwner 8766
} elseif ($CheckOnly) {
    Write-Bad '客户端 minecraft-java-client (8766) 未运行（-CheckOnly 不会启动它）'
} elseif (-not $WithClient) {
    Write-Skip '未指定 -WithClient，本轮不需要客户端只读工具'
} elseif (-not (Test-CanLaunch -Port 8766 -Label '客户端 minecraft-java-client')) {
    # Test-CanLaunch 已记入 failures，这里什么都不做
} else {
    # 必须用 runClientDedicated 而不是 runClient：后者 runDir 也是 run/，与正在跑的
    # 服务器抢 run/.fabric/processedMods/*.jar，客户端重映射 mod 时删不掉直接启动失败。
    Write-Host '  启动 runClientDedicated（独立窗口，用完请自行退出）...' -ForegroundColor DarkGray
    Start-Process -FilePath $gradlew -ArgumentList 'runClientDedicated' -WorkingDirectory $repo | Out-Null
    Wait-ForMcp -Port 8766 -Label '客户端 minecraft-java-client' | Out-Null
}

# ---------------------------------------------------------------- 3. npx 类 server
# 这两个是 stdio server，Claude Code 每次会话自己拉起。这里只验证它们「能被拉起」，
# 因为失败形态是进程启动即崩，在会话里表现为工具静默缺失，不会有任何提示。
Write-Step 'npx 类 server（stdio，验证可拉起）'
foreach ($pkg in @('@adhisang/minecraft-modding-mcp', 'mcmodding-mcp')) {
    $job = Start-Job -ScriptBlock {
        param($p)
        # 只要进程能活过冷启动、不立刻抛栈，就算可拉起
        $ErrorActionPreference = 'Continue'
        npx -y $p 2>&1
    } -ArgumentList $pkg

    $done = Wait-Job $job -Timeout 90
    $output = (Receive-Job $job -ErrorAction SilentlyContinue) -join "`n"
    Stop-Job $job -ErrorAction SilentlyContinue
    Remove-Job $job -Force -ErrorAction SilentlyContinue

    if ($output -match 'Cannot find module|Something went wrong installing|npm ERR!|ERR_MODULE_NOT_FOUND') {
        $first = ($output -split "`n" |
            Where-Object { $_ -match 'Cannot find module|Something went wrong installing|npm ERR!' } |
            Select-Object -First 1).Trim()
        Write-Bad "$pkg 启动即失败：$first"
    } elseif ($done -and $job.State -eq 'Completed' -and [string]::IsNullOrWhiteSpace($output)) {
        # 立刻退出且无输出 = 不是一个长驻 stdio server
        Write-Bad "$pkg 立即退出且无输出，不像可用的 stdio server"
    } else {
        Write-Ok "$pkg 可拉起"
    }
}

# ---------------------------------------------------------------- 汇总
Write-Host ''
if ($failures.Count -eq 0) {
    Write-Host '全部就绪 —— 现在可以开新的 Claude Code 会话了。' -ForegroundColor Green
    exit 0
}

Write-Host "有 $($failures.Count) 项未就绪：" -ForegroundColor Red
foreach ($f in $failures) { Write-Host "  - $f" -ForegroundColor Red }
Write-Host ''
Write-Host '未就绪的 server 在会话里不会报错，只会静默消失。修好再开会话。' -ForegroundColor Yellow
exit 1
