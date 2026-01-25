# Project Overview

This project is a desktop application named "AlInaDesktop". It is built using JavaFX for the user interface and Spring Boot for the backend. The application integrates with AI services, likely providing a chat-based interface for interacting with AI models.

**Key Technologies:**

*   **Backend:** Spring Boot
*   **Frontend:** JavaFX
*   **Language:** Kotlin, Java
*   **Build Tool:** Gradle
*   **AI:** OpenAI, LangChain4j

**Architecture:**

The application follows a client-server architecture, where the JavaFX client communicates with the Spring Boot backend. The backend, in turn, interacts with external AI services. The application is launched through the `com.patres.alina.AppLauncher` class, which initializes the Spring Boot application (`com.patres.alina.server.ServerApplication`) and the JavaFX user interface.

# Building and Running

**Build:**

To build the project, run the following command in the root directory:

```bash
./gradlew build
```

**Run:**

To run the application, use the following command:

```bash
./gradlew run
```

**TODO:** Add information about running tests.

# Development Conventions

**Coding Style:**

The project uses both Kotlin and Java. The existing code should be used as a reference for coding style and conventions.

**Testing:**

**TODO:** Add information about the testing practices and frameworks used in the project.

**Contribution Guidelines:**

**TODO:** Add information about the contribution guidelines for this project.
