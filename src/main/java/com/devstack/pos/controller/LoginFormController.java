package com.devstack.pos.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.text.Text;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.devstack.pos.entity.AppUser;
import com.devstack.pos.service.AppUpdateService;
import com.devstack.pos.service.SessionManager;
import com.devstack.pos.service.TrialService;
import com.devstack.pos.service.UserService;
import com.devstack.pos.util.JwtUtil;
import com.devstack.pos.util.StageManager;
import com.devstack.pos.util.TerminalConfigManager;
import com.devstack.pos.util.UserSessionData;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import com.jfoenix.controls.JFXPasswordField;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LoginFormController implements Initializable {
    public HBox context;
    public JFXPasswordField txtPassword;
    public VBox profileContainer;
    public HBox pinDotsContainer;
    public Text lblTime;
    public Text lblDate;
    public Text lblAppVersion;
    public Text lblStoreName;
    public Text lblTerminalName;

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final SessionManager sessionManager;
    private final TrialService trialService;
    private final AppUpdateService appUpdateService;
    private final com.devstack.pos.service.TerminalService terminalService;
    
    private AppUser selectedUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG: LoginFormController.initialize() called");

        // Validate terminal status against DB
        if (!terminalService.isTerminalValid()) {
            System.err.println("Terminal is no longer valid in database. Clearing cache and redirecting to activation...");
            TerminalConfigManager.unpair();
            javafx.application.Platform.runLater(this::navigateToActivation);
            return;
        }
        
        // Start real-time clock
        initClock();
        
        // Reset state
        selectedUser = null;
        txtPassword.setText("");
        
        // Load active cashiers into the profile selection area
        loadCashierProfiles();
        lblAppVersion.setText("v" + appUpdateService.getCurrentVersion());
        
        if (TerminalConfigManager.isPaired()) {
            if (lblStoreName != null) lblStoreName.setText(TerminalConfigManager.getBusinessName());
            if (lblTerminalName != null) lblTerminalName.setText("• " + TerminalConfigManager.getTerminalName());
        }
        
        // Ensure dots are initialized
        updatePinDots();

        // Set up Enter key handler for password field - trigger login
        txtPassword.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                btnSignInOnAction(new ActionEvent());
            }
        });
        
        // Check for app updates and show confirmation popup
        checkForAppUpdate();
    }

    private void loadCashierProfiles() {
        List<AppUser> cashiers = userService.findActiveCashiers();
        profileContainer.getChildren().clear();
        
        if (cashiers.isEmpty()) {
            System.err.println("CRITICAL: No cashiers found in database! Profiles area will be empty.");
            Label warning = new Label("No profiles. Check DB for CASHIER role.");
            warning.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
            profileContainer.getChildren().add(warning);
        } else {
            for (AppUser cashier : cashiers) {
                HBox card = createProfileCard(cashier);
                profileContainer.getChildren().add(card);
            }
        }
    }

    private HBox createProfileCard(AppUser user) {
        HBox card = new HBox();
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setSpacing(15);
        card.setPrefWidth(350);
        card.setMinHeight(80);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.getStyleClass().add("profile-card");
        
        // Avatar Circle
        StackPane avatarFrame = new StackPane();
        Circle avatarCircle = new Circle(22);
        avatarCircle.setFill(Color.web("#cbd5e1"));
        
        Label initial = new Label(user.getFirstName() != null && !user.getFirstName().isEmpty() 
            ? user.getFirstName().substring(0, 1).toUpperCase() 
            : user.getEmail().substring(0, 1).toUpperCase());
        initial.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        
        avatarFrame.getChildren().addAll(avatarCircle, initial);
        
        // Info Box
        VBox infoBox = new VBox();
        infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        infoBox.setSpacing(2);
        
        Label nameLabel = new Label(user.getFirstName() != null ? user.getFirstName() : user.getEmail().split("@")[0]);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        
        // Extract Role (assuming the most relevant role)
        String roleName = "Cashier"; // Default
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            roleName = user.getRoles().iterator().next().getName().replace("ROLE_", "");
            roleName = roleName.substring(0, 1).toUpperCase() + roleName.substring(1).toLowerCase();
        }
        Label roleLabel = new Label(roleName);
        roleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");
        
        infoBox.getChildren().addAll(nameLabel, roleLabel);
        
        card.getChildren().addAll(avatarFrame, infoBox);
        
        // Selection handling
        card.setOnMouseClicked(e -> selectCashier(user, card, avatarCircle, initial, nameLabel, roleLabel));
        
        return card;
    }

    private void selectCashier(AppUser user, HBox card, Circle avatarCircle, Label initial, Label nameLabel, Label roleLabel) {
        // Reset all cards in the flowpane/vbox
        for (javafx.scene.Node node : profileContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox h = (HBox) node;
                h.getStyleClass().remove("profile-card-selected");
                if (!h.getStyleClass().contains("profile-card")) {
                    h.getStyleClass().add("profile-card");
                }
                
                StackPane frame = (StackPane) h.getChildren().get(0);
                Circle c = (Circle) frame.getChildren().get(0);
                c.setFill(Color.web("#cbd5e1"));
                Label init = (Label) frame.getChildren().get(1);
                init.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
                
                VBox info = (VBox) h.getChildren().get(1);
                Label name = (Label) info.getChildren().get(0);
                name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
                Label role = (Label) info.getChildren().get(1);
                role.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
            }
        }
        
        // Highlight selected
        selectedUser = user;
        card.getStyleClass().remove("profile-card");
        card.getStyleClass().add("profile-card-selected");
        
        avatarCircle.setFill(Color.web("#6366f1"));
        initial.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        
        // Focus password field
        txtPassword.requestFocus();
    }

    private void initClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalDateTime now = LocalDateTime.now();
            lblTime.setText(now.format(DateTimeFormatter.ofPattern("HH:mm")));
            lblDate.setText(now.format(DateTimeFormatter.ofPattern("E dd MMM yyyy")));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void updatePinDots() {
        int pinLength = txtPassword.getText().length();
        for (int i = 0; i < pinDotsContainer.getChildren().size(); i++) {
            Pane dot = (Pane) pinDotsContainer.getChildren().get(i);
            if (i < pinLength) {
                dot.setStyle("-fx-background-color: #cbd5e1; -fx-background-radius: 50; -fx-min-width: 18; -fx-min-height: 18;");
            } else {
                dot.setStyle("-fx-background-color: transparent; -fx-border-color: #cbd5e1; -fx-border-width: 2; -fx-background-radius: 50; -fx-min-width: 18; -fx-min-height: 18;");
            }
        }
    }

    /**
     * OTA Auto-Update Check.
     * 
     * On app startup, reads latest_app_version from system_settings table.
     * If a newer version exists:
     *   - If download URL is set → offers to download + install automatically
     *   - If no URL → shows notification to inform the cashier
     * 
     * If update is MANDATORY, login is blocked until the new version is installed.
     */
    private void checkForAppUpdate() {
        try {
            if (!appUpdateService.isUpdateAvailable()) {
                return; // Already on latest version
            }

            String currentVer = appUpdateService.getCurrentVersion();
            String latestVer = appUpdateService.getLatestVersion();
            String releaseNotes = appUpdateService.getReleaseNotes();
            boolean mandatory = appUpdateService.isUpdateMandatory();
            boolean hasDownloadUrl = appUpdateService.hasDownloadUrl();

            System.out.println("=== UPDATE CHECK ===");
            System.out.println("LOCAL APP VERSION: '" + currentVer + "'");
            System.out.println("DATABASE VERSION:  '" + latestVer + "'");
            System.out.println("IS NEWER? " + appUpdateService.isUpdateAvailable());
            System.out.println("====================");

            Alert updateAlert = new Alert(Alert.AlertType.CONFIRMATION);
            updateAlert.setTitle("System Update Available");
            updateAlert.setHeaderText("\uD83D\uDD04 New Version v" + latestVer + " Available");

            String message = "A new version of the POS system is available.\n\n"
                    + "Current Version: v" + currentVer + "\n"
                    + "New Version: v" + latestVer + "\n\n"
                    + "\uD83D\uDCCB What's New:\n" + releaseNotes + "\n\n";

            if (hasDownloadUrl) {
                message += "Click 'Update Now' to automatically download and install the update.";
            } else {
                message += "Please contact your administrator to install the update.";
            }

            if (mandatory) {
                message += "\n\n\u26A0\uFE0F This is a MANDATORY update.\n"
                        + "You cannot use the system until it is updated.";
            }

            updateAlert.setContentText(message);

            // Configure buttons
            ButtonType btnUpdateNow = new ButtonType(hasDownloadUrl ? "Update Now" : "OK");

            if (mandatory) {
                updateAlert.getButtonTypes().setAll(btnUpdateNow);
            } else {
                ButtonType btnLater = new ButtonType("Later", ButtonBar.ButtonData.CANCEL_CLOSE);
                updateAlert.getButtonTypes().setAll(btnUpdateNow, btnLater);
            }

            updateAlert.showAndWait().ifPresent(response -> {
                if (response == btnUpdateNow) {
                    if (hasDownloadUrl) {
                        // ===== AUTO DOWNLOAD & INSTALL =====
                        performAutoUpdate();
                    } else {
                        // No download URL — just close if mandatory
                        if (mandatory) {
                            javafx.application.Platform.exit();
                            System.exit(0);
                        }
                    }
                } else {
                    System.out.println("User deferred update to later.");
                }
            });

            // If mandatory and the user didn't update, block
            if (mandatory && !hasDownloadUrl) {
                javafx.application.Platform.exit();
                System.exit(0);
            }

        } catch (Exception e) {
            System.err.println("Update check failed (non-critical): " + e.getMessage());
        }
    }

    /**
     * Download the update .exe from the server and launch the installer.
     * Shows a progress dialog during download.
     */
    private void performAutoUpdate() {
        // Create a custom alert for download progress
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Update in Progress");
        progressAlert.setHeaderText("\u2B07\uFE0F Downloading Update...");
        
        // Remove default buttons
        progressAlert.getButtonTypes().clear();

        // Create UI components for progress
        javafx.scene.control.ProgressBar progressBar = new javafx.scene.control.ProgressBar(0);
        progressBar.setPrefWidth(350);
        progressBar.setMinHeight(20);
        
        Label statusLabel = new Label("Preparing download...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1e293b; -fx-font-weight: bold;");
        
        Label detailLabel = new Label("Connecting to server...");
        detailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        VBox contentBox = new VBox(15, statusLabel, progressBar, detailLabel);
        contentBox.setAlignment(javafx.geometry.Pos.CENTER);
        contentBox.setPadding(new javafx.geometry.Insets(20));

        progressAlert.getDialogPane().setContent(contentBox);
        progressAlert.show();

        // Download in background thread
        new Thread(() -> {
            try {
                java.nio.file.Path installerPath = appUpdateService.downloadUpdate(progress -> {
                    javafx.application.Platform.runLater(() -> {
                        if (progress < 0) {
                            // Indeterminate mode if size is unknown
                            progressBar.setProgress(javafx.scene.control.ProgressBar.INDETERMINATE_PROGRESS);
                            statusLabel.setText("Downloading... (Size unknown)");
                        } else {
                            progressBar.setProgress(progress);
                            int percent = (int) (progress * 100);
                            statusLabel.setText("Downloading... " + percent + "%");
                        }
                        detailLabel.setText("Retrieving files from cloud server...");
                    });
                });

                javafx.application.Platform.runLater(() -> {
                    // 1. Force close the progress dialog first
                    if (progressAlert.isShowing()) {
                        progressAlert.setResult(javafx.scene.control.ButtonType.OK);
                        progressAlert.close();
                    }
                    
                    // 2. Clear the dialog background artifacts
                    progressAlert.getDialogPane().getScene().getWindow().hide();

                    // 3. Show the final instruction
                    Alert launchAlert = new Alert(Alert.AlertType.INFORMATION);
                    launchAlert.setTitle("Download Complete");
                    launchAlert.setHeaderText("\u2705 Ready to Install");
                    launchAlert.setContentText("The update has been downloaded.\n\n" +
                            "IMPORTANT: The application will now close and the Windows Installer will start.\n" +
                            "Please follow the prompts in the installer window to finish the update.");
                    launchAlert.showAndWait();

                    try {
                        appUpdateService.launchInstallerAndExit(installerPath);
                    } catch (Exception e) {
                        showUpdateError("Failed to start installer: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    progressAlert.close();
                    showUpdateError("Download interrupted: " + e.getMessage());
                });
            }
        }, "OTA-Update-Thread").start();
    }

    /**
     * Show an error dialog for update failures
     */
    private void showUpdateError(String errorMessage) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("Update Failed");
        errorAlert.setHeaderText("\u274C Update Failed");
        errorAlert.setContentText(errorMessage);
        errorAlert.showAndWait();
    }

    // Signup is managed via the web portal — no create account from desktop

    @javafx.fxml.FXML
    public void btnKeypadPressed(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof javafx.scene.control.Button) {
            String text = ((javafx.scene.control.Button) source).getText();
            String current = txtPassword.getText();
            
            if (current.length() < 4) {
                txtPassword.appendText(text);
                String newPin = txtPassword.getText();
                System.out.println("DEBUG: PIN Length: " + newPin.length() + " (Selected: " + (selectedUser != null) + ")");
                updatePinDots();
                
                // Auto-submit if EXACTLY 4 digits are entered and a user is selected
                if (newPin.length() == 4) {
                    if (selectedUser != null) {
                        System.out.println("DEBUG: Auto-submitting login for: " + selectedUser.getEmail());
                        btnSignInOnAction(new ActionEvent());
                    } else {
                        System.out.println("DEBUG: 4 digits entered but no profile selected.");
                    }
                }
            }
        }
    }

    @javafx.fxml.FXML
    public void btnKeypadClear(ActionEvent event) {
        txtPassword.clear();
        updatePinDots();
        txtPassword.requestFocus();
    }

    @javafx.fxml.FXML
    public void btnKeypadBackspace(ActionEvent event) {
        String current = txtPassword.getText();
        if (!current.isEmpty()) {
            txtPassword.setText(current.substring(0, current.length() - 1));
            updatePinDots();
        }
        txtPassword.requestFocus();
    }

    public void btnSignInOnAction(ActionEvent actionEvent) {
        try {
            // Live validation: ensure terminal is still active and registered
            if (!terminalService.isTerminalValid()) {
                TerminalConfigManager.unpair();
                new Alert(Alert.AlertType.ERROR, "This register or business has been suspended or removed in the Web Admin Portal.\nTerminal disconnected.").showAndWait();
                navigateToActivation();
                return;
            }

            // Check trial version status before allowing login
            if (trialService.isTrialEnabled() && trialService.isTrialExpired()) {
                Alert trialAlert = new Alert(Alert.AlertType.ERROR);
                trialAlert.setTitle("Trial Period Expired");
                trialAlert.setHeaderText("Access Denied");
                trialAlert.setContentText(
                        "Your trial period has expired. Please contact the administrator to extend or activate the full version.\n\n"
                                +
                                "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n" +
                                "Status: " + trialService.getTrialStatusMessage());
                trialAlert.showAndWait();
                return;
            }

            AppUser userToLogin = selectedUser;
            if (userToLogin == null) {
                new Alert(Alert.AlertType.WARNING, "Please select your profile first!").show();
                return;
            }

            String jwtToken = userService.checkPassword(userToLogin.getEmail(), txtPassword.getText());
            if (!ObjectUtils.isEmpty(jwtToken)) {
                // Clear previous session data safely
                UserSessionData.clear();

                // Store JWT token
                UserSessionData.jwtToken = jwtToken;
                UserSessionData.email = userToLogin.getEmail();
                UserSessionData.businessId = TerminalConfigManager.getBusinessId();
                UserSessionData.businessName = TerminalConfigManager.getBusinessName();
                UserSessionData.terminalId = TerminalConfigManager.getTerminalId();
                UserSessionData.terminalName = TerminalConfigManager.getTerminalName();

                // Extract and store user roles from JWT token
                java.util.List<String> roles = jwtUtil.getRolesFromToken(jwtToken);
                UserSessionData.userRoles = new java.util.HashSet<>(roles);

                // Restrict access to CASHIER role ONLY for this desktop application
                if (!UserSessionData.isCashier()) {
                    UserSessionData.clear();
                    Alert accessAlert = new Alert(Alert.AlertType.ERROR);
                    accessAlert.setTitle("Access Denied");
                    accessAlert.setHeaderText("Insufficient Permissions");
                    accessAlert.setContentText("This desktop application is reserved for POS Cashiers only.\n\n" +
                            "Please log in via the Web Portal for management and administrative tasks.");
                    accessAlert.showAndWait();
                    return;
                }

                    // Extract and store granular permissions from JWT token
                    java.util.List<String> permissions = jwtUtil.getPermissionsFromToken(jwtToken);
                    UserSessionData.permissions = new java.util.HashSet<>(permissions);

                    // Initialize last activity time
                    UserSessionData.updateLastActivity();

                    // Start session monitoring for inactivity and token expiration
                    sessionManager.startSessionMonitoring();

                    System.out.println("=== LOGIN SUCCESSFUL ===");
//                    System.out.println("User: " + txtEmail.getText());
                    System.out.println("Roles from JWT: " + UserSessionData.userRoles);
                    System.out.println("Is Cashier: " + UserSessionData.isCashier());
                    System.out.println("Session monitoring started");
                    System.out.println("========================");

                    // Show trial warning ONLY if 7 days or less remaining
                    if (trialService.isTrialActive() && trialService.isTrialWarningPeriod()) {
                        long daysRemaining = trialService.getDaysRemaining();
                        Alert trialWarning = new Alert(Alert.AlertType.WARNING);
                        trialWarning.setTitle("Trial Version Warning");
                        trialWarning.setHeaderText("⚠️Trial Period Ending Soon");

                        String message;
                        if (daysRemaining == 0) {
                            message = "⚠️WARNING: Your trial period expires TODAY!\n\n" +
                                    "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n\n" +
                                    "Please contact the administrator to extend or activate the full version to avoid service interruption.";
                        } else if (daysRemaining == 1) {
                            message = "⚠️WARNING: Your trial period expires TOMORROW!\n\n" +
                                    "Days remaining: " + daysRemaining + " day\n" +
                                    "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n\n" +
                                    "Please contact the administrator to extend or activate the full version.";
                        } else {
                            message = "⚠️WARNING: Your trial period is ending soon!\n\n" +
                                    "Days remaining: " + daysRemaining + " days\n" +
                                    "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n\n" +
                                    "Please contact the administrator to extend or activate the full version before the trial expires.";
                        }

                        trialWarning.setContentText(message);
                        trialWarning.show();
                    }
                    // No popup if more than 7 days remaining

                    setUi("MainForm");
            } else {
                new Alert(Alert.AlertType.WARNING, "Invalid PIN. Please try again!").show();
                txtPassword.clear();
                updatePinDots();
            }
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @javafx.fxml.FXML
    public void btnUnpairTerminalOnAction(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Disconnect Terminal");
        confirm.setHeaderText("Unpair Register from " + TerminalConfigManager.getBusinessName());
        confirm.setContentText("Are you sure you want to disconnect this register from this business?\nYou will need the Business Code and Pairing Key to reconnect.");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            TerminalConfigManager.unpair();
            navigateToActivation();
        }
    }

    private void navigateToActivation() {
        try {
            Stage stage = com.devstack.pos.Initialize.getPrimaryStage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/devstack/pos/view/TerminalActivationForm.fxml"));
            loader.setControllerFactory(com.devstack.pos.Initialize.getSpringContext()::getBean);
            Scene scene = new Scene(loader.load());

            var cssUrl = getClass().getResource("/com/devstack/pos/view/styles/pos-styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("KumaraPOS - Terminal Setup");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setUi(String url) throws IOException {
        Stage stage = (Stage) context.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/com/devstack/pos/view/" + url + ".fxml"));
        loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);

        Parent root = loader.load();
        StageManager.loadFullScreenScene(stage, root);
    }
}
