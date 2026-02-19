package com.edusoftwerks.gwtboot.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
  name = "jar",
  description = "Build production JAR with compiled GWT code",
  mixinStandardHelpOptions = true
)
public class JarCommand implements Callable<Integer> {
  @CommandLine.Option(
    names = { "-m", "--memory" },
    description = "Maximum heap size in MB (default: ${DEFAULT-VALUE})",
    defaultValue = "2048"
  )
  private int maxMemoryMb;

  @CommandLine.Option(
    names = { "-w", "--workers" },
    description = "Number of local workers for parallel compilation (default: ${DEFAULT-VALUE})",
    defaultValue = "2"
  )
  private int workers;

  @Override
  public Integer call() throws Exception {
    Console.info("===================================");
    Console.info("Building Production JAR");
    Console.info("===================================");
    Console.println("");

    // Check if pom.xml exists in current directory
    Path pomPath = Paths.get("pom.xml");
    if (!Files.exists(pomPath)) {
      Console.error("No pom.xml found in current directory");
      Console.error("Please run this command from your GWT Boot project root");
      return 1;
    }

    // Step 1: Clean
    Console.info("Step 1: Cleaning previous builds...");
    Console.println("");
    int exitCode = Utils.executeMavenCommand("clean");
    if (exitCode != 0) {
      Console.error("Clean failed");
      return 1;
    }

    // Step 2: Compile Java sources
    Console.println("");
    Console.info("Step 2: Compiling Java sources...");
    Console.println("");
    exitCode = Utils.executeMavenCommand("compile");
    if (exitCode != 0) {
      Console.error("Java compilation failed");
      return 1;
    }

    // Step 3: Compile GWT code (produces obfuscated JavaScript)
    Console.println("");
    Console.info("Step 3: Compiling GWT code to optimized JavaScript...");
    Console.println("");

    // Extract GWT version from pom.xml
    String gwtVersion = Utils.extractGwtVersionFromPom(pomPath);
    if (gwtVersion == null) {
      Console.error("Could not determine GWT version from pom.xml");
      return 1;
    }

    Console.info("Using GWT version: " + gwtVersion);

    // Find all GWT modules
    List<String> moduleNames = Utils.findGwtModules();
    if (moduleNames.isEmpty()) {
      Console.error(
        "Could not find any GWT modules (*.gwt.xml) in src/main/java"
      );
      return 1;
    }

    Console.info("Found " + moduleNames.size() + " GWT module(s):");
    for (String module : moduleNames) {
      Console.info("  - " + module);
    }
    Console.println("");

    // Build classpath
    String classpath = Utils.buildClasspath(gwtVersion);
    if (classpath == null) {
      Console.error("Could not build classpath");
      Console.error(
        "Please ensure GWT is installed in your local Maven repository"
      );
      return 1;
    }

    // Compile GWT modules
    exitCode = executeGwtCompiler(classpath, moduleNames);
    if (exitCode != 0) {
      Console.error("GWT compilation failed");
      return 1;
    }

    // Step 4: Package Spring Boot fat JAR
    Console.println("");
    Console.info("Step 4: Creating Spring Boot fat JAR...");
    Console.println("");
    exitCode = Utils.executeMavenCommand("package", "-DskipTests");
    if (exitCode != 0) {
      Console.error("Maven package failed");
      return 1;
    }

    Console.println("");
    Console.success("===================================");
    Console.success("Production JAR built successfully!");
    Console.success("===================================");
    Console.println("");
    Console.info("JAR location: target/*.jar");
    Console.println("");
    Console.info("To run the application:");
    Console.info("  java -jar target/*.jar");
    Console.println("");

    return 0;
  }

  private int executeGwtCompiler(String classpath, List<String> moduleNames)
    throws IOException, InterruptedException {
    List<String> command = new ArrayList<>();
    command.add("java");
    command.add("-Xmx" + maxMemoryMb + "m");
    command.add("-cp");
    command.add(classpath);
    command.add("com.google.gwt.dev.Compiler");
    command.add("-war");
    command.add("target/classes/static");
    command.add("-sourceLevel");
    command.add("17");
    command.add("-logLevel");
    command.add("INFO");
    command.add("-style");
    command.add("OBFUSCATED");
    command.add("-optimize");
    command.add("9");
    command.add("-localWorkers");
    command.add(String.valueOf(workers));
    command.add("-extra");
    command.add("target/extra");
    command.add("-compileReport");
    command.add("-XcompilerMetrics");

    // Add all module names to the command
    command.addAll(moduleNames);

    return ProcessExecutor.executeCommand(command);
  }
}
