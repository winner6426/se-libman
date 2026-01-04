package com.app.librarymanager.controllers;

import com.app.librarymanager.interfaces.AuthStateListener;
import com.app.librarymanager.models.User;
import com.app.librarymanager.services.PermissionService;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.AvatarUtil;
import com.app.librarymanager.utils.StageManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

public class LayoutController implements AuthStateListener {

  private static LayoutController instance;

  public static synchronized LayoutController getInstance() {
    if (instance == null) {
      instance = new LayoutController();
    }
    return instance;
  }

  private List<String> ADMIN_ROUTES = List.of("/views/admin/dashboard.fxml",
      "/views/admin/manage-books.fxml", "/views/admin/manage-users.fxml",
      "/views/admin/manage-loans.fxml", "/views/admin/manage-returns.fxml",
      "/views/admin/manage-categories.fxml", "/views/admin/manage-cards.fxml",
      "/views/admin/manage-transactions.fxml", "/views/admin/role-permissions.fxml");

  private final List<String> USER_ROUTES = List.of("/views/home.fxml", "/views/profile.fxml",
      "/views/my-card.fxml");
  @FXML
  private Button homeNavBtn;
  @FXML
  private Button allBooksNavBtn;
  @FXML
  private Button categoriesNavBtn;
  @FXML
  private Button loansNavBtn;
  @FXML
  private Button favoritesNavBtn;
  @FXML
  private Button cardNavBtn;
  @FXML
  private Button btnDashboard;
  @FXML
  private Button btnManageBooks;
  @FXML
  private Button btnManageLoans;
  @FXML
  private Button btnManageReturns;
  @FXML
  private Button btnManageRequests;
  @FXML
  private Button btnManageUsers;
  @FXML
  private Button btnManageCategories;
  @FXML
  private Button btnManageCards;
  @FXML
  private Button btnManageTransactions;
  @FXML
  private Button btnRolePermissions;
  @FXML
  private ToolBar adminToolBar;
  @FXML
  private ToolBar navBar;
  @FXML
  private StackPane contentPane;
  @FXML
  private ImageView avatarImageView;
  @FXML
  private Button logoutBtn;
  @FXML
  private TextField searchField;
  @FXML
  private ImageView navLogo;


  private Popup popup;


  private final Map<String, Parent> componentCache = new HashMap<>();

  @FXML
  private void initialize() {
    AuthController.getInstance().addAuthStateListener(this);
    AuthController.getInstance().loadSession();

    popup = new Popup();
    popup.setAutoHide(true);

    if (AuthController.getInstance().validateIdToken()) {
      User currentUser = AuthController.getInstance().getCurrentUser();
//      preloadComponents(currentUser);
      updateUI(true, currentUser);
      // Determine if user has any admin routes allowed
      var allowed = PermissionService.getInstance().getAllowedRoutes(currentUser.getRole());
      boolean hasAnyAdminAccess = currentUser.isAdmin() || allowed.stream().anyMatch(ADMIN_ROUTES::contains);
      if (hasAnyAdminAccess) {
        // If admin, load dashboard; otherwise load the first admin route the role is allowed to access
        if (currentUser.isAdmin()) {
          loadComponent("/views/admin/dashboard.fxml");
        } else {
          String first = ADMIN_ROUTES.stream().filter(allowed::contains).findFirst().orElse("/views/home.fxml");
          loadComponent(first);
        }
      } else {
        loadComponent("/views/home.fxml");
      }
    } else {
      AuthController.getInstance().logout();
//      preloadComponents(null);
      updateUI(false, null);
      loadComponent("/views/home.fxml");
    }
    searchField.setOnAction(event -> handleSearch());
    allBooksNavBtn.setOnAction(event -> handleSearch());

    homeNavBtn.setOnAction(event -> {
      if (!canAccessRoute("/views/home.fxml")) {
        AlertDialog.showAlert("error", "Access Denied", "You don't have permission to access Home.", null);
        return;
      }
      loadComponent("/views/home.fxml");
    });
    categoriesNavBtn.setOnAction(event -> {
      if (!canAccessRoute("/views/categories.fxml")) {
        AlertDialog.showAlert("error", "Access Denied", "You don't have permission to access Categories.", null);
        return;
      }
      loadComponent("/views/categories.fxml");
    });
    loansNavBtn.setOnAction(event -> {
      System.out.println("My Loans button clicked");
      // If user is not authenticated, open login window immediately (more responsive)
      if (!AuthController.getInstance().isAuthenticated() || AuthController.getInstance().getCurrentUser() == null) {
        StageManager.showLoginWindow();
        return;
      }
      if (!canAccessRoute("/views/loans.fxml")) {
        AlertDialog.showAlert("error", "Access Denied", "You don't have permission to access My Loans.", null);
        return;
      }
      loadComponent("/views/loans.fxml");
    });
    favoritesNavBtn.setOnAction(event -> {
      AuthController.requireLogin();
      if (AuthController.getInstance().isAuthenticated()) {
        if (!canAccessRoute("/views/favorite-books.fxml")) {
          AlertDialog.showAlert("error", "Access Denied", "You don't have permission to access Favorites.", null);
          return;
        }
        loadComponent("/views/favorite-books.fxml");
      }
    });
    cardNavBtn.setOnAction(event -> {
      AuthController.requireLogin();
      if (AuthController.getInstance().isAuthenticated()) {
        if (!canAccessRoute("/views/my-card.fxml")) {
          AlertDialog.showAlert("error", "Access Denied", "You don't have permission to access My Card.", null);
          return;
        }
        loadComponent("/views/my-card.fxml");
      }
    });
    navLogo.setOnMouseClicked(event -> loadComponent("/views/home.fxml"));
  }

