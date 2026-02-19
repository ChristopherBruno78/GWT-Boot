# GWT Boot CLI Installer
# Installs the Java CLI tool and wrapper scripts for Linux, macOS, and Windows

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_error() {
    echo -e "${RED}Error: $1${NC}" >&2
}

print_success() {
    echo -e "${GREEN}$1${NC}"
}

print_info() {
    echo -e "${BLUE}$1${NC}"
}

print_warning() {
    echo -e "${YELLOW}$1${NC}"
}

echo ""
print_info "==================================="
print_info "GWT Boot CLI Installer"
print_info "==================================="
echo ""

# Detect OS first
OS="unknown"
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS="linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    OS="macos"
elif [[ "$OSTYPE" == "cygwin" ]] || [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]]; then
    OS="windows"
fi

print_info "Detected OS: $OS"
echo ""

# Prompt for system-wide installation (Linux/macOS only)
if [ "$OS" != "windows" ]; then
    print_info "This installer will install GWT Boot CLI system-wide to /usr/local/"
    echo ""
    read -p "Do you want to proceed with installation? (y/N): " PROCEED
    PROCEED=${PROCEED:-n}
    if [ "$PROCEED" != "y" ] && [ "$PROCEED" != "Y" ]; then
        print_warning "Installation cancelled."
        exit 0
    fi
    echo ""

    # Verify sudo access
    print_info "Requesting sudo access..."
    if ! sudo -v; then
        print_error "Failed to obtain sudo privileges. Please run as a user with sudo access."
        exit 1
    fi
    print_success "Sudo access granted."
    echo ""

    # Keep sudo session alive in background
    (while true; do sudo -n true; sleep 50; kill -0 "$$" || exit; done 2>/dev/null) &
    SUDO_KEEPALIVE_PID=$!
    trap "kill $SUDO_KEEPALIVE_PID 2>/dev/null" EXIT
fi

# Get the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Install archetype to local Maven repository
print_info "Installing GWT Boot archetype to Maven repository..."
echo ""

(cd "$PROJECT_ROOT" && mvn clean install)

if [ $? -ne 0 ]; then
    echo ""
    print_error "Failed to install archetype to Maven repository"
    exit 1
fi

echo ""
print_success "Archetype installed to Maven repository"
echo ""

# Build the CLI tool
print_info "Building GWT Boot CLI tool..."
echo ""

(cd "$SCRIPT_DIR" && mvn clean package)

if [ $? -ne 0 ]; then
    echo ""
    print_error "Failed to build CLI tool"
    exit 1
fi

echo ""
print_success "CLI tool built successfully"
echo ""

JAR_FILE="$SCRIPT_DIR/target/gwt-boot-cli.jar"
if [ ! -f "$JAR_FILE" ]; then
    print_error "JAR file not found after build: $JAR_FILE"
    exit 1
fi

# Determine installation directories based on OS
if [ "$OS" = "windows" ]; then
    # Windows installation paths (user-local, no sudo needed)
    LIB_DIR="$HOME/AppData/Local/gwt-boot/lib"
    BIN_DIR="$HOME/AppData/Local/gwt-boot/bin"
    INSTALL_LOCATION="user"
    SUDO_CMD=""
else
    # Linux/macOS installation paths (system-wide, sudo required)
    LIB_DIR="/usr/local/lib/gwt-boot"
    BIN_DIR="/usr/local/bin"
    INSTALL_LOCATION="system-wide"
    SUDO_CMD="sudo"
fi

print_info "Installing to $INSTALL_LOCATION location:"
echo "  JAR: $LIB_DIR"
echo "  Scripts: $BIN_DIR"
echo ""

# Check for existing installations
WILL_OVERWRITE=false
OVERWRITE_FILES=()

if [ -f "$LIB_DIR/gwt-boot-cli.jar" ]; then
    WILL_OVERWRITE=true
    OVERWRITE_FILES+=("$LIB_DIR/gwt-boot-cli.jar")
fi

if [ "$OS" = "windows" ]; then
    if [ -f "$BIN_DIR/gwt-boot.bat" ]; then
        WILL_OVERWRITE=true
        OVERWRITE_FILES+=("$BIN_DIR/gwt-boot.bat")
    fi
    if [ -f "$BIN_DIR/gwt-boot.ps1" ]; then
        WILL_OVERWRITE=true
        OVERWRITE_FILES+=("$BIN_DIR/gwt-boot.ps1")
    fi
