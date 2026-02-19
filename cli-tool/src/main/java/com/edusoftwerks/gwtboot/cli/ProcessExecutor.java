package com.edusoftwerks.gwtboot.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProcessExecutor {
  private static final String archetypeVersion = "1.0.0";

  public static int executeMavenArchetype(
    String groupId,
    String artifactId,
    String version,
    String packageName
  )
    throws IOException, InterruptedException {
    List<String> command = new ArrayList<>();
    command.add("mvn");
    command.add("archetype:generate");
    command.add("-DarchetypeGroupId=com.edusoftwerks");
    command.add("-DarchetypeArtifactId=gwt-boot-archetype");
    command.add("-DarchetypeVersion=" + archetypeVersion);
    command.add("-DgroupId=" + groupId);
    command.add("-DartifactId=" + artifactId);
    command.add("-Dversion=" + version);
    command.add("-Dpackage=" + packageName);
    command.add("-DinteractiveMode=false");

    return executeCommand(command);
  }

  public static int executeCommand(List<String> command)
    throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    pb.inheritIO();

    Process process = pb.start();
    return process.waitFor();
  }

  public static void makeExecutable(String filePath)
    throws IOException, InterruptedException {
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      // Windows doesn't need chmod
      return;
    }

    List<String> command = new ArrayList<>();
    command.add("chmod");
    command.add("+x");
    command.add(filePath);

    executeCommand(command);
  }

  public static Process executeCommandInBackground(
    List<String> command,
    String logFile
  )
    throws IOException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);

    if (logFile != null) {
      pb.redirectOutput(
        ProcessBuilder.Redirect.appendTo(new java.io.File(logFile))
      );
    } else {
      pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    }

    return pb.start();
  }

  public static Process executeCommandInBackgroundWithOutput(
    List<String> command
  )
    throws IOException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    pb.inheritIO();

    return pb.start();
  }
}
