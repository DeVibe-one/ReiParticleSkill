@echo off
setlocal
set ROOT=%~dp0
if not defined GRADLE_USER_HOME set GRADLE_USER_HOME=%ROOT%gradlehome
if not defined GRADLE_OPTS set GRADLE_OPTS=-Djava.io.tmpdir=%ROOT%tmp
pushd "%ROOT%"
call gradlew.bat --no-daemon :reiparticles-forge-runtime:build
if errorlevel 1 (
  popd
  exit /b %errorlevel%
)
if not exist "%ROOT%build\libs" mkdir "%ROOT%build\libs"
for %%F in ("%ROOT%reiparticles-forge-runtime\build\libs\*.jar") do (
  copy /Y "%%~fF" "%ROOT%build\libs\%%~nxF" >nul
)
echo Copied runtime jars to %ROOT%build\libs
popd
endlocal
