# tinySSB - the LoRa descendant of Secure Scuttlebutt

![tinySSB logo](doc/_img/tinySSB-banner.png)

## Unified Bundle Format for Modular Extensions

This project aims to refactor the Tremola app, a Secure Scuttlebutt (SSB) client for Android, to support a modular architecture that allows for easy integration and management of mini apps. This README provides an overview of the project, its goals and setup instructions.

## Project Overview

Tremola is a decentralized messaging app that operates without central servers, ensuring robust and private communication. Over the years, various student projects have contributed mini apps to Tremola, resulting in a fragmented codebase. This project addresses this issue by designing a unified bundle format for mini apps, enabling modular development and maintenance.

## Goals

- **Design a unified bundle format**: Establish a standardized structure for mini apps to ensure consistency and modularity.
- **Refactor the Tremola app**: Adapt the main codebase to support the new bundle format and remove hard-coded references to mini apps.
- **Create a mini app menu**: Develop a dedicated menu for launching mini apps, enhancing the user experience.
- **Implement example mini apps**: Transform the existing Kanban application into a mini app using the new bundle format as a proof of concept.

## Key Features

- **Modular Architecture**: Each mini app has its own directory with separated logic, GUI, assets, and resources.
- **Manifest File**: Each mini app includes a manifest.json file with metadata used for integration into the Tremola app.
- **Kotlin Handlers**: Custom handlers for each mini app are implemented in separate Kotlin files, ensuring no mini app-specific code in the main application.
- **Plugin System**: A parent class and Plugin Loader manage the discovery and initialization of mini apps.

## Setup instructions

1. **Clone the Repository**

2. **Open the Project in Android Studio**
   - Launch Android Studio.
   - Select "Open an existing Android Studio project" and navigate to the cloned repository folder.

3. **Setup Emulator**
   - Configure an Android Emulator in Android Studio. For this project, a Pixel 5 emulator with Android 11 is recommended.

4. **Build the Project**
   - Allow Gradle to build the project and resolve dependencies.

