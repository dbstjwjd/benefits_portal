<#
.SYNOPSIS
    IDIS 재배포. 빌드 → 업로드 → 교체 → 기동 확인까지 한 번에 한다.

.DESCRIPTION
    로컬에서 JAR 을 만들어 서버에 올리고 서비스를 갈아끼운다.
    바꾸기 전 jar 는 idis.jar.bak 으로 남으므로 되돌릴 수 있다.

    ★ 스키마가 바뀐 배포에는 쓰지 말 것 ★

    운영은 ddl-auto=validate 라 엔티티와 테이블이 다르면 애플리케이션이 아예 뜨지 않는다.
    이 스크립트는 DB 를 건드리지 않으므로, 아래에 해당하면 먼저 SQL 을 넣고 나서 실행한다.

      - 엔티티에 필드를 추가·삭제했다          → ALTER TABLE ... ADD/DROP COLUMN
      - enum 에 값을 추가했다(질문 타입 등)     → ALTER TABLE ... MODIFY ... enum(...)
      - 테이블을 새로 만들었다                  → CREATE TABLE

    정확한 문장은 deploy/setup.md 의 '다시 배포할 때' 를 본다.
    스키마가 그대로인 배포(화면·로직만 수정)면 이 스크립트로 끝난다.

.EXAMPLE
    .\deploy\deploy.ps1
    .\deploy\deploy.ps1 -Yes          # 확인 프롬프트 없이
    .\deploy\deploy.ps1 -SkipBuild    # 이미 만든 jar 를 그대로 올릴 때
#>

[CmdletBinding()]
param(
    # 스키마 확인 프롬프트를 건너뛴다
    [switch]$Yes,
    # gradlew clean bootJar 를 건너뛰고 기존 jar 를 올린다
    [switch]$SkipBuild
)

# ══════════════════════════════════════════════════════════════
#  여기부터 채운다
# ══════════════════════════════════════════════════════════════

# 서버 주소 (Lightsail 고정 IP 또는 도메인)
$ServerHost = '15.164.128.111'

# SSH 접속 계정. Lightsail Ubuntu 는 기본이 ubuntu
$SshUser = 'ubuntu'

# 개인키 경로. 공백이 있어도 그대로 두면 된다
$KeyPath    = 'C:\Users\IPC 이승호\Desktop\개발\private key\idis.pem'

# ── 아래는 서버 구성을 바꾸지 않았다면 그대로 둔다 ──────────────

$ServiceName  = 'idis'
$RemoteDir    = '/opt/idis'
$RemoteJar    = "$RemoteDir/idis.jar"
$RemoteBackup = "$RemoteDir/idis.jar.bak"
$RemoteOwner  = 'idis:idis'
$HealthUrl    = 'http://127.0.0.1:8080/login'
$HealthWaitSeconds = 90

# ══════════════════════════════════════════════════════════════
#  여기부터는 건드릴 일이 없다
# ══════════════════════════════════════════════════════════════

$ErrorActionPreference = 'Stop'

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$LocalJar    = Join-Path $ProjectRoot 'build\libs\idis.jar'
$Gradlew     = Join-Path $ProjectRoot 'gradlew.bat'

function Write-Step  { param($m) Write-Host ""; Write-Host "== $m" -ForegroundColor Cyan }
function Write-Ok    { param($m) Write-Host "   $m" -ForegroundColor Green }
function Write-Warn2 { param($m) Write-Host "   $m" -ForegroundColor Yellow }
function Write-Err   { param($m) Write-Host "   $m" -ForegroundColor Red }

function Stop-WithError {
    param($Message)
    Write-Host ""
    Write-Err $Message
    exit 1
}

<#
    ssh/scp/gradlew 처럼 밖에서 돌리는 명령용.

    Windows PowerShell 5.1 은 ErrorActionPreference 가 Stop 이면
    네이티브 명령이 stderr 에 한 줄만 써도 NativeCommandError 로 끊어 버린다.
    그러면 아래 종료 코드 검사까지 못 오고 PowerShell 스택이 그대로 튀어나온다.
    그래서 호출하는 동안만 Continue 로 낮춘다.
#>
function Invoke-Native {
    param(
        [Parameter(Mandatory)][scriptblock]$Command,
        [string]$What,
        [switch]$PassThruExitCode
    )
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        # Out-Host 로 흘려보낸다. 그냥 두면 명령 출력이 함수 반환값에 섞여
        # 종료 코드를 받으려던 변수가 '출력 + 코드' 배열이 되어 버린다.
        & $Command | Out-Host
    } finally {
        $ErrorActionPreference = $previous
    }
    $code = $LASTEXITCODE
    if ($PassThruExitCode) {
        return $code
    }
    if ($code -ne 0) {
        Stop-WithError "$What 실패 (종료 코드 $code)"
    }
}

