# QuickBall - Floating Ball Accessibility Service

A floating ball that appears on the edge of your screen and stays visible over all apps, even when your app is in the background.

## Features

- **Always Visible**: The floating ball appears over all apps and stays visible even when your app is in the background
- **Draggable**: You can drag the ball to any position on the screen
- **Auto-Snap**: The ball automatically snaps to the nearest edge when you finish dragging
- **Accessibility Service**: Uses Android's accessibility service to ensure the ball is always on top

## How to Use

1. **Install the App**: Build and install the QuickBall app on your Android device
2. **Enable Accessibility Service**: 
   - Open the QuickBall app
   - Tap "Enable Service" button
   - This will open Android's Accessibility Settings
   - Find "QuickBall" in the list of accessibility services
   - Enable the QuickBall service
3. **Floating Ball Appears**: Once enabled, a red floating ball will appear on the right edge of your screen
4. **Interact with the Ball**:
   - **Drag**: Touch and drag the ball to move it around the screen
   - **Tap**: Single tap the ball for quick actions (currently placeholder)
   - **Auto-Snap**: When you release the ball, it will automatically snap to the nearest edge

## Technical Details

- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 15 (API 35)
- **Permissions Required**:
  - `SYSTEM_ALERT_WINDOW`: For displaying overlay windows
  - `BIND_ACCESSIBILITY_SERVICE`: For accessibility service functionality

## Architecture

- **FloatingBallAccessibilityService**: Main accessibility service that creates and manages the floating ball
- **MainActivity**: UI for enabling/disabling the service and checking status
- **Accessibility Service**: Uses `TYPE_ACCESSIBILITY_OVERLAY` for Android 8.0+ and `TYPE_PHONE` for older versions

## Customization

You can customize the floating ball by modifying:

- **Ball Appearance**: Edit `floating_ball_background.xml` to change colors, size, or shape
- **Ball Size**: Modify the `ballSize` variable in `FloatingBallAccessibilityService.kt`
- **Ball Position**: Change initial position in the `createLayoutParams()` method
- **Tap Actions**: Implement functionality in the `onBallTapped()` method

## Troubleshooting

- **Ball Not Visible**: Make sure the accessibility service is enabled in Android settings
- **Can't Drag Ball**: Ensure you have the necessary permissions and the service is running
- **Ball Disappears**: Check if the accessibility service is still enabled

## Security Note

This app requires accessibility permissions to function. These permissions are necessary for the floating ball to appear over other apps. The app only uses these permissions for the floating ball functionality and does not access or monitor any user data.
