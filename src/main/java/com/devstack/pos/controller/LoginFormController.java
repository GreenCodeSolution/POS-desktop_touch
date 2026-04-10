package com.devstack.pos.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.text.Text;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.devstack.pos.entity.AppUser;
import com.devstack.pos.service.SessionManager;
import com.devstack.pos.service.TrialService;
import com.devstack.pos.service.UserService;
import com.devstack.pos.util.JwtUtil;
import com.devstack.pos.util.StageManager;
import com.devstack.pos.util.UserSessionData;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import com.jfoenix.controls.JFXPasswordField;
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

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final SessionManager sessionManager;
    private final TrialService trialService;
    
    private AppUser selectedUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DEBUG: LoginFormController.initialize() called");
        
        // Start real-time clock
        initClock();
        
        // Reset state
        selectedUser = null;
        txtPassword.setText("");
        
        // Load active cashiers into the profile selection area
        loadCashierProfiles();
        
        // Ensure dots are initialized
        updatePinDots();

        // Set up Enter key handler for password field - trigger login
        txtPassword.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                btnSignInOnAction(new ActionEvent());
            }
        });
        
        // Automatically hide email field as we use profile selection now
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
        card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 16; -fx-padding: 10 20; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 16;");
        
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
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        
        // Extract Role (assuming the most relevant role)
        String roleName = "Cashier"; // Default
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            roleName = user.getRoles().iterator().next().getName().replace("ROLE_", "");
            roleName = roleName.substring(0, 1).toUpperCase() + roleName.substring(1).toLowerCase();
        }
        Label roleLabel = new Label(roleName);
        roleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        
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
                h.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 16; -fx-padding: 10 20; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 16;");
                
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
        card.setStyle("-fx-background-color: #eef2ff; -fx-background-radius: 16; -fx-padding: 10 20; -fx-border-color: #4f46e5; -fx-border-width: 2; -fx-border-radius: 16;");
        
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

    private void setUi(String url) throws IOException {
        Stage stage = (Stage) context.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/com/devstack/pos/view/" + url + ".fxml"));
        loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);

        Parent root = loader.load();
        StageManager.loadFullScreenScene(stage, root);
    }
}
