markdown
# CryptoSeal Development Guide

## Project Overview
CryptoSeal is a business logistics Android application. The UI is built entirely using Jetpack Compose and Kotlin. The app features a standard Login screen, requiring a Username and Password. followed by a main dashboard utilizing a Bottom Navigation Bar with five distinct tabs. Upon successful authentication, the user is navigated to the Main Dashboard, and the backstack is cleared so they cannot navigate back to the login screen using the device's back button.


## Architectural Guidelines
* Use Jetpack Compose for all UI elements.
* Implement Material Design 3 (Material You) components.
* Use standard Jetpack Navigation Compose for routing between screens.
* Maintain state using ViewModels.
* Keep the package lists hoisted in a shared ViewModel or repository so that actions in the Creator and Scanner tabs immediately update the UI in the Packages and Activity tabs.

How the files should be organized:  
app.cryptoseal/  
├── MainActivity.kt  
├── CryptoSealApplication.kt  
├── core/  
│   ├── navigation/  
│   ├── theme/  
│   ├── components/  
│   ├── crypto/  
│   └── data/  
└── feature/  
├── auth/  
│   ├── LoginScreen.kt  
│   └── AuthViewModel.kt  
├── packages/  
│   ├── PackagesScreen.kt  
│   ├── PackageDetailScreen.kt  
│   ├── CreatorScreen.kt  
│   └── PackagesViewModel.kt  
├── scanner/  
│   ├── ScannerScreen.kt  
│   └── ScannerViewModel.kt  
├── activity/  
│   ├── ActivityScreen.kt  
│   └── ActivityViewModel.kt  
└── profile/  
├── ProfileScreen.kt  
└── ProfileViewModel.kt


## Tabs and Navigation
The main interface consists of a Scaffold with a BottomNavigationBar containing five specific destinations, ordered left to right for ergonomic thumb reach with the most frequently accessed screens at the edges:

### 1 Packages
A unified scrollable LazyColumn displaying all of the user's shipments, both sent and received. 
Use a `Card` or a customized `Row` with rounded corners (e.g., `RoundedCornerShape`) to act as a clickable container. Inside the row, place a leading `Icon` (representing a package), followed by a `Text` element for the shipment name, a small `Badge` or colored `Text` for the status indicator, and a trailing `Icon` (a right-pointing chevron/arrow) at the far right edge to indicate it is clickable for more details. 
Tapping a package card navigates to the Package Detail Screen. 
This is not a separate bottom nav destination, it is a nested route so the user stays contextually within the Packages tab. 
The back button returns to the list.

**Package Detail Screen**

This is a full-screen view that opens when a user taps a package card from the Packages tab or an event from the Activity tab.
Display the shipment name, sender and receiver, current status, recipient or sender information, and the date created. A scrollable vertical timeline implemented as a `LazyColumn` with custom drawn connectors (vertical lines between nodes). 
Each node displays the handler's name, the timestamp of their scan, and their GPS location if available. The first entry is always the creator, and the last entry is the most recent scan. This is the core accountability feature of CryptoSeal.


### 2 QR Code Scanner
A camera-enabled screen using the device hardware to scan external CryptoSeal QR codes. Successful scans parse the data, register the current user into the shipment's chain of custody, and add an entry to the Packages list under "Received".
    * **UI Component:** Implement Android `CameraX` for the live viewfinder taking up the majority of the screen.
    * **Layout:** Add a semi-transparent dark overlay on top of the camera preview with a clear, transparent square cutout in the center (a targeting reticle). Add a `Text` prompt below the square saying "Align QR code within frame". When a code is detected, surface a `ModalBottomSheet` displaying the shipment information and a confirmation that the user has been added to the chain of custody.

### 3 QR Code Creator
A form where the user inputs shipment details to generate a unique QR code label. Successful generation adds an entry to the Packages list under "Sent" and initializes the chain of custody with the creator as the first entry.
    * **UI Component:** A vertically scrollable `Column`.
    * **Layout:** Use multiple `OutlinedTextField` components for the user to input the shipment details (recipient, description, routing information). Place a wide, prominent `Button` at the bottom labeled "Generate QR Code". Upon clicking, it should display the generated QR code in a centered `Image` component or a modal dialog with an option to share or print.

### 4 Activity
A real-time, chronological feed of scan events across all of the user's shipments. This provides an at-a-glance overview of what is happening without requiring the user to drill into each package individually.
    * **UI Component:** A scrollable `LazyColumn` of event entries.
    * **Layout:** Each event entry should display a leading `Icon` representing the event type (e.g., scanned, created, delivered), a `Text` line describing the event (e.g., "Package #42 was scanned by Handler X"), a secondary `Text` for the timestamp and location, and a subtle divider between entries. Tapping an event entry navigates to the corresponding Package Detail Screen.
    * **Empty State:** If there is no activity yet, display a centered illustration or icon with a `Text` message such as "No activity yet. Create or scan a package to get started."

### 5 Profile
A screen displaying the current authorized employee's identity and their contacts directory.
    * **UI Component:** A standard scrollable `Column` layout.
    * **User Card:** Top section should feature a circular user avatar (`Box` with `CircleShape`), `Text` for the Employee ID, and a "Cryptographic Identity" section showing their Public Key inside a read-only `OutlinedTextField` with an integrated "Copy" `IconButton`.
    * **Contacts Directory:** Below the user card, a section header "Contacts" followed by a `LazyColumn` of contact entries. Each entry displays the contact's name and their role or company. Include a trailing `IconButton` for actions (e.g., remove). At the top of the contacts section, include an `OutlinedTextField` for searching and a `Button` or `FloatingActionButton` for adding new contacts.
   

