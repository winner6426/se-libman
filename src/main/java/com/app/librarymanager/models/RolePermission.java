package com.app.librarymanager.models;

import java.util.List;
import java.util.Date;
import lombok.Data;
import org.bson.Document;

@Data
public class RolePermission {
  private String role;
  private List<String> routes;
  private List<String> visibleRoutes;
  private Date updatedAt;

  public RolePermission() {}

  public RolePermission(String role, List<String> routes) {
    this.role = role;
    this.routes = routes;
    this.updatedAt = new Date();
  }

  public RolePermission(Document doc) {
    this.role = doc.getString("role");
    this.routes = doc.getList("routes", String.class);
    this.visibleRoutes = doc.getList("visibleRoutes", String.class);
    this.updatedAt = doc.getDate("updatedAt");
  }

  public Document toDocument() {
    Document d = new Document();
    d.append("role", role);
    d.append("routes", routes);
    d.append("visibleRoutes", visibleRoutes);
    d.append("updatedAt", new Date());
    return d;
  }

  // Explicit getters to avoid relying on Lombok processing in all build environments
  public String getRole() {
    return role;
  }

  public List<String> getRoutes() {
    return routes;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public List<String> getVisibleRoutes() {
    return visibleRoutes;
  }

  public void setVisibleRoutes(List<String> visibleRoutes) {
    this.visibleRoutes = visibleRoutes;
  }
}
