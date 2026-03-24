@echo off
setlocal

:: Define variables
set "SDK_URL_WIN=https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
set "INSTALL_DIR=%USERPROFILE%\android-sdk"
set "CMD_LINE_TOOLS_DIR=%INSTALL_DIR%\cmdline-tools"

echo Downloading Android SDK for Windows
echo Installing Android SDK to: %INSTALL_DIR%

:: Create directory structure
if not exist "%CMD_LINE_TOOLS_DIR%" mkdir "%CMD_LINE_TOOLS_DIR%"

:: Download Command Line Tools
echo Downloading Command Line Tools...
powershell -Command "Invoke-WebRequest -Uri '%SDK_URL_WIN%' -OutFile 'sdk_tools.zip'"

:: Extract
echo Extracting...
powershell -Command "Expand-Archive -Path 'sdk_tools.zip' -DestinationPath '%CMD_LINE_TOOLS_DIR%' -Force"
del sdk_tools.zip

:: Fix directory structure
:: Move the internal 'cmdline-tools' folder to 'latest'
move "%CMD_LINE_TOOLS_DIR%\cmdline-tools" "%CMD_LINE_TOOLS_DIR%\latest"

:: Accept all licenses
echo Accepting licenses...
set "SDK_MANAGER=%CMD_LINE_TOOLS_DIR%\latest\bin\sdkmanager.bat"
(echo y&echo y&echo y&echo y&echo y&echo y) | "%SDK_MANAGER%" --sdk_root="%INSTALL_DIR%" --licenses

:: Install essential packages
echo Installing platform-tools and build-tools...
"%SDK_MANAGER%" --sdk_root="%INSTALL_DIR%" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo ----------------------------------------------------
echo SETUP COMPLETE!
echo Add these to your environment variables:
echo ANDROID_HOME=%INSTALL_DIR%
echo PATH=%%ANDROID_HOME%%\cmdline-tools\latest\bin;%%ANDROID_HOME%%\platform-tools;%%PATH%%
pause
