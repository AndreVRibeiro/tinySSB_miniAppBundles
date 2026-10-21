# 📡 tinySSB — The LoRa Descendant of Secure Scuttlebutt

![tinySSB Banner](doc/_img/tinySSB-banner.png)

**A modular mini-app architecture for Tremola, a decentralized Android messaging application.**

## 📖 About the Project

This project was developed as part of my Bachelor's thesis in Computer Science at the University of Basel. It focuses on refactoring **Tremola**, an Android client for Secure Scuttlebutt (SSB) and its tinySSB communication approach, to support a modular architecture for mini apps.

Over time, multiple student projects contributed mini apps to Tremola, leading to a fragmented codebase with app-specific logic embedded in the main application. This project introduces a **Unified Bundle Format** that provides a consistent way to structure, integrate, and manage mini apps.

The goal is to make Tremola easier to extend and maintain while reducing dependencies between individual mini apps and the core application.

## 🎯 Goals

- 🧩 **Design a Unified Bundle Format** - Define a standardized structure for mini apps.
- 🔧 **Refactor Tremola** - Adapt the application to support modular mini-app integration.
- 📱 **Create a Mini-App Menu** - Provide a dedicated interface for discovering and launching mini apps.
- 📋 **Migrate an Existing App** - Convert the Kanban application into a mini app to demonstrate the new architecture.

## ✨ Key Features

- 🗂️ **Modular Structure** - Each mini app has its own directory for logic, UI, assets, and resources.
- 📄 **Manifest-Based Configuration** - A `manifest.json` file describes each mini app's metadata.
- 🛠️ **Separate Kotlin Handlers** - Mini-app-specific behavior is handled independently of the core application.
- 🔌 **Plugin Loading System** - A shared parent class and plugin loader support mini-app discovery and initialization.
- 📲 **Android Integration** - Mini apps can be launched through a dedicated menu within Tremola.

## 🧰 Tech Stack

- **Languages:** Kotlin, JavaScript, HTML/CSS 
- **Platform:** Android
- **Development Environment:** Android Studio
- **Build System:** Gradle
- **Configuration:** JSON

## 🚀 Getting Started

### Prerequisites

- Android Studio
- Android SDK and a compatible Android emulator or physical Android device
- Gradle dependencies required by the project

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/AndreVRibeiro/tinySSB_miniAppBundles.git
   cd <repository-directory>
   ```

2. **Open the project**

   Launch Android Studio and select **Open**, then choose the cloned repository directory.

3. **Set up an emulator**

   Create an Android Virtual Device in Android Studio. A Pixel 5 emulator running Android 11 was used as the recommended configuration for this project.

4. **Build and run**

   Allow Gradle to sync and download the required dependencies. Once the build completes, run the application on the emulator or a compatible Android device.

## 🎓 Academic Context

- **Project:** Bachelor's thesis in Computer Science
- **Institution:** University of Basel
- **Focus:** Android development, modular software architecture, and decentralized communication

## 👤 Author

**André Vieira Ribeiro**

Master's student in Machine Intelligence at the University of Basel, with a background in Computer Science.

---

*This repository documents an academic project exploring a more modular and maintainable architecture for Tremola.*
