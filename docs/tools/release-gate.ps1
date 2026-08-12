# 发布门禁 —— 一条命令跑完所有机械检查。
#
# 存在理由：v0.8.4 原地重发时只跑了 compileJava 与 branch CI，没跑 runGametest，
# 结果一条 required 用例红着上线并且两天无人发现。**只要门禁是一张清单，就一定会有人跳步。**
#
# 用法：
#   powershell -File docs/tools/release-gate.ps1              # 日常门禁，也是发布流程第 1 步
#   powershell -File docs/tools/release-gate.ps1 -Release     # **同步到清洁分支之后**再跑
#
# -Release 的时机不能提前：它要求内部与公开两树的非注释内容**完全等价**，
# 而两次发布之间内部本来就领先公开树（RELEASE_WORKFLOW §5 同步后才可能相等）。
# 在同步前跑 -Release 必然红，那不是缺陷。正确顺序：日常门禁 → §4 保护标签 → §5 同步 → -Release。
#
# 本脚本只覆盖机械检查。真实客户端视觉、交互与远程环境验收仍必须由用户完成，
# 门禁通过不等于节点可以关闭。

param([switch]$Release)

$ErrorActionPreference = "Continue"
$repo = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location $repo

$failures = @()

function Invoke-Step {
    param([string]$Name, [scriptblock]$Body)

    Write-Host ""
    Write-Host ("=" * 66)
    Write-Host "▶ $Name"
    Write-Host ("=" * 66)
    # 先置成失败值：若命令根本没启动（python/gradlew 不在 PATH），PowerShell 不会写
    # $LASTEXITCODE，它会留着**上一步**的 0，于是没跑起来的检查表现成通过。
    $global:LASTEXITCODE = 127
    try {
        & $Body
    } catch {
        Write-Host $_.Exception.Message -ForegroundColor Red
        $global:LASTEXITCODE = 127
    }
    if ($LASTEXITCODE -ne 0) {
        $script:failures += $Name
        $hint = if ($LASTEXITCODE -eq 127) { "（127 = 命令未能启动或抛异常）" } else { "" }
        Write-Host "✗ $Name 失败（退出码 $LASTEXITCODE）$hint" -ForegroundColor Red
    } else {
        Write-Host "✓ $Name" -ForegroundColor Green
    }
}

# 1. 完整构建：含 javac、JUnit，以及注册完整性不变量（RegistrationInvariantTest）。
Invoke-Step "完整构建与单元测试" { & .\gradlew.bat build --no-daemon --console=plain }

# 2. GameTest：唯一能抓住「玩家可见文案改了但断言没改」的门。发布链此前不含它。
Invoke-Step "GameTest" { & .\gradlew.bat runGametest --no-daemon --console=plain }

# 3. 文档 lint：死链、不存在的路径与提交、改名残留、活跃文档份数。
Invoke-Step "文档 lint" { & python docs/tools/doc_lint.py }

# 4. 两树等价：公开分支不得存在内部没有的功能/依赖/构建改动。
#    ⚠️ ref 必须显式传，且要排在开关之前。原来只传 "--release"，而 tree_equiv 是按位置取
#    内部 ref 的，于是那个开关被当成了 ref；git diff 拿到坏 ref 失败、返回空、被读成「零差异」。
#    结果 -Release 这道闸从上线起就没真正跑过（2026-07-29 实证）。
$equivArgs = @("docs/tools/tree_equiv.py", "HEAD", "release/26.1.2-clean")
if ($Release) { $equivArgs += "--release" }
Invoke-Step "内部/公开两树等价审计" { & python $equivArgs }

# 5. git 卫生：未跟踪源文件、备份 ref 类型与命名、中断残留。
Invoke-Step "git 卫生" { & python docs/tools/git_hygiene.py }

# 6. 空白与行尾。
Invoke-Step "git diff --check" { & git diff --check }

# 7. 事实快照：不判定成败，供人工核对文档里的叙述是否仍然成立。
Write-Host ""
Write-Host ("=" * 66)
Write-Host "▶ 当前事实（供人工核对，不参与判定）"
Write-Host ("=" * 66)
& python docs/tools/facts.py

Write-Host ""
Write-Host ("=" * 66)
if ($failures.Count -eq 0) {
    Write-Host "门禁全部通过。" -ForegroundColor Green
    Write-Host "提醒：真实客户端视觉/交互与远程环境验收仍需用户完成。"
    exit 0
}
Write-Host "门禁失败：" -ForegroundColor Red
foreach ($failure in $failures) { Write-Host "  - $failure" -ForegroundColor Red }
exit 1