  @FXML
  public void handleImageClick() {
    if (popup.isShowing()) {
      popup.hide();
    } else {
      Bounds boundsInScreen = avatarImageView.localToScreen(avatarImageView.getBoundsInLocal());
      popup.setOnShown(event -> {
        double popupX = boundsInScreen.getMinX() + (boundsInScreen.getWidth()) - (popup.getWidth());
        double popupY = boundsInScreen.getMaxY();
        popup.setX(popupX);
        popup.setY(popupY);
      });
      popup.show(avatarImageView, popup.getX(), popup.getY());
    }
  }

  private void preloadComponents(User user) {
    try {
      for (String route : USER_ROUTES) {
        componentCache.put(route, loadFXML(route));
      }
      if (user != null) {
        if (user.isAdmin()) {
          for (String route : ADMIN_ROUTES) {
            componentCache.put(route, loadFXML(route));
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Parent loadFXML(String fxmlPath) throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
    return loader.load();
  }

  private void updateUI(boolean isAuthenticated, User user) {
    boolean hasAdminToolbar = isAuthenticated && (user.isAdmin() || !PermissionService.getInstance().getAllowedRoutes(user.getRole()).isEmpty());
    adminToolBar.setVisible(hasAdminToolbar);
    adminToolBar.setManaged(hasAdminToolbar);
    if (isAuthenticated) {
      if (user.getAvatar() != null) {
        avatarImageView.setImage(user.getAvatar());
      } else if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
        avatarImageView.setImage(new ImageView(user.getPhotoUrl()).getImage());
      } else {
        String displayName = user.getDisplayName();
        String email = user.getEmail();
        String name = displayName != null && !displayName.isEmpty() ? displayName : email;
        avatarImageView.setImage(
            new ImageView(new AvatarUtil().getAvatarUrl(name)).getImage());
      }
      VBox popupContent = new VBox(0);
      popupContent.getStyleClass().add("popup-container");
      Button profileButton = new Button("Profile");
      profileButton.setOnAction(event -> handleProfileSettings());
      Button logoutButton = new Button("Logout");
      logoutButton.getStyleClass().add("danger");
      logoutButton.setOnAction(event -> onLogoutButtonClick());

      popupContent.getChildren().addAll(profileButton, logoutButton);
      popup.getContent().clear();
      popup.getContent().add(popupContent);

    } else {
      VBox popupContent = new VBox(0);
      popupContent.getStyleClass().add("popup-container");
      Button loginButton = new Button("Login");
      loginButton.setOnAction(event -> onLoginButtonClick());

      Button registerButton = new Button("Register");
      registerButton.setOnAction(event -> onRegisterButtonClick());

      popupContent.getChildren().addAll(loginButton, registerButton);

      popup.getContent().clear();
      popup.getContent().add(popupContent);

      avatarImageView.setImage(
          new ImageView(new AvatarUtil().getAvatarUrl("Anonymous")).getImage());
    }
    updateNavVisibility(user);
  }

  @FXML
  private void onLoginButtonClick() {
    StageManager.showLoginWindow();
  }

  private boolean isSearchComponentLoaded = false;

  private void handleSearch() {
    String keyword = searchField.getText().trim();
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/search.fxml"));
      Parent searchComponent = loader.load();
      SearchController searchController = loader.getController();
      searchController.setKeyword(keyword);
      contentPane.getChildren().clear();
      contentPane.getChildren().add(searchComponent);
      navBar.getItems().removeIf(item -> item instanceof TextField);
      isSearchComponentLoaded = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @FXML
  public void handleViewBookDetails() {
    // Show book details for selected book
  }

  @FXML
  public void handleProfileSettings() {
    loadComponent("/views/profile.fxml");
    popup.hide();
  }

  @FXML
  public void handleShowDashboard(Event e) {
    handleChangeActiveButton(e);
    loadComponent("/views/admin/dashboard.fxml");

  }

  @FXML
  public void handleManageBooks(Event e) {
    handleChangeActiveButton(e);
    loadComponent("/views/admin/manage-books.fxml");
  }

  @FXML
  public void handleManageUsers(Event e) {
    handleChangeActiveButton(e);
    loadComponent("/views/admin/manage-users.fxml");
  }

  @FXML
  void handleManageBookLoans(Event e) {
    handleChangeActiveButton(e);
    loadComponent("/views/admin/manage-loans.fxml");
  }

  @FXML
  void handleManageLoanRequests(Event e) {
    if (!canAccessAdminRoute("/views/admin/manage-loan-requests.fxml")) {
      AlertDialog.showAlert("error", "Access Denied", "You don't have permission to access Loan Requests.", null);
      return;
    }
    handleChangeActiveButton(e);
    loadComponent("/views/admin/manage-loan-requests.fxml");
  }

  // Loan Records handler removed per request

  @FXML
  void handleManageReturns(Event e) {
    if (!canAccessAdminRoute("/views/admin/manage-returns.fxml")) {
      AlertDialog.showAlert("error", "Access Denied", "You don't have permission to access Returns.", null);
      return;
    }
    handleChangeActiveButton(e);
    loadComponent("/views/admin/manage-returns.fxml");
  }

  @FXML
  public void handleManageCategories(Event e) {
    if (!canAccessAdminRoute("/views/admin/manage-categories.fxml")) {
      AlertDialog.showAlert("error", "Access Denied", "You don't have permission to access Categories.", null);
      return;
    }
    handleChangeActiveButton(e);
    loadComponent("/views/admin/manage-categories.fxml");
  }

  @FXML
  public void handleManageCards(Event e) {
    if (!canAccessAdminRoute("/views/admin/manage-cards.fxml")) {
      AlertDialog.showAlert("error", "Access Denied", "You don't have permission to access Cards.", null);
      return;
    }
    handleChangeActiveButton(e);
    loadComponent("/views/admin/manage-cards.fxml");
  }

  @FXML
  public void handleManageTransactions(Event e) {
    if (!canAccessAdminRoute("/views/admin/manage-transactions.fxml")) {
      AlertDialog.showAlert("error", "Access Denied", "You don't have permission to access Transactions.", null);
      return;
    }
    handleChangeActiveButton(e);
    loadComponent("/views/admin/manage-transactions.fxml");
  }

  @FXML
  public void handleRolePermissions(Event e) {
    if (!canAccessAdminRoute("/views/admin/role-permissions.fxml")) {
      AlertDialog.showAlert("error", "Access Denied", "You don't have permission to access Role Permissions.", null);
      return;
    }
    handleChangeActiveButton(e);
    loadComponent("/views/admin/role-permissions.fxml");
  }

  private boolean canAccessAdminRoute(String route) {
    if (!AuthController.getInstance().isAuthenticated() || AuthController.getInstance().getCurrentUser() == null) return false;
    User user = AuthController.getInstance().getCurrentUser();
    return user.isAdmin() || PermissionService.getInstance().hasAccess(user.getRole(), route);
  }

  private boolean canAccessRoute(String route) {
    // If not authenticated, treat user as GUEST and check GUEST permissions
    if (!AuthController.getInstance().isAuthenticated() || AuthController.getInstance().getCurrentUser() == null) {
      return PermissionService.getInstance().hasAccess("GUEST", route);
    }
    User user = AuthController.getInstance().getCurrentUser();
    return user.isAdmin() || PermissionService.getInstance().hasAccess(user.getRole(), route);
  }

  private void updateNavVisibility(User user) {
    String role;
    boolean isAdmin = false;
    if (user == null) {
      // Treat unauthenticated visitors as GUEST
      role = "GUEST";
      isAdmin = false;
      // admin toolbar hidden for guests by default (will be enforced again below)
    } else {
      role = user.getRole();
      isAdmin = user.isAdmin();
      // Determine if the role has any allowed admin routes or visible admin routes
    }

    // Debug prints removed

    // Determine if any staff features are visible for this role
    boolean anyStaffVisible = ADMIN_ROUTES.stream().anyMatch(r -> PermissionService.getInstance().hasVisibility(role, r));

    // If no staff feature is visible, hide the logout button and entire left toolbar
    logoutBtn.setVisible(anyStaffVisible);
    logoutBtn.setManaged(anyStaffVisible);

    // Hide the entire admin toolbar when no staff features are visible (collapse left area)
    adminToolBar.setVisible(anyStaffVisible);
    adminToolBar.setManaged(anyStaffVisible);

    // Top nav buttons
    homeNavBtn.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/home.fxml"));
    allBooksNavBtn.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/search.fxml"));
    categoriesNavBtn.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/categories.fxml"));
    loansNavBtn.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/loans.fxml"));
    favoritesNavBtn.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/favorite-books.fxml"));
    cardNavBtn.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/my-card.fxml"));
    // Admin toolbar buttons
    btnDashboard.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/admin/dashboard.fxml"));
    btnManageBooks.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/admin/manage-books.fxml"));
    btnManageLoans.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/admin/manage-loans.fxml"));
    btnManageReturns.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/admin/manage-returns.fxml"));
    btnManageRequests.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/admin/manage-loan-requests.fxml"));
    btnManageUsers.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/admin/manage-users.fxml"));
    btnManageCategories.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/admin/manage-categories.fxml"));
    btnManageCards.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/admin/manage-cards.fxml"));
    btnManageTransactions.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/admin/manage-transactions.fxml"));
    btnRolePermissions.setVisible(isAdmin || PermissionService.getInstance().hasVisibility(role, "/views/admin/role-permissions.fxml"));
  }

  @FXML
  private void onRegisterButtonClick() {
    StageManager.showRegisterWindow();
  }


  @FXML
  private void onLogoutButtonClick() {
    // Hide popup immediately
    popup.hide();

    // Perform logout first so Auth state is cleared
    AuthController.getInstance().logout();

    // Ensure search field is restored if it was loaded
    if (isSearchComponentLoaded) {
      navBar.getItems().add(2, searchField);
      searchField.clear();
      isSearchComponentLoaded = false;
    }

    // Remove any active styles on admin toolbar buttons (defensive: works even if no admin was logged in)
    adminToolBar.getItems().stream().filter(node -> node instanceof Button)
        .map(node -> (Button) node).filter(button -> button.getStyleClass().contains("active"))
        .forEach(button -> button.getStyleClass().remove("active"));

    // Immediately update UI to guest (not keeping previous user's UI state)
    updateUI(false, null);

    // Load guest home view
    loadComponent("/views/home.fxml");
  }

  @FXML
  public void closeLoginWindow() {

  }

  @Override
  public void onAuthStateChanged(boolean isAuthenticated) {
    updateUI(isAuthenticated, AuthController.getInstance().getCurrentUser());
  }

  /**
   * Re-evaluate navigation visibility for the current auth state/user.
   * Public so other controllers (e.g., RolePermissionsController) can force
   * an immediate UI refresh after permission changes.
   */
  public void refreshNavForCurrentUser() {
    updateUI(AuthController.getInstance().isAuthenticated(), AuthController.getInstance().getCurrentUser());
  }

  private void handleChangeActiveButton(Event e) {
    if (!(e.getSource() instanceof Button clickedButton)) {
      return;
    }
    adminToolBar.getItems().stream().filter(node -> node instanceof Button)
        .map(node -> (Button) node).filter(button -> button.getStyleClass().contains("active"))
        .forEach(button -> button.getStyleClass().remove("active"));

    clickedButton.getStyleClass().add("active");
  }

  private void loadComponent(String fxmlPath) {
    Task<Parent> loadTask = new Task<>() {
      @Override
      protected Parent call() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        return loader.load();
      }
    };

    loadTask.setOnSucceeded(event -> {
      Platform.runLater(() -> {
        Parent component = loadTask.getValue();
        contentPane.getChildren().clear();
        contentPane.getChildren().add(component);
        if (isSearchComponentLoaded) {
          navBar.getItems().add(2, searchField);
          searchField.clear();
          isSearchComponentLoaded = false;
        }
      });
    });

    loadTask.setOnFailed(event -> {
      Throwable t = loadTask.getException();
      System.err.println("Failed to load component: " + fxmlPath + ". Exception: " + (t != null ? t.getMessage() : "null"));
      if (t != null) t.printStackTrace();
      // Print root causes
      Throwable cause = t != null ? t.getCause() : null;
      while (cause != null) {
        System.err.println("Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
        cause.printStackTrace();
        cause = cause.getCause();
      }
      // Notify user
      Platform.runLater(() -> AlertDialog.showAlert("error", "Error", "Failed to load component: " + fxmlPath + ". See console for details.", null));
    });

    new Thread(loadTask).start();
  }
}