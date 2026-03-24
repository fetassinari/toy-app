# Toy Registry

An Android app to register and manage your children's toys. Built with Kotlin, Jetpack Compose, and Firebase.

## Features

- **Authentication**: Sign in with Google or Email/Password
- **Toy Management**: Add, edit, and delete toys with:
  - Name and description
  - Storage location (where the toy is kept)
  - Image attachments with thumbnail display
  - Category assignment
- **Category Management**: Create, edit, and delete categories to organize toys
- **Search**: Search toys by name, description, location, or category
- **Real-time Sync**: Data syncs in real-time via Firebase Firestore

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Hilt dependency injection
- **Backend**: Firebase (Auth, Firestore, Storage)
- **Image Loading**: Coil
- **Navigation**: Jetpack Navigation Compose

## Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- A Firebase project

### Firebase Setup

1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Add an Android app with package name `com.toyregistry.app`
3. Download the `google-services.json` file and place it in the `app/` directory
4. Enable the following Firebase services:
   - **Authentication**: Enable Email/Password and Google sign-in providers
   - **Cloud Firestore**: Create a database (start in test mode or configure security rules)
   - **Storage**: Set up Firebase Storage for image uploads

### Google Sign-In Setup

1. In Firebase Console, go to Authentication > Sign-in method > Google
2. Enable Google sign-in and configure the OAuth consent screen
3. The `default_web_client_id` string in `res/values/strings.xml` will be automatically populated from `google-services.json`

### Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### Storage Security Rules

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /toys/{userId}/{allPaths=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Building

1. Clone the repository
2. Add your `google-services.json` to the `app/` directory
3. Open in Android Studio
4. Sync Gradle and run the app

## Project Structure

```
app/src/main/java/com/toyregistry/app/
├── ToyRegistryApp.kt          # Application class (Hilt)
├── MainActivity.kt             # Single activity entry point
├── data/
│   ├── model/
│   │   ├── Toy.kt              # Toy data model
│   │   └── Category.kt         # Category data model
│   └── repository/
│       ├── AuthRepository.kt   # Firebase Auth operations
│       ├── ToyRepository.kt    # Firestore + Storage operations for toys
│       └── CategoryRepository.kt # Firestore operations for categories
├── di/
│   └── AppModule.kt            # Hilt dependency injection module
└── ui/
    ├── auth/
    │   ├── AuthScreen.kt       # Login/Register screen
    │   └── AuthViewModel.kt
    ├── home/
    │   ├── HomeScreen.kt       # Toy list with search
    │   └── HomeViewModel.kt
    ├── toy/
    │   ├── AddEditToyScreen.kt # Add/Edit toy form
    │   └── AddEditToyViewModel.kt
    ├── category/
    │   ├── CategoryScreen.kt   # Category management
    │   └── CategoryViewModel.kt
    ├── navigation/
    │   ├── NavRoutes.kt        # Route definitions
    │   └── ToyRegistryNavHost.kt # Navigation graph
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```
