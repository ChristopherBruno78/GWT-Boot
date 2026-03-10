package com.edusoftwerks.gwtboot.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
  name = "library",
  description = "Generate a new GWT library project from archetype",
  mixinStandardHelpOptions = true
)
public class LibraryCommand implements Callable<Integer> {
  @Parameters(
    index = "0",
    description = "The name of the library",
    arity = "0..1"
  )
  private String libName;

  @Override
  public Integer call() throws Exception {
    Console.info("===================================");
    Console.info("GWT Boot Library Generator");
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

    // Prompt for library name if not provided as argument
    if (libName == null || libName.trim().isEmpty()) {
      libName = InputReader.readLine("Enter library name (e.g., MyLibrary): ");
      if (libName == null || libName.trim().isEmpty()) {
        Console.error("library name cannot be empty");
        return 1;
      }
      libName = libName.trim();
    }

    // artifactId is the lowercase version of libName
    String artifactId = libName.toLowerCase();

    // Prompt for version (with default)
    String version = InputReader.readLineWithDefault(
      "Enter version (default: DEV): ",
      "DEV"
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
    Console.println("Name:       " + libName);
    Console.println("groupId:    " + groupId);
    Console.println("artifactId: " + artifactId);
    Console.println("version:    " + version);
    Console.println("package:    " + packageName);
    Console.println("");

    boolean confirmed = InputReader.confirm(
      "Generate library project with these settings?",
      true
    );
    if (!confirmed) {
      Console.warning("Project generation cancelled.");
      return 0;
    }

    Console.println("");
    Console.info("Generating library project...");
    Console.println("");

    int exitCode = ProcessExecutor.executeMavenArchetype(
      "gwt-boot-library-archetype",
      groupId,
      artifactId,
      version,
      packageName
    );

    if (exitCode == 0) {
      // Rename directory from artifactId to libName if they differ
      Path artifactIdDir = Paths.get(artifactId);
      Path libNameDir = Paths.get(libName);

      if (!artifactId.equals(libName) && Files.exists(artifactIdDir)) {
        Files.move(artifactIdDir, libNameDir);
        Console.info("Renamed project directory to: " + libName);
      }

      // Update pom.xml with the libName
      Path pomPath = Paths.get(libName, "pom.xml");
      if (Files.exists(pomPath)) {
        String pomContent = Files.readString(pomPath);
        pomContent =
          pomContent.replaceFirst(
            "<name>" + artifactId + "</name>",
            "<name>" + libName + "</name>"
          );
        Files.writeString(pomPath, pomContent);
        Console.info("Updated pom.xml with library name: " + libName);
      }

      // Rename App.gwt.xml to {libName}.gwt.xml
      String packagePath = packageName.replace('.', '/');
      Path appGwtXmlPath = Paths.get(
        libName,
        "src/main/java",
        packagePath,
        "App.gwt.xml"
      );
      Path newGwtXmlPath = Paths.get(
        libName,
        "src/main/java",
        packagePath,
        libName + ".gwt.xml"
      );
      if (Files.exists(appGwtXmlPath)) {
        String gwtXmlContent = Files.readString(appGwtXmlPath);
        gwtXmlContent =
          gwtXmlContent.replace(
            "rename-to='app'",
            "rename-to='" + artifactId + "'"
          );

        Files.writeString(newGwtXmlPath, gwtXmlContent);
        Files.delete(appGwtXmlPath);
      }

      Console.println("");
      Console.success("===================================");
      Console.success("Library project generated successfully!");
      Console.success("===================================");
      Console.println("");
    } else {
      Console.println("");
      Console.error("Library project generation failed");
      return 1;
    }

    return 0;
  }
}
