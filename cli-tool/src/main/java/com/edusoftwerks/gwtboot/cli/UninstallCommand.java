package com.edusoftwerks.gwtboot.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
  name = "uninstall",
  description = "Uninstall GWT Boot CLI from your system"
)
public class UninstallCommand implements Callable<Integer> {
  @Option(names = { "-y", "--yes" }, description = "Skip confirmation prompt")
  private boolean skipConfirmation;

  @Override
  public Integer call() throws Exception {
    Console.info("GWT Boot CLI Uninstaller");
    Console.info("");

    // Detect installation locations
    List<InstallLocation> installations = detectInstallations();

    if (installations.isEmpty()) {
      Console.error("No GWT Boot CLI installation detected.");
      Console.info("");
      Console.info("Checked locations:");
      for (String path : getCheckPaths()) {
        Console.info("  - " + path);
      }
      return 1;
    }

    Console.info("Found GWT Boot CLI installation(s):");
    Console.info("");
    for (InstallLocation loc : installations) {
      Console.info("  " + loc.description);
      for (String file : loc.files) {
        Console.info("    - " + file);
      }
    }
    Console.info("");

    // Confirm uninstall
    if (!skipConfirmation) {
      Console.warning(
        "This will permanently remove GWT Boot CLI from your system."
      );
      String response = InputReader.readLine(
        "Do you want to continue? (yes/no): "
      );
      if (
        !response.equalsIgnoreCase("yes") && !response.equalsIgnoreCase("y")
      ) {
        Console.info("Uninstall cancelled.");
        return 0;
      }
      Console.info("");
    }

    // Perform uninstall
    boolean success = true;
    for (InstallLocation loc : installations) {
      Console.info("Removing " + loc.description + "...");
      for (String filePath : loc.files) {
        try {
          Path path = Paths.get(filePath);
          if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
              deleteDirectory(path);
            } else {
              Files.delete(path);
            }
            Console.success("  Deleted: " + filePath);
          }
        } catch (IOException e) {
          Console.error("  Failed to delete: " + filePath);
          Console.error("  Error: " + e.getMessage());
          success = false;
        }
      }
    }

    Console.info("");
    if (success) {
      Console.success("GWT Boot CLI has been successfully uninstalled!");
      Console.info("");
      Console.info(
        "Note: You may want to remove the following from your PATH:"
      );
      Console.info("  - /usr/local/bin");
      Console.info("  - ~/.local/bin");
      Console.info(
        "  - %USERPROFILE%\\AppData\\Local\\gwt-boot\\bin (Windows)"
      );
    } else {
      Console.warning("Uninstall completed with some errors.");
      Console.info(
        "You may need to manually remove remaining files with elevated permissions."
      );
    }

    return success ? 0 : 1;
  }

  private List<InstallLocation> detectInstallations() {
    List<InstallLocation> installations = new ArrayList<>();
    String home = System.getProperty("user.home");
    String os = System.getProperty("os.name").toLowerCase();

    if (os.contains("win")) {
      // Windows installation
      checkAndAdd(
        installations,
        "Windows User Installation",
        home + "\\AppData\\Local\\gwt-boot\\lib\\gwt-boot-cli.jar",
        home + "\\AppData\\Local\\gwt-boot\\bin\\gwt-boot.bat",
        home + "\\AppData\\Local\\gwt-boot\\bin\\gwt-boot.ps1",
        home + "\\AppData\\Local\\gwt-boot" // Directory itself
      );
    } else {
      // Linux/macOS installations
      checkAndAdd(
        installations,
        "System-wide Installation (/usr/local)",
        "/usr/local/lib/gwt-boot/gwt-boot-cli.jar",
        "/usr/local/bin/gwt-boot"
      );

      checkAndAdd(
        installations,
        "User Installation (~/.local)",
        home + "/.local/lib/gwt-boot/gwt-boot-cli.jar",
        home + "/.local/bin/gwt-boot"
      );
    }

    return installations;
  }

  private void checkAndAdd(
    List<InstallLocation> installations,
    String description,
    String... paths
  ) {
    List<String> existingFiles = new ArrayList<>();
    for (String path : paths) {
      if (new File(path).exists()) {
        existingFiles.add(path);
      }
    }
    if (!existingFiles.isEmpty()) {
      installations.add(new InstallLocation(description, existingFiles));
    }
  }

  private List<String> getCheckPaths() {
    List<String> paths = new ArrayList<>();
    String home = System.getProperty("user.home");
    String os = System.getProperty("os.name").toLowerCase();

    if (os.contains("win")) {
      paths.add(home + "\\AppData\\Local\\gwt-boot\\");
    } else {
      paths.add("/usr/local/lib/gwt-boot/");
      paths.add("/usr/local/bin/gwt-boot");
      paths.add(home + "/.local/lib/gwt-boot/");
      paths.add(home + "/.local/bin/gwt-boot");
    }

    return paths;
  }

  private void deleteDirectory(Path directory) throws IOException {
    if (Files.exists(directory)) {
      Files
        .walk(directory)
        .sorted((a, b) -> b.compareTo(a)) // Reverse order to delete files before directories
        .forEach(
          path -> {
            try {
              Files.delete(path);
            } catch (IOException e) {
              // Will be reported by caller
            }
          }
        );
    }
  }

  private record InstallLocation(String description, List<String> files) {}
}
