package tn.ZeroS.FXMLPreviewer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FXMLPreviewerApp extends Application {

    private TextArea codeEditor;
    private BorderPane previewPane;
    private Stage primaryStage;
    private File currentFile;
    private boolean autoRefresh = true;
    private final AtomicLong lastModification = new AtomicLong();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private WatchService watchService;
    private Path watchedDirectory;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("FXML Live Previewer");

        // Set up the main UI components
        BorderPane root = new BorderPane();
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);

        // Code editor setup
        codeEditor = new TextArea();
        codeEditor.setWrapText(false);
        codeEditor.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 12px;");

        // Preview pane setup
        previewPane = new BorderPane();
        previewPane.setPadding(new Insets(10));

        // Split pane setup
        splitPane.getItems().addAll(codeEditor, new ScrollPane(previewPane));
        splitPane.setDividerPositions(0.5);

        // Toolbar setup
        HBox toolbar = createToolbar();

        // Layout setup
        root.setTop(toolbar);
        root.setCenter(splitPane);

        // Create the scene
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);

        // Set up auto-refresh
        setupAutoRefresh();

        // Show the application
        primaryStage.show();

        // Setup file watcher
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            showErrorDialog("Could not initialize file watcher: " + e.getMessage());
        }
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));

        Button openButton = new Button("Open");
        Button saveButton = new Button("Save");
        Button refreshButton = new Button("Refresh Preview");
        CheckBox autoRefreshToggle = new CheckBox("Auto Refresh");
        autoRefreshToggle.setSelected(autoRefresh);
        Label statusLabel = new Label("Ready");

        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        openButton.setOnAction(e -> openFile());
        saveButton.setOnAction(e -> saveFile());
        refreshButton.setOnAction(e -> refreshPreview());
        autoRefreshToggle.setOnAction(e -> {
            autoRefresh = autoRefreshToggle.isSelected();
            if (autoRefresh && currentFile != null) {
                setupFileWatcher();
            }
        });

        toolbar.getChildren().addAll(openButton, saveButton, refreshButton, autoRefreshToggle, statusLabel);
        return toolbar;
    }

    private void openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open FXML File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("FXML Files", "*.fxml"));

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            loadFile(selectedFile);
        }
    }

    private void loadFile(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            currentFile = file;
            codeEditor.setText(content);
            primaryStage.setTitle("FXML Live Previewer - " + file.getName());
            refreshPreview();
            setupFileWatcher();
        } catch (IOException e) {
            showErrorDialog("Could not load file: " + e.getMessage());
        }
    }

    private void saveFile() {
        if (currentFile != null) {
            try {
                Files.write(currentFile.toPath(), codeEditor.getText().getBytes());
                refreshPreview();
            } catch (IOException e) {
                showErrorDialog("Could not save file: " + e.getMessage());
            }
        } else {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save FXML File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("FXML Files", "*.fxml"));

            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                try {
                    Files.write(file.toPath(), codeEditor.getText().getBytes());
                    currentFile = file;
                    primaryStage.setTitle("FXML Live Previewer - " + file.getName());
                    refreshPreview();
                    setupFileWatcher();
                } catch (IOException e) {
                    showErrorDialog("Could not save file: " + e.getMessage());
                }
            }
        }
    }

    private void refreshPreview() {
        if (codeEditor.getText().trim().isEmpty()) {
            return;
        }

        try {
            // Prepare input stream from editor content
            InputStream fxmlStream = new ByteArrayInputStream(codeEditor.getText().getBytes());

            // Load the FXML
            FXMLLoader loader = new FXMLLoader();
            Parent root = loader.load(fxmlStream);

            // Update the preview pane
            Platform.runLater(() -> {
                previewPane.getChildren().clear();
                previewPane.setCenter(root);
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                previewPane.getChildren().clear();
                TextArea errorArea = new TextArea("FXML Error: " + e.getMessage());
                errorArea.setEditable(false);
                errorArea.setWrapText(true);
                previewPane.setCenter(errorArea);
            });
        }
    }

    private void setupAutoRefresh() {
        // Watch for changes in the text editor
        codeEditor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!autoRefresh) return;

            long currentTime = System.currentTimeMillis();
            lastModification.set(currentTime);

            // Debounce refresh to prevent constant reloading during typing
            executor.schedule(() -> {
                if (lastModification.get() == currentTime) {
                    Platform.runLater(this::refreshPreview);
                }
            }, 500, TimeUnit.MILLISECONDS);
        });
    }

    private void setupFileWatcher() {
        if (!autoRefresh || currentFile == null) return;

        // Cancel any existing watch
        if (watchedDirectory != null) {
            try {
                watchService.close();
                watchService = FileSystems.getDefault().newWatchService();
            } catch (IOException e) {
                showErrorDialog("Error resetting file watcher: " + e.getMessage());
                return;
            }
        }

        watchedDirectory = currentFile.getParentFile().toPath();

        try {
            // Register the directory with the watch service
            watchedDirectory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            // Start watch thread
            Thread watchThread = new Thread(() -> {
                try {
                    while (true) {
                        WatchKey key = watchService.take();

                        for (WatchEvent<?> event : key.pollEvents()) {
                            Path changedPath = (Path) event.context();
                            if (currentFile.getName().equals(changedPath.toString())) {
                                // File was changed externally
                                Platform.runLater(() -> {
                                    try {
                                        String content = new String(Files.readAllBytes(currentFile.toPath()));
                                        codeEditor.setText(content);
                                    } catch (IOException e) {
                                        showErrorDialog("Could not reload file: " + e.getMessage());
                                    }
                                });
                            }
                        }

                        if (!key.reset()) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    // Thread was interrupted, exit
                }
            });
            watchThread.setDaemon(true);
            watchThread.start();

        } catch (IOException e) {
            showErrorDialog("Could not watch file for changes: " + e.getMessage());
        }
    }

    private void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        executor.shutdown();
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing watch service: " + e.getMessage());
        }
    }
}