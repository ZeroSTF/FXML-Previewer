# FXML Live Previewer

A lightweight JavaFX application for real-time previewing and editing of FXML files.

## Features

- Live preview of FXML files as you edit
- Auto-refresh on content changes
- External file change detection
- Fast and lightweight interface
- Simple and intuitive UI

## Requirements

- Java 17 or higher
- JavaFX 19 or higher

## Building the Application

### Build with Maven

To build the application:

```bash
mvn clean package
```

This will create:
- A runnable JAR file in the `target` directory
- An executable (.exe) file for Windows
- An installer package for Windows

### Running the Application

You can run the application in several ways:

1. Using the Maven JavaFX plugin:
```bash
mvn javafx:run
```

2. Using the generated JAR file:
```bash
java -jar target/fxml-previewer-1.0.0.jar
```

3. Using the generated executable (Windows):
```
target/FXMLPreviewer.exe
```

4. Using the installer (Windows):
    - Run the installer from the target directory
    - Follow the installation wizard

## Usage

1. Open the application
2. Click the "Open" button to load an existing FXML file, or start typing in the editor
3. The preview will update automatically as you edit
4. Use the "Save" button to save your changes
5. Toggle "Auto Refresh" to enable/disable automatic preview updates

## Project Structure

```
fxml-previewer/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── fxmlpreviewer/
│       │           └── FXMLPreviewerApp.java
│       └── resources/
│           └── icon.ico
└── pom.xml
```

## Customization

You can easily customize the application by:
- Modifying the editor styling in `FXMLPreviewerApp.java`
- Changing the auto-refresh timing
- Adding additional editor features
