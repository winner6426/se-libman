package com.app.librarymanager.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import javax.imageio.ImageIO;
import org.json.JSONObject;

public class UploadFileUtil {

  public static JSONObject uploadFile(String filePath, String name) {
    if (filePath == null || filePath.isEmpty()) {
      return new JSONObject().put("success", false).put("message", "File path is required");
    }
    File file = new File(filePath);
    String fileName;
    if (name == null || name.isEmpty()) {
      fileName = validateName(file.getName());
    } else {
      fileName = validateName(name + "." + file.getName().split("\\.")[1]);
    }
    String urlString = "https://rdrive.serv00.net/upload?fileName=" + fileName;

    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(urlString))
          .header("Content-Type", "application/octet-stream")
          .POST(BodyPublishers.ofFile(file.toPath()))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      //  System.out.println("Response Code: " + response.statusCode());
      //  System.out.println("Response Body: " + response.body());

      return new JSONObject(response.body());

    } catch (Exception e) {
      e.printStackTrace();
      return new JSONObject().put("success", false).put("message", e.getMessage());
    }
  }

  public static JSONObject uploadImage(String filePath, String name, int cropSize) {
    if (filePath == null || filePath.isEmpty()) {
      return new JSONObject().put("success", false).put("message", "File path is required");
    }
    File file = new File(filePath);
    String fileName;
    if (name == null || name.isEmpty()) {
      fileName = validateName(file.getName());
    } else {
      fileName = validateName(name + "." + file.getName().split("\\.")[1]);
    }

    try {
      BufferedImage croppedImage = cropImage(file, cropSize);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(croppedImage,
          file.getName().split("\\.")[1].equals("png") ? "png" : "jpeg"
          , baos);
      byte[] imageBytes = baos.toByteArray();

      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create("https://rdrive.serv00.net/upload?fileName=" + fileName))
          .header("Content-Type", "image/jpeg")
          .POST(BodyPublishers.ofByteArray(imageBytes))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      //  System.out.println("Response Code: " + response.statusCode());
      //  System.out.println("Response Body: " + response.body());

      return new JSONObject(response.body());

    } catch (Exception e) {
      e.printStackTrace();
      return new JSONObject().put("success", false)
          .put("message", "Error in uploading image: " + e.getMessage());
    }
  }

  private static String validateName(String name) {
    return name.replaceAll("[^a-zA-Z0-9.-]", "_");
  }

  public static BufferedImage cropImage(File file, int size) throws IOException {
    BufferedImage originalImage = ImageIO.read(file);
    int width = originalImage.getWidth();
    int height = originalImage.getHeight();

    if (width > height) {
      height = size;
      width = (int) ((double) originalImage.getWidth() / originalImage.getHeight() * size);
    } else {
      width = size;
      height = (int) ((double) originalImage.getHeight() / originalImage.getWidth() * size);
    }

    BufferedImage resizedImage = new BufferedImage(width, height, originalImage.getType());
    resizedImage.getGraphics().drawImage(originalImage, 0, 0, width, height, null);

    return resizedImage.getSubimage(
        (width - size) / 2,
        (height - size) / 2,
        size,
        size
    );
  }

  public static BufferedImage cropImage(File file) throws IOException {
    BufferedImage
        originalImage = ImageIO.read(file);
    int size = Math.min(originalImage.getWidth(), originalImage.getHeight());
    return originalImage.getSubimage(
        (originalImage.getWidth() - size) / 2,
        (originalImage.getHeight() - size) / 2,
        size,
        size
    );
  }

}