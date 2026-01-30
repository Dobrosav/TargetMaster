# TargetMaster

TargetMaster is a simple 3D shooting game built using JavaFX. Test your aim by hitting targets in a 3D environment!

## Features

*   **3D Environment:** Fully interactive 3D scene with skybox and floor.
*   **Sniper Mechanics:** Detailed 3D sniper rifle model with scope, bolt, and muzzle brake.
*   **Aiming & Shooting:** Mouse-controlled aiming and shooting mechanics.
*   **Scope Mode:** Right-click to zoom in with a realistic scope overlay.
*   **Sound Effects:** High-quality MP3 sniper fire sound effects using Java Sound API.
*   **Dynamic Targets:** Targets spawn at random locations after being hit.
*   **Visual Effects:** Muzzle flash, bullet trajectories, and particle effects upon impact.
*   **Scoring System:** Track your hits and misses.

## Prerequisites

*   **Java Development Kit (JDK) 21** or higher.
*   **Apache Maven** 3.x.

## Installation & Build

1.  Clone the repository:
    ```bash
    git clone https://github.com/Dobrosav/TargetMaster.git
    cd TargetMaster
    ```

2.  Build the project using Maven:
    ```bash
    mvn clean package
    ```

## Running the Application

You can run the application directly using the JavaFX Maven plugin:

```bash
mvn javafx:run
```

Alternatively, you can run the executable JAR file generated in the `target` directory (if configured with `maven-shade-plugin`):

```bash
java -jar target/TargetMaster-1.0-SNAPSHOT.jar
```
*(Note: Ensure that the JAR includes dependencies or that JavaFX modules are correctly provided on the module path if running a non-shaded JAR).*

## Controls

*   **Mouse Movement:** Aim the sniper rifle (Camera rotation).
*   **Left Mouse Button:** Fire bullet.
*   **Right Mouse Button:** Toggle Scope Mode (Zoom in/out).

## Technologies Used

*   **Java 21**
*   **JavaFX 21** (Controls, Graphics, FXML)
*   **mp3spi** (Java Sound MP3 decoding)
*   **Apache Maven**
*   **ControlsFX** & **Ikonli** (Dependencies)

## License

This project is open-source.