# ── 0. 사전 점검 ──────────────────────────────────────────────

Write-Step "사전 점검"

if ($ServerHost -like '*<*') {
    Stop-WithError "스크립트 상단의 `$ServerHost 를 실제 값으로 바꿔주세요."
}
foreach ($cmd in @('ssh', 'scp')) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
        Stop-WithError "$cmd 를 찾을 수 없습니다. 설정 > 앱 > 선택적 기능에서 'OpenSSH 클라이언트'를 설치해주세요."
    }
}
if (-not (Test-Path $KeyPath)) {
    Stop-WithError "개인키가 없습니다: $KeyPath"
}
if (-not $SkipBuild -and -not (Test-Path $Gradlew)) {
    Stop-WithError "gradlew.bat 을 찾을 수 없습니다: $Gradlew"
}
Write-Ok "ssh/scp, 개인키 확인"

# 키 파일에 넓은 그룹 권한이 남아 있으면 OpenSSH 가 접속을 거부한다
$broad = (Get-Acl $KeyPath).Access | Where-Object {
    $_.IdentityReference.Value -match 'Everyone|Users|Authenticated Users|모든 사용자|사용자'
}
if ($broad) {
    Write-Warn2 "개인키 권한이 넓습니다. OpenSSH 가 거부하면 아래를 실행하세요:"
    Write-Warn2 "  icacls `"$KeyPath`" /inheritance:r /grant:r `"$env:USERNAME`:R`""
}

# ── 1. 스키마 경고 ────────────────────────────────────────────

if (-not $Yes) {
    Write-Host ""
    Write-Host "  ────────────────────────────────────────────────────" -ForegroundColor Yellow
    Write-Host "   이 스크립트는 DB 를 건드리지 않습니다." -ForegroundColor Yellow
    Write-Host "   운영은 ddl-auto=validate 라 스키마가 안 맞으면 앱이 뜨지 않습니다." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "   엔티티 필드 추가·삭제 / enum 값 추가 / 테이블 신설이 있었다면" -ForegroundColor Yellow
    Write-Host "   먼저 서버 DB 에 ALTER 를 넣고 오세요. (deploy/setup.md 참고)" -ForegroundColor Yellow
    Write-Host "  ────────────────────────────────────────────────────" -ForegroundColor Yellow
    Write-Host ""
    $answer = Read-Host "   스키마 변경이 없는 배포입니까? (y/N)"
    if ($answer -ne 'y' -and $answer -ne 'Y') {
        Write-Host ""
        Write-Host "   중단했습니다. 스키마를 먼저 반영한 뒤 다시 실행하세요." -ForegroundColor Yellow
        exit 0
    }
}

# ── 2. 빌드 ───────────────────────────────────────────────────

if ($SkipBuild) {
    Write-Step "빌드 건너뜀 (-SkipBuild)"
    if (-not (Test-Path $LocalJar)) {
        Stop-WithError "올릴 jar 가 없습니다: $LocalJar"
    }
} else {
    Write-Step "빌드 (gradlew clean bootJar)"
    Push-Location $ProjectRoot
    try {
        Invoke-Native -What "빌드" -Command { & $Gradlew clean bootJar --console=plain }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $LocalJar)) {
    Stop-WithError "빌드는 끝났는데 jar 가 없습니다: $LocalJar"
}
$jarInfo = Get-Item $LocalJar
$jarMb = [math]::Round($jarInfo.Length / 1MB, 1)
Write-Ok "$($jarInfo.Name)  ${jarMb}MB  $($jarInfo.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss'))"

# 로컬 설정이 섞여 들어가지 않았는지 마지막으로 본다
$leaked = & { Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($LocalJar)
    try { $zip.Entries | Where-Object { $_.FullName -eq 'BOOT-INF/classes/application-local.properties' } }
    finally { $zip.Dispose() } }
if ($leaked) {
    Stop-WithError "jar 안에 application-local.properties 가 들어 있습니다. build.gradle 의 bootJar exclude 를 확인하세요."
}
Write-Ok "로컬 설정 미포함 확인"

# ── 3. 업로드 ─────────────────────────────────────────────────

$sshOpts = @(
    '-i', $KeyPath,
    '-o', 'StrictHostKeyChecking=accept-new',
    '-o', 'ConnectTimeout=15'
)
$target = "$SshUser@$ServerHost"

