package com.app.librarymanager.services;

import com.app.librarymanager.controllers.RolePermissionController;
import com.app.librarymanager.models.RolePermission;
import com.app.librarymanager.models.Role;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionService {
  private static PermissionService instance;
  private Map<String, List<String>> allowedCache = new HashMap<>();
  private Map<String, List<String>> visibleCache = new HashMap<>();

  private PermissionService() {
    refresh();
  }

  public static synchronized PermissionService getInstance() {
    if (instance == null) instance = new PermissionService();
    return instance;
  }

  public synchronized void refresh() {
    allowedCache.clear();
    visibleCache.clear();
    List<RolePermission> all = RolePermissionController.listAll();
    for (RolePermission rp : all) {
      String r = rp.getRole() == null ? "" : rp.getRole().trim().toUpperCase();
      List<String> a = rp.getRoutes() == null ? Collections.emptyList() : rp.getRoutes();
      List<String> v = rp.getVisibleRoutes() == null ? Collections.emptyList() : rp.getVisibleRoutes();
      allowedCache.put(r, a);
      visibleCache.put(r, v);
    }
  }

  public boolean hasAccess(String role, String route) {
    if (role == null || route == null) return false;
    String r = role.trim().toUpperCase();
    List<String> allowed = allowedCache.getOrDefault(r, Collections.emptyList());
    if (allowed.contains(route)) return true;
    // Fallback: if role has no explicit access, inherit from USER
    if (!"USER".equals(r)) {
      List<String> userAllowed = allowedCache.getOrDefault("USER", Collections.emptyList());
      return userAllowed.contains(route);
    }
    return false;
  }

  public List<String> getAllowedRoutes(String role) {
    if (role == null) return Collections.emptyList();
    String r = role.trim().toUpperCase();
    List<String> allowed = allowedCache.getOrDefault(r, Collections.emptyList());
    if (allowed.isEmpty() && !"USER".equals(r)) {
      return allowedCache.getOrDefault("USER", Collections.emptyList());
    }
    return allowed;
  }

  public boolean hasVisibility(String role, String route) {
    if (role == null || route == null) return false;
    String r = role.trim().toUpperCase();
    List<String> visible = visibleCache.getOrDefault(r, Collections.emptyList());
    if (visible.contains(route)) return true;
    // Fallback to USER visible routes when none specified for the role
    if (!"USER".equals(r)) {
      List<String> userVisible = visibleCache.getOrDefault("USER", Collections.emptyList());
      return userVisible.contains(route);
    }
    return false;
  }

  public List<String> getVisibleRoutes(String role) {
    if (role == null) return Collections.emptyList();
    String r = role.trim().toUpperCase();
    List<String> visible = visibleCache.getOrDefault(r, Collections.emptyList());
    if (visible.isEmpty() && !"USER".equals(r)) {
      return visibleCache.getOrDefault("USER", Collections.emptyList());
    }
    return visible;
  }
}
