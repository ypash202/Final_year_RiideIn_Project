# Final_year_RiideIn_Project

Riide In is a final-year Android mobile application prototype designed for a ride-sharing service. The app supports two main user roles: customer and driver. Customers can search for a ride, select a vehicle type, contact a driver, track ride progress, cancel rides, view ride history, and manage account-related options. Drivers can go online, receive ride requests, contact customers, update ride status, complete rides, and view driver-related information.

The project focuses on building a realistic ride-sharing workflow using Android Studio, Kotlin, XML layouts, Firebase Authentication, and Cloud Firestore.

## Project Overview

Riide In was developed as a prototype ride-sharing application for a final-year computing project. The aim of the project is to demonstrate a working mobile app that connects customers and drivers through a simple ride request and ride management system.

The app includes customer and driver flows, role-based navigation, ride status updates, contact/chat functionality, ride history, wallet-style fare information, profile handling, and Firebase-based data storage.

## Main Features

### Customer Features

- Customer registration and login
- Phone OTP verification support
- Customer home screen with map-style interface
- Enter pickup and destination route
- Select vehicle type such as moto, cab, or delivery
- Choose and contact a driver
- Active ride tracking screen
- Use SOS button during an active ride for emergency alert support
- Contact driver through ride chat
- Call driver from the contact screen
- Cancel ride with cancellation reason
- View completed and cancelled ride history
- Access side menu options such as wallet, messages, history, notifications, invite friends, settings, and logout

### Driver Features

- Driver login and role-based access
- Driver home screen with online/offline status
- Driver side menu and bottom navigation
- Receive and manage ride requests
- Contact customer during an active ride
- Call customer from the contact screen
- Mark arrival status
- Start and complete ride flow
- Handle customer cancellation
- Driver wallet-style fare information and ride history support

### Messaging Features

- Active ride chat between customer and driver
- Messages saved under ride-based chat records
- Sender and receiver role handling
- Seen status support for viewed messages
- Toast notification when a new ride message is received
- Inbox-style messages screen for customer and driver
- Delete support for selected chat records

### Safety and Ride Control Features

- SOS alert support during active ride
- Ride cancellation handling
- Ride status updates such as arriving, arrived, in progress, completed, and cancelled
- Firestore records for completed rides and SOS alerts

## Technologies Used

- Android Studio
- Kotlin
- XML layouts
- Firebase Authentication
- Cloud Firestore
- Firebase phone authentication test numbers
- Git and GitHub
- Genymotion and Android Emulator for testing

## Firebase Usage

The project uses Firebase for authentication and cloud data storage.

Firebase Authentication is used for:

- Email and password login
- Phone OTP verification during development

Cloud Firestore is used for storing:

- User profile details
- Customer and driver role information
- Ride requests
- Ride chat messages
- Completed ride history
- SOS alert records
- Wallet-style fare and history-related ride data

## App Structure

The app contains separate screens and activities for customer and driver workflows.

Important screens include:

- Onboarding screen
- Login screen
- Register screen
- OTP verification screen
- Customer home screen
- Enter route screen
- Vehicle selection screen
- Choose driver screen
- Ride tracking screen
- Driver home screen
- Driver ride request screen
- Driver arrived/navigation screen
- Messages screen
- History screen
- Settings screen
- Side menu screen
- Cancel ride screen

## Installation and Setup

1. Clone or download the project from GitHub.
2. Open the project in Android Studio.
3. Make sure the Android SDK and required Gradle tools are installed.
4. Add the Firebase configuration file inside the `app` folder:

```text
app/google-services.json