Write-Step "업로드 → ${target}:/tmp/idis.jar"
Invoke-Native -What "업로드" -Command { & scp @sshOpts $LocalJar "${target}:/tmp/idis.jar" }
Write-Ok "완료"

# ── 4. 교체 + 기동 ────────────────────────────────────────────
#
# 원격에서 돌릴 스크립트. 한글을 넣으면 콘솔 인코딩에 따라 깨질 수 있어
# 서버 쪽 출력은 영문으로만 둔다.

# 값만 PowerShell 이 채우고, 본문은 리터럴 here-string 이라 bash 문법을 그대로 쓴다.
# (@"..."@ 안에서는 $ 와 백틱을 PowerShell 이 먼저 해석해 버린다)
$remoteHeader = @"
SVC='$ServiceName'
JAR='$RemoteJar'
BAK='$RemoteBackup'
OWNER='$RemoteOwner'
URL='$HealthUrl'
WAIT='$HealthWaitSeconds'
"@

# 서버 쪽 출력은 콘솔 인코딩을 타지 않도록 영문으로만 둔다
$remoteBody = @'
set -eu

test -f /tmp/idis.jar || { echo 'ERROR: /tmp/idis.jar not found'; exit 1; }

echo '-- stopping'
sudo systemctl stop "$SVC"

if [ -f "$JAR" ]; then
  echo "-- backing up to $BAK"
  sudo cp -f "$JAR" "$BAK"
fi

echo '-- replacing'
sudo mv /tmp/idis.jar "$JAR"
sudo chown "$OWNER" "$JAR"
sudo chmod 644 "$JAR"

echo '-- starting'
sudo systemctl start "$SVC"

echo "-- waiting for $URL"
ok=0
i=0
while [ "$i" -lt "$WAIT" ]; do
  i=$((i + 1))
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "$URL" || true)
  if [ "$code" = "200" ]; then ok=1; echo "-- healthy after ${i}s"; break; fi
  if ! systemctl is-active --quiet "$SVC"; then echo '-- service is not running'; break; fi
  sleep 1
done

if [ "$ok" != "1" ]; then
  echo '=== HEALTH CHECK FAILED ==='
  echo '--- systemctl status ---'
  sudo systemctl status "$SVC" --no-pager -l || true
  echo '--- journalctl (last 60 lines) ---'
  sudo journalctl -u "$SVC" -n 60 --no-pager || true
  exit 1
fi

echo '-- deployed jar:'
ls -l "$JAR"
'@

# 이 파일이 CRLF 로 저장돼 있으면 here-string 내용에도 캐리지 리턴이 섞인다.
# 그대로 보내면 원격 bash 가 첫 줄부터 깨지므로 반드시 걷어낸다.
$remoteScript = ($remoteHeader + "`n" + $remoteBody) -replace "`r", ""

<#
    스크립트를 ssh 로 파이프하면 PowerShell 이 UTF-8 BOM 을 앞에 붙여 보내
    원격 bash 가 첫 줄을 "﻿SVC=... : command not found" 로 읽는다.
    base64 로 실어 보내면 인코딩·따옴표 문제가 한 번에 사라진다.
#>
$remoteBase64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($remoteScript))

Write-Step "서버 교체 + 기동 확인"
$deployExit = Invoke-Native -PassThruExitCode -Command {
    & ssh @sshOpts $target "echo $remoteBase64 | base64 -d | bash -s"
}

if ($deployExit -ne 0) {
    Write-Host ""
    Write-Err "배포 실패. 위 로그를 확인하세요."
    Write-Host ""
    Write-Host "   되돌리려면:" -ForegroundColor Yellow
    Write-Host "     ssh -i `"$KeyPath`" $target" -ForegroundColor Yellow
    Write-Host "     sudo systemctl stop $ServiceName" -ForegroundColor Yellow
    Write-Host "     sudo mv $RemoteBackup $RemoteJar" -ForegroundColor Yellow
    Write-Host "     sudo chown $RemoteOwner $RemoteJar" -ForegroundColor Yellow
    Write-Host "     sudo systemctl start $ServiceName" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "   Schema-validation 오류라면 스키마 반영을 빠뜨린 것입니다." -ForegroundColor Yellow
    Write-Host "   deploy/setup.md 의 '다시 배포할 때' 를 보세요." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Ok "배포 완료"
Write-Host "   되돌릴 백업: $RemoteBackup" -ForegroundColor DarkGray
Write-Host "   로그: ssh -i `"$KeyPath`" $target 'sudo tail -f /var/log/idis/idis.log'" -ForegroundColor DarkGray
