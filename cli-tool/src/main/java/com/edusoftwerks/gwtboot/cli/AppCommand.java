package com.edusoftwerks.gwtboot.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
  name = "app",
  description = "Generate a new GWT Boot project from archetype",
  mixinStandardHelpOptions = true
)
public class AppCommand implements Callable<Integer> {
  @Parameters(
    index = "0",
    description = "The name of the application",
    arity = "0..1"
  )
  private String appName;

  @Override
  public Integer call() throws Exception {
    Console.info("===================================");
    Console.info("GWT Boot Project Generator");
    Console.info("===================================");
    Console.println("");

    // Prompt for groupId
    String groupId = InputReader.readLine(
      "Enter groupId (e.g., com.mycompany): "
    );
    if (groupId == null || groupId.trim().isEmpty()) {
      Console.error("groupId cannot be empty");
      return 1;
    }
    groupId = groupId.trim().toLowerCase();

    // Prompt for app name if not provided as argument
    if (appName == null || appName.trim().isEmpty()) {
      appName = InputReader.readLine("Enter app name (e.g., MyApp): ");
      if (appName == null || appName.trim().isEmpty()) {
        Console.error("app name cannot be empty");
        return 1;
      }
      appName = appName.trim();
    }

    // artifactId is the lowercase version of appName
    String artifactId = appName.toLowerCase();

    // Prompt for version (with default)
    String version = InputReader.readLineWithDefault(
      "Enter version (default: 0.0.1-SNAPSHOT): ",
      "0.0.1-SNAPSHOT"
    );

    // Prompt for package (with default derived from groupId.artifactId)
    String defaultPackage = groupId + "." + artifactId;
    String packageName = InputReader.readLineWithDefault(
      "Enter package name (default: " + defaultPackage + "): ",
      defaultPackage
    );

    Console.println("");
    Console.info("Summary:");
    Console.println("--------");
    Console.println("Name:   " + appName);
    Console.println("groupId:    " + groupId);
    Console.println("artifactId: " + artifactId);
    Console.println("version:    " + version);
    Console.println("package:    " + packageName);
    Console.println("");

    boolean confirmed = InputReader.confirm(
      "Generate project with these settings?",
      true
    );
    if (!confirmed) {
      Console.warning("Project generation cancelled.");
      return 0;
    }

    Console.println("");
    Console.info("Generating project...");
    Console.println("");

    int exitCode = ProcessExecutor.executeMavenArchetype(
      groupId,
      artifactId,
      version,
      packageName
    );

    if (exitCode == 0) {
      // Rename directory from artifactId to appName if they differ
      Path artifactIdDir = Paths.get(artifactId);
      Path appNameDir = Paths.get(appName);

      if (!artifactId.equals(appName) && Files.exists(artifactIdDir)) {
        Files.move(artifactIdDir, appNameDir);
        Console.info("Renamed project directory to: " + appName);
      }

      // Update pom.xml with the appName
      Path pomPath = Paths.get(appName, "pom.xml");
      if (Files.exists(pomPath)) {
        String pomContent = Files.readString(pomPath);
        // Replace <name>artifactId</name> with <name>appName</name>
        pomContent =
          pomContent.replaceFirst(
            "<name>" + artifactId + "</name>",
            "<name>" + appName + "</name>"
          );
        Files.writeString(pomPath, pomContent);
        Console.info("Updated pom.xml with app name: " + appName);
      }

      // Rename App.gwt.xml to {appName}.gwt.xml
      String packagePath = packageName.replace('.', '/');
      Path appGwtXmlPath = Paths.get(
        appName,
        "src/main/java",
        packagePath,
        "App.gwt.xml"
      );
      Path newGwtXmlPath = Paths.get(
        appName,
        "src/main/java",
        packagePath,
        appName + ".gwt.xml"
      );
      if (Files.exists(appGwtXmlPath)) {
        // Read and update content
        String gwtXmlContent = Files.readString(appGwtXmlPath);
        gwtXmlContent =
          gwtXmlContent.replace(
            "rename-to='app'",
            "rename-to='" + artifactId + "'"
          );

        // Write to new file and delete old one
        Files.writeString(newGwtXmlPath, gwtXmlContent);
        Files.delete(appGwtXmlPath);
      }

      // Fix line endings and make scripts executable
      Path mvnwPath = Paths.get(appName, "mvnw");
      if (Files.exists(mvnwPath)) {
        ProcessExecutor.makeExecutable(mvnwPath.toString());
      }

      Path mvnwCmdPath = Paths.get(appName, "mvnw.cmd");
      if (Files.exists(mvnwCmdPath)) {
        ProcessExecutor.makeExecutable(mvnwCmdPath.toString());
      }

      Console.println("");
      Console.success("===================================");
      Console.success("Project generated successfully!");
      Console.success("===================================");
      Console.println("");
    } else {
      Console.println("");
      Console.error("Project generation failed");
      return 1;
    }

    return 0;
  }
}
