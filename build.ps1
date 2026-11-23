# Build VexView inject jar (MCP/NOTCH compatible) + native injector bundle.
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File injection\build.ps1
#
# 产物：injection\dist\VexView-Inject.jar / PigBridge.dll / PigInjector.exe
# 用法（与 PigClient 注入包一致）：
#   PigInjector.exe --process javaw.exe --jar VexView-Inject.jar --options namespace=auto
#   或在图形界面里选择 jar / 进程
param(
    [string]$Jdk = 'C:\Program Files\Java\jdk1.8.0_202'
)

$ErrorActionPreference = 'Stop'
$Here = Split-Path -Parent $MyInvocation.MyCommand.Path
$Vex  = (Resolve-Path (Join-Path $Here '..\VexView-main')).Path

$Javac = Join-Path $Jdk 'bin\javac.exe'
$JarEx = Join-Path $Jdk 'bin\jar.exe'
if (!(Test-Path $Javac)) { throw "javac not found: $Javac" }

$Build   = Join-Path $Here 'build'
$Classes = Join-Path $Build 'classes'
$Dist    = Join-Path $Here 'dist'
Remove-Item $Build -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $Classes | Out-Null
New-Item -ItemType Directory -Force $Dist | Out-Null

Write-Host '[1/4] resolve dependencies'
$g = 'C:\Users\Tex\.gradle\caches\modules-2\files-2.1'
function Find-Jar([string]$rel, [string]$filter) {
    $dir = Join-Path $g $rel
    if (!(Test-Path $dir)) { throw "missing dependency dir: $dir" }
    $f = Get-ChildItem $dir -Recurse -Filter $filter -File -ErrorAction SilentlyContinue |
        Sort-Object Name | Select-Object -First 1
    if (!$f) { throw "missing dependency jar: $filter under $dir" }
    return $f.FullName
}
$netty      = Find-Jar 'io.netty\netty-all' 'netty-all-4.0.23.Final.jar'
$lwjgl      = Find-Jar 'org.lwjgl.lwjgl\lwjgl' 'lwjgl-2.9.4-nightly-20150209.jar'
$lwjglUtil  = Find-Jar 'org.lwjgl.lwjgl\lwjgl_util' 'lwjgl_util-2.9.4-nightly-20150209.jar'
$log4jApi   = Find-Jar 'org.apache.logging.log4j\log4j-api' 'log4j-api-2.0-beta9.jar'
$commonsLang= Find-Jar 'org.apache.commons\commons-lang3' 'commons-lang3-3.3.2.jar'
$guava      = Find-Jar 'com.google.guava\guava' 'guava-17.0.jar'
$authlib    = Find-Jar 'com.mojang\authlib' 'authlib-1.5.21.jar'

# 完整的 MCP 映射 Minecraft+Forge 编译 SDK（ForgeGradle 生成，含全部原版类）
$forgeSrcDir = 'C:\Users\Tex\.gradle\caches\minecraft\net\minecraftforge\forge\1.8.9-11.15.1.2318-1.8.9\stable\22'
$forgeSrc = Join-Path $forgeSrcDir 'forgeSrc-1.8.9-11.15.1.2318-1.8.9.jar'
if (!(Test-Path $forgeSrc)) {
    $forgeSrc = Get-ChildItem $forgeSrcDir -Filter 'forgeSrc-1.8.9-*.jar' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'sources' } | Select-Object -First 1 -ExpandProperty FullName
}
if (!$forgeSrc -or !(Test-Path $forgeSrc)) { throw "missing forgeSrc MCP SDK jar under $forgeSrcDir" }

$cp = @(
    $forgeSrc,
    $authlib,
    (Join-Path $Here 'libs\asm-all-5.0.3.jar'),
    $netty, $lwjgl, $lwjglUtil, $log4jApi, $commonsLang, $guava
) -join ';'

Write-Host '[2/4] compile injection + VexView sources (JDK 8, MCP)'
$src = @()
$src += Get-ChildItem (Join-Path $Here 'src\main\java') -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$src += Get-ChildItem (Join-Path $Vex  'src\main\java') -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& $Javac -encoding UTF-8 -source 1.8 -target 1.8 -nowarn -Xmaxerrs 20000 -cp $cp -d $Classes @src
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

Write-Host '[3/4] assemble jar'
Copy-Item (Join-Path $Here 'src\main\resources\*') $Classes -Recurse -Force
Copy-Item (Join-Path $Vex  'src\main\resources\*') $Classes -Recurse -Force
# bundle ASM (agent 的字节码变换依赖它)
Push-Location $Classes
& $JarEx xf (Join-Path $Here 'libs\asm-all-5.0.3.jar')
if ($LASTEXITCODE -ne 0) { Pop-Location; throw 'asm extract failed' }
Pop-Location

$InjectJar = Join-Path $Build 'VexView-Inject.jar'
Push-Location $Classes
& $JarEx cf $InjectJar .
if ($LASTEXITCODE -ne 0) { Pop-Location; throw 'jar create failed' }
Pop-Location

$mf = Join-Path $Build 'VEX.MF'
Set-Content -Path $mf -Encoding ASCII -Value @(
    'Pig-Entry: cn.pig.runtime.VexRuntime'
)
& $JarEx ufm $InjectJar $mf
if ($LASTEXITCODE -ne 0) { throw 'manifest update failed' }

Write-Host '[4/4] output'
Copy-Item $InjectJar $Dist -Force
foreach ($native in @('PigBridge.dll', 'PigInjector.exe')) {
    try {
        Copy-Item (Join-Path $Here "native\$native") $Dist -Force
    } catch {
        Write-Warning "无法覆盖 dist\$native（可能正在运行注入器）。jar 已更新，native 保持原样：$($_.Exception.Message)"
    }
}
Get-ChildItem $Dist | ForEach-Object { '  ' + $_.Name + '  ' + [math]::Round($_.Length / 1MB, 1) + ' MB' }
Write-Host 'OK -> ' $Dist
