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
  name = "compile",
  description = "Compile all GWT modules for the safari permutation only",
  mixinStandardHelpOptions = true
)
public class CompileCommand implements Callable<Integer> {

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

  @CommandLine.Parameters(
    index = "0",
    description = "Output directory for compiled JS (default: current working directory)",
    defaultValue = "."
  )
  private String outputDir;

  @Override
  public Integer call() throws Exception {
    Console.info("===================================");
    Console.info("Compiling GWT (safari permutation)");
    Console.info("===================================");
    Console.println("");

    Path pomPath = Paths.get("pom.xml");
    if (!Files.exists(pomPath)) {
      Console.error("No pom.xml found in current directory");
      Console.error("Please run this command from your GWT Boot project root");
      return 1;
    }

    String gwtVersion = Utils.extractGwtVersionFromPom(pomPath);
    if (gwtVersion == null) {
      Console.error("Could not determine GWT version from pom.xml");
      return 1;
    }

    Console.info("Using GWT version: " + gwtVersion);

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

    String classpath = Utils.buildClasspath(gwtVersion);
    if (classpath == null) {
      Console.error("Could not build classpath");
      Console.error(
        "Please ensure GWT is installed in your local Maven repository"
      );
      return 1;
    }

    Console.info("Output directory: " + outputDir);
    Console.info("Compiling GWT modules (safari only)...");
    Console.println("");

    int exitCode = executeGwtCompiler(classpath, moduleNames);
    if (exitCode != 0) {
      Console.error("GWT compilation failed");
      return 1;
    }

    Console.println("");
    Console.success("===================================");
    Console.success("GWT compilation successful!");
    Console.success("===================================");
    Console.println("");
    Console.info("Output: " + outputDir);
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
    command.add(outputDir);
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
    command.add("-setProperty");
    command.add("user.agent=safari");

    command.addAll(moduleNames);

    return ProcessExecutor.executeCommand(command);
  }
}
