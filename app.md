### Project Overview
CryptoSeal is a business logistics Android application. 
That implements a chain of custody for a package so the sender and receiver know where their delivery is. 
The person who is at the bottom of the chain is currently is responsible for the integrity. 


### Architectural Guidelines
* The UI is built entirely using Jetpack Compose and Kotlin.
* Implement Material Design 3 components.t
* Use standard Jetpack Navigation Compose for routing between screens.
* Maintain state using ViewModels.
* Keep the package lists hoisted in a shared ViewModel so that actions in the Creator and Scanner tabs immediately update the UI of the list.

How the files should be organized:  
app.cryptoseal/  
├── MainActivity.kt
├── Navigation.kt
├── Theme.kt
├── data/  
│   ├── PackageItem.kt
│   └── User.kt
├── screens/  
│   ├── LoginScreen.kt
│   └── DashboardScreen.kt
└── tabs/  
    ├── PackagesViewModel.kt
    ├── packages/
    │   ├── PackagesTab.kt  
    │   └── PackageDetailSheet.kt
    ├── creator/
    │   └── CreatorTab.kt
    ├── scanner/  
    │   └── ScannerTab.kt
    ├── activity/  
    │   └── ActivityTab.kt   
    └── profile/  
        ├── ProfileTab.kt  
        └── ProfileViewModel.kt


### Navigation
The app features a standard Login screen, requiring a Username and Password, followed by a main dashboard utilizing a Bottom Navigation Bar with five distinct tabs.
Upon successful authentication, the user is navigated to the Main Dashboard, and the backstack is cleared so they cannot navigate back to the login screen using the device's back button.
The main interface consists of a Scaffold with a BottomNavigationBar containing five specific destinations, ordered left to right for ergonomic thumb reach with the most frequently accessed screens at the edges.

### 1 Packages
A unified scrollable LazyColumn displaying all of the user's shipments, both sent and received. 
Use a `Card` or a customized `Row` with rounded corners (e.g., `RoundedCornerShape`) to act as a clickable container. Inside the row, place a leading `Icon` (representing a package), followed by a `Text` element for the shipment name, a small `Badge` or colored `Text` for the status indicator, and a trailing `Icon` (a right-pointing chevron/arrow) at the far right edge to indicate it is clickable for more details. 
Tapping a package card navigates to the Package Detail Screen. 
This is not a separate bottom nav destination, it is a nested route so the user stays contextually within the Packages tab. 
The back button returns to the list.

**Package Detail Sheet**

This is an almost full-screen view that opens when a user taps a package card from the Packages tab or an event from the Activity tab.
Display the shipment name, sender and receiver, current status, recipient or sender information, and the date created. A scrollable vertical timeline implemented as a `LazyColumn` with custom drawn connectors (vertical lines between nodes). 
Each node displays the handler's name, the timestamp of their scan, and their GPS location if available. The first entry is always the creator, and the last entry is the most recent scan. This is the core accountability feature of CryptoSeal.

### 2 Activity
A real-time, chronological feed of scan events across all of the user's shipments. This provides an at-a-glance overview of what is happening without requiring the user to drill into each package individually. 
A scrollable `LazyColumn` of event entries.
Each event entry should display a leading `Icon` representing the event type (e.g., scanned, created, delivered), a `Text` line describing the event (e.g., "Package #42 was scanned by Handler X"), a secondary `Text` for the timestamp and location, and a subtle divider between entries. 
Tapping an event entry navigates to the corresponding Package Detail Screen.
If there is no activity yet, display a centered illustration or icon with a `Text` message such as "No activity yet"

### 3 QR Code Creator
A form where the user inputs shipment details to generate a unique QR code label. 
Successful generation adds an entry to the Packages list under "Sent" and initializes the chain of custody with the creator as the first entry.
A vertically scrollable `Column`.
Use multiple `OutlinedTextField` components for the user to input the shipment details. 
Name is a wide box at the top.
Sender is already filled in and unchangeable on the left and on the right receiver.
A bigger text box for the description.
A image submission placeholder for now.
Place a wide, prominent `Button` at the bottom labeled "Generate QR Code". 
Upon clicking, it should display the generated QR code in a centered `Image` component or a modal dialog with an option to share or print.

### 4 QR Code Scanner
A camera-enabled screen utilizing Android CameraX to scan external CryptoSeal QR codes. 
The live viewfinder is encapsulated within a rounded, bordered Card element that integrates seamlessly with the UI. 
A dark navy rounded overlay provides a border to the camera preview, featuring a custom-drawn targeting reticle composed of cyan-colored corner brackets. 
A Text prompt located below the reticle instructs the user to "Align QR code within frame." 
Upon a successful scan, the app parses the data and registers the current user into the shipment's chain of custody.
If it is the receiver it completes that shipment and surfaces a ModalBottomSheet displaying the shipment details + complaints and confirmation button.

### 5 Profile
A screen displaying the current authorized employee's identity and their contacts directory.
Top section should feature a circular user avatar (`Box` with `CircleShape`), `Text` for the Username, and below some text for the person's role.
Below the user card, two sections for email and phone number.
After these a "Contacts" `LazyColumn` of entries. 
Each entry displays the contact's name and their role. 
Include a trailing `IconButton` for actions. 
At the top of the contacts section, include a `Button` or `FloatingActionButton` for adding new contacts.
   