else
    if [ -f "$BIN_DIR/gwt-boot" ]; then
        WILL_OVERWRITE=true
        OVERWRITE_FILES+=("$BIN_DIR/gwt-boot")
    fi
fi

# Warn if files will be overwritten
if [ "$WILL_OVERWRITE" = true ]; then
    print_warning "WARNING: The following files will be overwritten:"
    for file in "${OVERWRITE_FILES[@]}"; do
        echo "  - $file"
    done
    echo ""
    read -p "Continue with installation? (y/N): " CONFIRM
    CONFIRM=${CONFIRM:-n}
    if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
        print_warning "Installation cancelled."
        exit 0
    fi
    echo ""
fi

# Create directories
$SUDO_CMD mkdir -p "$LIB_DIR"
$SUDO_CMD mkdir -p "$BIN_DIR"

# Copy JAR
print_info "Installing JAR file..."
$SUDO_CMD cp "$JAR_FILE" "$LIB_DIR/gwt-boot-cli.jar"
print_success "JAR installed to $LIB_DIR/gwt-boot-cli.jar"
echo ""

# Create and install wrapper scripts
if [ "$OS" = "windows" ]; then
    # Create Windows batch script
    print_info "Creating Windows batch script..."
    cat > "$BIN_DIR/gwt-boot.bat" << 'EOF'
@echo off
java -jar "%~dp0\..\lib\gwt-boot-cli.jar" %*
EOF

    # Create PowerShell script
    cat > "$BIN_DIR/gwt-boot.ps1" << 'EOF'
#!/usr/bin/env pwsh
$LIB_DIR = Join-Path $PSScriptRoot "..\lib"
$JAR_FILE = Join-Path $LIB_DIR "gwt-boot-cli.jar"
& java -jar $JAR_FILE $args
EOF

    print_success "Windows scripts installed:"
    echo "  - $BIN_DIR/gwt-boot.bat"
    echo "  - $BIN_DIR/gwt-boot.ps1"
    echo ""

    print_warning "Add to PATH:"
    echo "  setx PATH \"%PATH%;$BIN_DIR\""

else
    # Create Unix shell script (Linux/macOS)
    print_info "Creating shell script..."
    $SUDO_CMD tee "$BIN_DIR/gwt-boot" > /dev/null << EOF
#!/bin/bash
# GWT Boot CLI launcher
exec java -jar "$LIB_DIR/gwt-boot-cli.jar" "\$@"
EOF

    $SUDO_CMD chmod +x "$BIN_DIR/gwt-boot"

    print_success "Shell script installed: $BIN_DIR/gwt-boot"
    echo ""

    # Check if BIN_DIR is in PATH
    if [[ ":$PATH:" != *":$BIN_DIR:"* ]]; then
        print_warning "Add to PATH:"
        if [ -f "$HOME/.bashrc" ]; then
            echo "  echo 'export PATH=\"\$PATH:$BIN_DIR\"' >> ~/.bashrc"
            echo "  source ~/.bashrc"
        elif [ -f "$HOME/.zshrc" ]; then
            echo "  echo 'export PATH=\"\$PATH:$BIN_DIR\"' >> ~/.zshrc"
            echo "  source ~/.zshrc"
        else
            echo "  export PATH=\"\$PATH:$BIN_DIR\""
        fi
    fi
fi

echo ""
print_success "==================================="
print_success "Installation Complete!"
print_success "==================================="
echo ""

echo "Successfully installed:"
echo "  - GWT Boot Archetype: ~/.m2/repository/com/edusoftwerks/gwt-boot-archetype/1.0.0/"
echo "  - GWT Boot CLI JAR: $LIB_DIR/gwt-boot-cli.jar"
echo "  - GWT Boot CLI Script: $BIN_DIR/gwt-boot"
echo ""

if [[ ":$PATH:" == *":$BIN_DIR:"* ]] || [ "$INSTALL_LOCATION" = "system-wide" ]; then
    echo "GWT Boot CLI is ready to use:"
    echo "  gwt-boot version"
    echo ""
    echo "Get started:"
    echo "  gwt-boot boot myapp"
else
    echo "Installation successful!"
    echo "Please add $BIN_DIR to your PATH to use gwt-boot"
fi
echo ""
