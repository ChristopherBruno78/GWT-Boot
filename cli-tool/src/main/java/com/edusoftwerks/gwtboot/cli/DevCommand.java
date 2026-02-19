package com.edusoftwerks.gwtboot.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static java.nio.file.StandardWatchEventKinds.*;

@Command(
        name = "dev",
        aliases = {"codeserver"},
        description = "Launch the GWT CodeServer for development",
        mixinStandardHelpOptions = true
)
public class DevCommand implements Callable<Integer> {
    @Option(
            names = {"-m", "--memory"},
            description = "Maximum heap size in MB (default: ${DEFAULT-VALUE})",
            defaultValue = "2048"
    )
    private int maxMemoryMb;

    @Override
    public Integer call() throws Exception {

        Console.info("===================================");
        Console.info("Starting GWT CodeServer");
        Console.info("===================================");
        Console.println("");


        // Check if pom.xml exists in current directory
        Path pomPath = Paths.get("pom.xml");
        if (!Files.exists(pomPath)) {
            Console.error("No pom.xml found in current directory");
            Console.error("Please run this command from your GWT Boot project root");
            return 1;
        }

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
            Console.error("Could not find gwt-codeserver jar in Maven repository");
            Console.error(
                    "Please ensure GWT is installed in your local Maven repository"
            );
            return 1;
        }

        Console.info("Max heap memory: " + maxMemoryMb + "MB");
        Console.info("Launching GWT CodeServer (SuperDevMode) in background...");
        Console.println("");
        Console.println("=== GWT CodeServer Output ===");
        Console.println("");

        return executeGwtCodeServer(classpath, moduleNames, maxMemoryMb).waitFor();
    }

    private Process executeGwtCodeServer(
            String classpath,
            List<String> moduleNames,
            int maxMemoryMb
    )
            throws IOException {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Xmx" + maxMemoryMb + "m");
        command.add("-cp");
        command.add(classpath);
        command.add("com.google.gwt.dev.codeserver.CodeServer");
        command.add("-src");
        command.add("src/main/java");
        command.add("-launcherDir");
        command.add("target/classes/static");
        command.add("-sourceLevel");
        command.add("17");
        command.add("-logLevel");
        command.add("INFO");

        // Add all module names to the command
        command.addAll(moduleNames);

        return ProcessExecutor.executeCommandInBackgroundWithOutput(command);
    }

}
