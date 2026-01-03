package com.app.librarymanager.controllers;

import com.app.librarymanager.models.RolePermission;
import com.app.librarymanager.controllers.RolePermissionController;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.models.Role;
import com.app.librarymanager.services.PermissionService;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import org.bson.Document;

public class RolePermissionsController extends ControllerWithLoader {

  @FXML
  private ComboBox<String> roleCombo;
  @FXML
  private TableView<FeatureRow> usersTable;
  @FXML
  private TableColumn<FeatureRow, String> usersFeatureColumn;
  @FXML
  private TableColumn<FeatureRow, Boolean> usersAccessColumn;
  @FXML
  private TableColumn<FeatureRow, Boolean> usersVisibleColumn;

  @FXML
  private TableView<FeatureRow> staffTable;
  @FXML
  private TableColumn<FeatureRow, String> staffFeatureColumn;
  @FXML
  private TableColumn<FeatureRow, Boolean> staffAccessColumn;
  @FXML
  private TableColumn<FeatureRow, Boolean> staffVisibleColumn;

  private static class RouteEntry {
    final String id;
    final String label;

    RouteEntry(String id, String label) {
      this.id = id;
      this.label = label;
    }
  }

  private final List<RouteEntry> ALL_ROUTES = List.of(
      new RouteEntry("/views/home.fxml", "Home"),
      new RouteEntry("/views/search.fxml", "All Books"),
      new RouteEntry("/views/loans.fxml", "My Loans"),
      new RouteEntry("/views/my-card.fxml", "My Card"),
      new RouteEntry("/views/favorite-books.fxml", "My Favorites"),
      new RouteEntry("/views/admin/dashboard.fxml", "Admin Dashboard"),
      new RouteEntry("/views/admin/manage-books.fxml", "Manage Books"),
      new RouteEntry("/views/admin/manage-users.fxml", "Manage Users"),
      new RouteEntry("/views/admin/manage-loans.fxml", "Manage Loans"),
      new RouteEntry("/views/admin/manage-returns.fxml", "Manage Returns"),
      new RouteEntry("/views/admin/manage-categories.fxml", "Manage Categories"),
      new RouteEntry("/views/admin/manage-cards.fxml", "Manage Cards"),
      new RouteEntry("/views/admin/manage-loan-requests.fxml", "Manage Requests"),
      new RouteEntry("/views/admin/manage-loan-records.fxml", "Manage Loan Records"),
      new RouteEntry("/views/admin/role-permissions.fxml", "Role Permissions")
  );

      private final List<RouteEntry> USERS_FEATURES = List.of(
        new RouteEntry("/views/home.fxml", "Home"),
        new RouteEntry("/views/search.fxml", "All Books"),
        new RouteEntry("/views/loans.fxml", "My Loans"),
        new RouteEntry("/views/categories.fxml", "Categories"),
        new RouteEntry("/views/favorite-books.fxml", "My Favorites"),
        new RouteEntry("/views/my-card.fxml", "My Card")
      );

      private final List<RouteEntry> STAFF_FEATURES = List.of(
        new RouteEntry("/views/admin/dashboard.fxml", "Dashboard"),
        new RouteEntry("/views/admin/manage-books.fxml", "Books"),
        new RouteEntry("/views/admin/manage-loans.fxml", "Loans"),
        new RouteEntry("/views/admin/manage-returns.fxml", "Returns"),
        new RouteEntry("/views/admin/manage-loan-requests.fxml", "Requests"),
        new RouteEntry("/views/admin/manage-users.fxml", "Users"),
        new RouteEntry("/views/admin/manage-categories.fxml", "Categories"),
        new RouteEntry("/views/admin/manage-cards.fxml", "Cards"),
        new RouteEntry("/views/admin/role-permissions.fxml", "Role Permissions")
      );

  @FXML
  private void initialize() {
    try {
      roleCombo.getItems().addAll("GUEST", "USER", "READER", "LIBRARIAN", "ADMIN", "ACCOUNTANT");
      roleCombo.setOnAction(e -> loadRoutesForSelectedRole());
      roleCombo.setValue("USER");

      // Setup table columns and make tables editable so checkboxes toggle
      usersTable.setEditable(true);
      staffTable.setEditable(true);

      usersFeatureColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().label));
      usersFeatureColumn.setEditable(false);

      usersAccessColumn.setCellValueFactory(cell -> cell.getValue().accessProperty());
      usersAccessColumn.setCellFactory(javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(usersAccessColumn));
      usersAccessColumn.setEditable(true);

      usersVisibleColumn.setCellValueFactory(cell -> cell.getValue().visibleProperty());
      usersVisibleColumn.setCellFactory(javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(usersVisibleColumn));
      usersVisibleColumn.setEditable(true);

      staffFeatureColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().label));
      staffFeatureColumn.setEditable(false);

      staffAccessColumn.setCellValueFactory(cell -> cell.getValue().accessProperty());
      staffAccessColumn.setCellFactory(javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(staffAccessColumn));
      staffAccessColumn.setEditable(true);

      staffVisibleColumn.setCellValueFactory(cell -> cell.getValue().visibleProperty());
      staffVisibleColumn.setCellFactory(javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(staffVisibleColumn));
      staffVisibleColumn.setEditable(true);

      loadRoutesForSelectedRole();
    } catch (Throwable t) {
      System.err.println("RolePermissionsController.initialize: failed: " + (t == null ? "null" : t.getMessage()));
      t.printStackTrace();
      AlertDialog.showAlert("error", "Error", "Failed to initialize Role Permissions view: " + t.getMessage(), null);
    }
  }

  private void loadRoutesForSelectedRole() {
    try {
      String role = roleCombo.getValue();
      List<String> allowed;
      try {
        RolePermission rp = RolePermissionController.getByRole(role);
        allowed = (rp != null && rp.getRoutes() != null) ? rp.getRoutes() : PermissionService.getInstance().getAllowedRoutes(role);
      } catch (Throwable t) {
        allowed = PermissionService.getInstance().getAllowedRoutes(role);
      }
      List<String> visible;
      try {
        RolePermission rp2 = RolePermissionController.getByRole(role);
        visible = (rp2 != null && rp2.getVisibleRoutes() != null) ? rp2.getVisibleRoutes() : PermissionService.getInstance().getVisibleRoutes(role);
      } catch (Throwable t) {
        visible = PermissionService.getInstance().getVisibleRoutes(role);
      }

      javafx.collections.ObservableList<FeatureRow> urows = javafx.collections.FXCollections.observableArrayList();
      for (RouteEntry route : USERS_FEATURES) {
        FeatureRow fr = new FeatureRow(route.id, route.label, allowed != null && allowed.contains(route.id), visible != null && visible.contains(route.id));
        urows.add(fr);
      }
      usersTable.setItems(urows);

      javafx.collections.ObservableList<FeatureRow> srows = javafx.collections.FXCollections.observableArrayList();
      for (RouteEntry route : STAFF_FEATURES) {
        FeatureRow fr = new FeatureRow(route.id, route.label, allowed != null && allowed.contains(route.id), visible != null && visible.contains(route.id));
        srows.add(fr);
      }
      staffTable.setItems(srows);
    } catch (Throwable t) {
      System.err.println("RolePermissionsController.loadRoutesForSelectedRole failed: " + (t == null ? "null" : t.getMessage()));
      t.printStackTrace();
      AlertDialog.showAlert("error", "Error", "Failed to load role routes: " + t.getMessage(), null);
    }
  }

  @FXML
  private void onRefresh() {
    PermissionService.getInstance().refresh();
    loadRoutesForSelectedRole();
  }

  @FXML
  private void onSave() {
    try {
      String role = roleCombo.getValue();
      List<String> routes = new ArrayList<>();
      List<String> routesVisible = new ArrayList<>();

      for (FeatureRow fr : usersTable.getItems()) {
        if (fr.getAccess()) routes.add(fr.getId());
        if (fr.getVisible()) routesVisible.add(fr.getId());
      }
      for (FeatureRow fr : staffTable.getItems()) {
        if (fr.getAccess()) routes.add(fr.getId());
        if (fr.getVisible()) routesVisible.add(fr.getId());
      }

      RolePermission rp = new RolePermission(role, routes);
      rp.setVisibleRoutes(routesVisible);
      boolean ok = com.app.librarymanager.controllers.RolePermissionController.upsert(rp);
      if (ok) {
        PermissionService.getInstance().refresh();
        // Debug: log what was saved and what the PermissionService now has
        System.err.println("RolePermissionsController.onSave: saved role=" + role + " routes=" + routes + " visible=" + routesVisible);
        System.err.println("PermissionService.getVisibleRoutes(" + role + ")=" + PermissionService.getInstance().getVisibleRoutes(role));
        // Read back directly from DB to verify persistence
        try {
          RolePermission rpCheck = RolePermissionController.getByRole(role);
          System.err.println("RolePermissionsController.onSave: db getByRole visibleRoutes=" + (rpCheck == null ? "null" : rpCheck.getVisibleRoutes()));
        } catch (Throwable ignore) {}
        // Immediately refresh nav visibility for the current user so changes are visible
        try {
          LayoutController.getInstance().refreshNavForCurrentUser();
        } catch (Throwable ignore) {}
        AlertDialog.showAlert("success", "Saved", "Permissions saved successfully", null);
      } else {
        AlertDialog.showAlert("error", "Error", "Failed to save permissions", null);
      }
    } catch (Throwable t) {
      System.err.println("RolePermissionsController.onSave failed: " + (t == null ? "null" : t.getMessage()));
      t.printStackTrace();
      AlertDialog.showAlert("error", "Error", "Failed to save permissions: " + t.getMessage(), null);
    }
  }
}

  // Helper model for TableView rows
  class FeatureRow {
    final String id;
    final String label;
    final javafx.beans.property.SimpleBooleanProperty access = new javafx.beans.property.SimpleBooleanProperty(false);
    final javafx.beans.property.SimpleBooleanProperty visible = new javafx.beans.property.SimpleBooleanProperty(false);

    FeatureRow(String id, String label, boolean access, boolean visible) {
      this.id = id;
      this.label = label;
      this.access.set(access);
      this.visible.set(visible);
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public boolean getAccess() { return access.get(); }
    public boolean getVisible() { return visible.get(); }
    public javafx.beans.property.BooleanProperty accessProperty() { return access; }
    public javafx.beans.property.BooleanProperty visibleProperty() { return visible; }
  }
