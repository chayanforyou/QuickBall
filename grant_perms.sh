#!/bin/bash

# Configuration
PKG="io.github.chayanforyou.quickball"

# Function to grant permissions to a specific package
grant_permissions() {
    local PKG=$1

    # Check if package is installed
    if ! adb shell pm list packages | grep -q "$PKG"; then
        echo "Skipping $PKG (not installed)"
        return
    fi

    echo "Granting permissions for $PKG..."

    # AppOps (Special Permissions)
    adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
    adb shell appops set $PKG WRITE_SETTINGS allow

    # Whitelist from battery optimization / doze
    adb shell cmd deviceidle whitelist +$PKG

    # Enable Services
    # Accessibility Service
    ACCESSIBILITY="$PKG/io.github.chayanforyou.quickball.service.QuickBallService"
    adb shell settings put secure enabled_accessibility_services $ACCESSIBILITY
    adb shell settings put secure accessibility_enabled 1

    echo "Finished $PKG"
}

# Run
grant_permissions "$PKG"

echo "All tasks complete! You might need to restart the app for some changes to take effect."