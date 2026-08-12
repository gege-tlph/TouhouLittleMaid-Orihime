# ---------------------------------------------------------------------------
# Dev-environment cycle rig.
#
# Sets a mod combination, restarts the dedicated server and the test client,
# auto-joins, and reports the outcome plus the payload channels the client
# decoded. Built 2026-07-29 while isolating the Refurbished join blocker, where
# the answer needed a 2x2 over two mods and each cell is a full restart.
#
#   powershell -File docs/tools/cycle.ps1 -Enable framework,refurbished -Disable openysm
#   powershell -File docs/tools/cycle.ps1 -Enable openysm -Disable framework,refurbished
#   powershell -File docs/tools/cycle.ps1 -Enable refurbished -SkipClient   # server only
#
# -Enable / -Disable take substrings of the jar names in run/mods and
# run-dedicated-client/mods; a disabled jar is renamed to *.jar.disabled, which
# Fabric ignores, so nothing is ever deleted.
#
# Three things this rig exists to prevent, all learned the hard way:
#
#   1. Silent spinning. Every wait loop has a stall detector -- if the log stops
#      growing for N polls it gives up and says STALLED. Listing the failure
#      strings you can think of is not enough; unknown failure shapes look
#      exactly like "still working" without this.
#   2. Reading the previous run's verdict. State is taken from latest.log, which
#      MC truncates on start. debug.log keeps the old text around and reported a
#      stale DISCONNECTED twice before this was fixed.
#   3. Changing two variables at once. Pass one -Enable/-Disable change per run;
#      a combined change produced a brand new failure that could not be
#      attributed and cost an extra round.
#
# Note that "the client joined" is a FLAKY signal for anything protocol-related:
# a client sometimes joins even with a broken registry mapping, because the
# offending packet is not always sent. Measure the mechanism, not the symptom.
#
# ASCII only on purpose: files written without a BOM are decoded as ANSI by
# Windows PowerShell 5.1, so non-ASCII here would produce bogus syntax errors.
# ---------------------------------------------------------------------------
param(
    [string[]]$Enable = @(),
    [string[]]$Disable = @(),
    [switch]$SkipClient
)

$ErrorActionPreference = 'Continue'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $repo

$modDirs = @('run\mods', 'run-dedicated-client\mods')

# Only ever stops processes provable to be this repo's, by command line.
function Stop-RepoGame {
    $procs = Get-CimInstance Win32_Process -Filter "Name='java.exe' or Name='javaw.exe'" |
        Where-Object { $_.CommandLine -like "*$repo*" -and
                      ($_.CommandLine -like '*dli.env=*' -or $_.CommandLine -like '*runServer*' -or $_.CommandLine -like '*runClientDedicated*') }
    foreach ($p in $procs) { try { Stop-Process -Id $p.ProcessId -Force -ErrorAction Stop } catch {} }
    Start-Sleep -Seconds 5
}

function Set-Mod([string]$pattern, [bool]$on) {
    foreach ($d in $modDirs) {
        foreach ($f in Get-ChildItem $d -Filter "*$pattern*" -ErrorAction SilentlyContinue) {
            $isDisabled = $f.Name.EndsWith('.disabled')
            if ($on -and $isDisabled) {
                Move-Item $f.FullName ($f.FullName -replace '\.disabled$','') -Force
            } elseif (-not $on -and -not $isDisabled) {
                Move-Item $f.FullName ($f.FullName + '.disabled') -Force
            }
        }
    }
}

function Wait-For([string]$log, [string]$ready, [string]$fail, [int]$minutes, [int]$stallLimit) {
    $deadline = (Get-Date).AddMinutes($minutes); $last = -1; $stall = 0
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 5
        $txt = if (Test-Path $log) { Get-Content $log -Raw -ErrorAction SilentlyContinue } else { '' }
        if ($fail -and $txt -match $fail) { return 'FAILED' }
        if ($txt -match $ready) { return 'READY' }
        $size = if (Test-Path $log) { (Get-Item $log).Length } else { 0 }
        if ($size -eq $last) { $stall++ } else { $stall = 0; $last = $size }
        if ($stall -ge $stallLimit) { return 'STALLED' }
    }
    return 'TIMEOUT'
}

Stop-RepoGame
foreach ($m in $Enable)  { Set-Mod $m $true }
foreach ($m in $Disable) { Set-Mod $m $false }

Write-Output '--- mod state ---'
Get-ChildItem 'run\mods' | Where-Object { $_.Name -match 'framework|refurb|ysm' } |
    ForEach-Object { '  ' + $_.Name }

# Delete the previous latest.log before launching. MC truncates it on start, which is
# what the "don't read the previous run's verdict" fix relied on -- but a server that
# never starts truncates nothing, so the stale "Done (...)" line is read as READY.
# That happened on 2026-07-30: SERVER=READY and CLIENT=JOINED were both reported while
# no server process existed at all. Deleting the file makes "never started" indistinguishable
# from "no progress" instead of indistinguishable from "succeeded".
Remove-Item 'run\logs\latest.log' -ErrorAction SilentlyContinue

Start-Process -FilePath '.\gradlew.bat' -ArgumentList 'runServer' -WorkingDirectory $repo -WindowStyle Minimized | Out-Null
$s = Wait-For 'run\logs\latest.log' 'Done \(.*\)! For help' 'Failed to start the minecraft server' 6 15
Write-Output "SERVER=$s"
if ($s -ne 'READY' -or $SkipClient) { exit }

# --args must be one quoted token; PowerShell otherwise splits it on spaces and
# gradle reads the pieces as task names ("Task 'TlmTester' not found").
$argline = '--args="--username TlmTester --quickPlayMultiplayer 127.0.0.1:25565"'

$clog = 'run-dedicated-client\logs\latest.log'
# Same reason as the server log above, and it must happen BEFORE the launch -- deleting
# it afterwards races the client's own first write.
Remove-Item $clog -ErrorAction SilentlyContinue

Start-Process -FilePath '.\gradlew.bat' -ArgumentList 'runClientDedicated', $argline -WorkingDirectory $repo -WindowStyle Minimized | Out-Null
$deadline = (Get-Date).AddMinutes(8); $last = -1; $stall = 0; $state = 'PENDING'
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 5
    $ctxt = if (Test-Path $clog) { Get-Content $clog -Raw -ErrorAction SilentlyContinue } else { '' }
    $stxt = if (Test-Path 'run\logs\latest.log') { Get-Content 'run\logs\latest.log' -Raw -ErrorAction SilentlyContinue } else { '' }
    if ($ctxt -match 'Client disconnected with reason') { $state = 'DISCONNECTED'; break }
    if ($ctxt -match 'emergencySaveAndCrash')            { $state = 'CRASHED'; break }
    if ($stxt -match 'lost connection')                  { $state = 'LOST'; break }
    if ($stxt -match 'joined the game')                  { $state = 'JOINED'; break }
    $size = if (Test-Path $clog) { (Get-Item $clog).Length } else { 0 }
    if ($size -eq $last) { $stall++ } else { $stall = 0; $last = $size }
    if ($stall -ge 22) { $state = 'STALLED'; break }
}
Write-Output "CLIENT=$state"

Write-Output '--- channels decoded (in order) ---'
$seen = @()
Select-String -Path $clog -Pattern 'channel with name "([^"]+)"' -AllMatches | ForEach-Object {
    foreach ($m in $_.Matches) {
        $n = $m.Groups[1].Value
        if ($seen.Count -eq 0 -or $seen[-1] -ne $n) { $seen += $n; '  ' + $n }
    }
}
