package com.devstack.pos;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

public class Initialize extends Application {

    private static Stage primaryStage;
    private static ConfigurableApplicationContext springContext;

    @Override
    public void start(Stage primaryStage) {
        Initialize.primaryStage = primaryStage;

        // 1. Show Splash Screen Immediately
        Stage splashStage = showSplashScreen();

        // 2. Start Spring Boot in a background thread
        new Thread(() -> {
            try {
                System.out.println(">>> Starting Spring Boot in background...");

                springContext = new SpringApplicationBuilder(PosApplication.class)
                        .headless(false)
                        .web(org.springframework.boot.WebApplicationType.NONE)
                        .run();

                System.out.println(">>> Spring ready! Switching to Login screen...");

                // 3. Switch to Login Screen on the JavaFX thread
                Platform.runLater(() -> {
                    try {
                        loadLoginScreen();
                        splashStage.hide(); // Hide the splash screen
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("FAILED TO START SPRING: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                });
            }
        }).start();
    }

    private Stage showSplashScreen() {
        Stage splash = new Stage(StageStyle.UNDECORATED);

        // Use your logo if it exists, otherwise use a placeholder
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color: white; -fx-padding: 40; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #e2e8f0; -fx-border-width: 1;");
        root.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.1)));

        Label title = new Label("KumaraPOS");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label subTitle = new Label("Connecting to Cloud Database...");
        subTitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        ProgressBar pb = new ProgressBar();
        pb.setPrefWidth(300);
        pb.setProgress(ProgressBar.INDETERMINATE_PROGRESS); // "Bouncing" progress bar

        root.getChildren().addAll(title, subTitle, pb);

        Scene scene = new Scene(root, 450, 300);
        scene.setFill(Color.TRANSPARENT);

        splash.setScene(scene);
        splash.centerOnScreen();
        splash.show();

        return splash;
    }

    private void loadLoginScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        var fxmlUrl = getClass().getResource("/com/devstack/pos/view/LoginForm.fxml");

        if (fxmlUrl == null) {
            throw new IOException("Could not find LoginForm.fxml");
        }

        loader.setLocation(fxmlUrl);
        loader.setControllerFactory(springContext::getBean);

        Scene scene = new Scene(loader.load());

        // Load CSS
        var cssUrl = getClass().getResource("/com/devstack/pos/view/styles/pos-styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setScene(scene);
        primaryStage.setTitle("KumaraPOS - Point of Sale");
        primaryStage.setMaximized(true);
        primaryStage.show();
        primaryStage.toFront();
    }

    @Override
    public void stop() throws Exception {
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
        System.exit(0);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }
}
