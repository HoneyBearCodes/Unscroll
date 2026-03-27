# Unscroll

Unscroll is an Android application designed to help you curb "doom-scrolling" and regain control of your time. By identifying apps that distract you most, Unscroll gracefully steps in to introduce mindful friction instead of blocking them outright.

## Features

- **App Blacklist**: Select the apps you find most addictive and restrict them.
- **Premium Interventions**: A beautifully designed "Reality Check" intercept that encourages mindfulness when opening a restricted app.
- **Hold-to-Confirm Intentionality**: To bypass an intervention, you must hold a confirmation button for a continuous 5 seconds, ensuring you're opening the app intentionally rather than reflexively.
- **Snooze Mechanics**: Temporarily pause the friction for short periods (e.g., 5, 10, or 15 minutes) if you actively need to use the app in the moment.

## How It Works

Unscroll listens to app transitions via Android's `AccessibilityService`. When a transition to a blacklisted application is detected, it immediately throws up an `OverlayService` screen utilizing the `WindowManager` API.

## Setup and Installation

Unscroll requires standard "digital wellbeing" permissions to function correctly:

1. **Draw Over Other Apps**: Allows Unscroll to display the friction interface over external apps.
2. **Usage Access**: Enables accurate time-tracking (optional based on future roadmap).
3. **Accessibility Service**: Required to monitor application launches reliably.

> **Note:** To test Unscroll, install the APK, launch it, grant the required permissions via the setup flow, add a test app (like YouTube or Instagram) to the blocked apps list, and then navigate to that app.

## Contributing
Contributions are welcome. Please open an issue or submit a pull request if you'd like to improve the app.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
