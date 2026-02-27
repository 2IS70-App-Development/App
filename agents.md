# CryptoSeal Development Guide

## Project Overview
CryptoSeal is a decentralized business logistics Android application. The UI is built entirely using Jetpack Compose and Kotlin. The app features a standard Login screen, followed by a main dashboard utilizing a Bottom Navigation Bar with five distinct screens. The Login Screen is a generic entry point requiring a Username and Password. Upon successful authentication, the user is navigated to the Main Dashboard, and the backstack is cleared so they cannot navigate back to the login screen using the device's back button.


## Architectural Guidelines
* Use Jetpack Compose for all UI elements.
* Implement Material Design 3 (Material You) components.
* Use standard Jetpack Navigation Compose for routing between screens.
* Maintain state using ViewModels.
* Keep the package lists hoisted in a shared ViewModel or repository so that actions in the Creator and Scanner tabs immediately update the UI in the List tabs.

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
    │   ├── SendingListScreen.kt  
    │   ├── ReceivingListScreen.kt  
    │   ├── CreatorScreen.kt  
    │   └── PackagesViewModel.kt  
    ├── scanner/  
    │   ├── ScannerScreen.kt  
    │   └── ScannerViewModel.kt  
    └── profile/  
        ├── ProfileScreen.kt  
        └── ProfileViewModel.kt  


## Main Dashboard (Bottom Navigation)
The main interface consists of a Scaffold with a BottomNavigationBar containing five specific destinations:

1.  **Sending Packages List:** A scrollable LazyColumn displaying packages the user has prepared for shipment.
    * **UI Component:** Use a `Card` or a customized `Row` with rounded corners (e.g., `RoundedCornerShape`) to act as a clickable container.
    * **Layout:** Inside the row, place a leading `Icon` (representing a package), followed by a `Text` element for the shipment name, a small `Badge` or colored `Text` for the status indicator, and a trailing `Icon` (a right-pointing chevron/arrow) at the far right edge to indicate it is clickable for more details.
2.  **Receiving Packages List:** A scrollable LazyColumn displaying packages the user has scanned and received.
    * **UI Component:** Must visually match the exact `Card`/`Row` component design defined in the Sending Packages List.
3.  **QR Code Creator:** A form where the user inputs shipment details to generate a unique QR code label[cite: 10]. Successful generation adds an entry to the Sending Packages List.
    * **UI Component:** A vertically scrollable `Column`.
    * **Layout:** Use multiple `OutlinedTextField` components for the user to input the public routing information and the private manifest data[cite: 15, 18]. Place a wide, prominent `Button` at the bottom labeled "Generate & Sign". Upon clicking, it should display the generated QR code in a centered `Image` component or a modal dialog.
4.  **QR Code Scanner:** A camera-enabled screen using the device hardware to scan external CryptoSeal QR codes for instant checks[cite: 13, 14]. Successful scans parse the data and add an entry to the Receiving Packages List.
    * **UI Component:** Implement Android `CameraX` for the live viewfinder taking up the majority of the screen.
    * **Layout:** Add a semi-transparent dark overlay on top of the camera preview with a clear, transparent square cutout in the center (a targeting reticle). Add a `Text` prompt below the square saying "Align QR code within frame". When a code is detected, surface a `ModalBottomSheet` displaying the decrypted information.
5.  **Settings and Profile:** A screen displaying the current authorized employee's ID[cite: 8], cryptographic public key, and general app preferences.
    * **UI Component:** A standard `Column` layout.
    * **Layout:** Top section should feature a circular user avatar (`Box` with `CircleShape`) and `Text` for the Employee ID. Below that, a "Cryptographic Identity" section showing their Public Key inside a read-only `OutlinedTextField` with an integrated "Copy" icon. Below that, a list of app preferences using `Switch` components (e.g., for toggling offline caching).
