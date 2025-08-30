# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

- **Run application**: `./gradlew run`
- **Build application**: `./gradlew build`
- **Clean build**: `./gradlew clean`
- **Run tests**: `./gradlew test`
- **Create modular runtime image**: `./gradlew jlink`
- **Create distributable ZIP**: `./gradlew jlinkZip`

## Project Architecture

### Technology Stack
- **Language**: Java 24
- **UI Framework**: JavaFX 26-ea+2 with AtlantaFX theme library
- **Backend**: Spring Boot 3.5.4 (running as embedded server, no web interface)
- **Database**: MongoDB (via Spring Data)
- **AI Integration**: Spring AI with OpenAI
- **Build System**: Gradle with Kotlin DSL

### Application Structure
This is a desktop AI assistant application with a hybrid architecture:

**Entry Point**: `AppLauncher` starts Spring Boot context without web layer, then launches JavaFX application

**Main Modules**:
- `com.patres.alina.common` - Shared models and DTOs
- `com.patres.alina.server` - Spring Boot backend services (message handling, OpenAI integration, plugins)
- `com.patres.alina.uidesktop` - JavaFX desktop UI components

**Key Components**:
- **Chat System**: Message handling with OpenAI integration, thread management, speech-to-text
- **Plugin System**: Dynamic function execution via OpenAI function calls, REST integrations
- **Global Shortcuts**: System-wide keyboard shortcuts using JNativeHook
- **Settings Management**: Persistent application, UI, and assistant settings
- **Theme System**: Dynamic theme switching with AtlantaFX

### Architecture Pattern
- Spring context provides services (repositories, facades, controllers)
- JavaFX UI retrieves Spring beans via `AppLauncher.getBean()`
- Event-driven communication between UI and backend using custom event bus
- Plugin functions are dynamically registered with OpenAI for AI-powered integrations

### Development Notes
- Uses Java 24 with JavaFX preview features enabled (`-Djavafx.enablePreview=true`)
- MongoDB for data persistence (threads, messages, plugins, settings)
- Global keyboard shortcuts for quick access to assistant features
- Supports multiple themes and customizable UI settings