package com.devstack.pos.util;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Utility class for managing stage/window configurations
 * Provides consistent UI sizing across the application
 */
public class StageManager {

    // Login and Signup window size
    private static final double AUTH_WINDOW_WIDTH = 600;
    private static final double AUTH_WINDOW_HEIGHT = 700;

    // Smooth transition flag
    private static final boolean enableTransitions = true;

    /**
     * Set scene to full screen (for main application screens)
     */
    public static void setFullScreen(Stage stage, Scene scene) {
        // Set stage properties
        stage.setScene(scene);
        stage.setMaximized(true);

        // Optional: Set title
        if (stage.getTitle() == null || stage.getTitle().isEmpty()) {
            stage.setTitle("Kumara Enterprises POS System");
        }
    }

    /**
     * Set scene to auth screen size (for login/signup)
     */
    public static void setAuthScreenSize(Stage stage, Scene scene) {
        stage.setScene(scene);
        stage.setWidth(AUTH_WINDOW_WIDTH);
        stage.setHeight(AUTH_WINDOW_HEIGHT);
        stage.setResizable(false);
        stage.centerOnScreen();

        if (stage.getTitle() == null || stage.getTitle().isEmpty()) {
            stage.setTitle("Kumara Enterprises POS System - Authentication");
        }
    }

    /**
     * Load scene with full screen (optimized for fast navigation)
     */
    public static void loadFullScreenScene(Stage stage, Parent root) {
        Scene currentScene = stage.getScene();
        if (currentScene != null) {
            // Keep window completely stable, just hot-swap the internal UI controls
            currentScene.setRoot(root);
            loadCSS(currentScene);
        } else {
            Scene scene = new Scene(root);
            loadCSS(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
        }

        if (stage.getTitle() == null || stage.getTitle().isEmpty()) {
            stage.setTitle("Kumara Enterprises POS System");
        }
    }

    /**
     * Load scene with auth screen size
     */
    public static void loadAuthScene(Stage stage, Parent root) {
        Scene scene = new Scene(root);
        loadCSS(scene);
        setAuthScreenSize(stage, scene);
    }

    /**
     * Load CSS stylesheet into scene
     */
    private static void loadCSS(Scene scene) {
        try {
            var cssUrl = StageManager.class.getResource("/com/devstack/pos/view/styles/pos-styles.css");
            if (cssUrl != null) {
                String cssExternalForm = cssUrl.toExternalForm();
                if (!scene.getStylesheets().contains(cssExternalForm)) {
                    scene.getStylesheets().add(cssExternalForm);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load CSS: " + e.getMessage());
        }
    }

    /**
     * Set dialog window size (for popup dialogs)
     */
    public static void setDialogSize(Stage stage, Scene scene, double width, double height) {
        stage.setScene(scene);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setResizable(false);
        stage.centerOnScreen();
    }
}
