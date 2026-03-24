#!/bin/bash

# Define variables.
OS_TYPE=""
if [[ "$OSTYPE" == "darwin"* ]]; then
    OS_TYPE="mac"
elif [[ "$OSTYPE" == "linux-gnu"* ]] || [[ "$OSTYPE" == "linux"* ]]; then
    OS_TYPE="linux"
else
    echo "Unsupported OS"
    read -p "Press any key to exit..."
    exit 1
fi

SDK_URL_LINUX="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
SDK_URL_MAC="https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"

INSTALL_DIR="$HOME/android-sdk"
CMD_LINE_TOOLS_DIR="$INSTALL_DIR/cmdline-tools"

echo "Detected OS: $OS_TYPE"
echo "Installing Android SDK to: $INSTALL_DIR"

# Create directory structure.
mkdir -p "$CMD_LINE_TOOLS_DIR"

# Download based on OS.
if [ "$OS_TYPE" == "mac" ]; then URL=$SDK_URL_MAC;
else URL=$SDK_URL_LINUX; fi

echo "Downloading Command Line Tools..."
curl -o sdk_tools.zip $URL

# Extract.
echo "Extracting..."
unzip -q sdk_tools.zip -d "$CMD_LINE_TOOLS_DIR"
rm sdk_tools.zip

# Fix directory structure.
# The SDK manager expects cmdline-tools/latest/bin/...
mv "$CMD_LINE_TOOLS_DIR/cmdline-tools" "$CMD_LINE_TOOLS_DIR/latest"

# Accept all licenses.
echo "Accepting licenses..."
yes | "$CMD_LINE_TOOLS_DIR/latest/bin/sdkmanager" --sdk_root="$INSTALL_DIR" --licenses

# Install essential packages.
echo "Installing platform-tools and build-tools..."
"$CMD_LINE_TOOLS_DIR/latest/bin/sdkmanager" --sdk_root="$INSTALL_DIR" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "----------------------------------------------------"
echo "SETUP COMPLETE!"
echo "Add these to your environment variables (.bashrc, .zshrc, or Windows Path):"
echo "ANDROID_HOME=$INSTALL_DIR"
echo "PATH=\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH"
read -p "Press any key to exit..."
