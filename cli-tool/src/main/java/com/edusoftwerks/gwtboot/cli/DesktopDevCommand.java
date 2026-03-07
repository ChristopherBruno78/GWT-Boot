package com.edusoftwerks.gwtboot.cli;

import com.sun.net.httpserver.HttpServer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "desktop-dev",
        description = "Launch the GWT CodeServer and a static file server for desktop development",
        mixinStandardHelpOptions = true
)
public class DesktopDevCommand implements Callable<Integer> {

    @Option(
            names = {"-m", "--memory"},
            description = "Maximum heap size in MB (default: ${DEFAULT-VALUE})",
            defaultValue = "2048"
    )
    private int maxMemoryMb;

    @Option(
            names = {"-p", "--port"},
            description = "Port for the static file server (default: ${DEFAULT-VALUE})",
            defaultValue = "8080"
    )
    private int port;

    @CommandLine.Parameters(
            index = "0",
            description = "Output directory for server JS code (default: target/classes/static)",
            defaultValue = "target/classes/static"
    )
    private String launcherDir;

    @Option(
            names = {"-r", "--resources"},
            description = "Maven resource directory to serve (default: ${DEFAULT-VALUE})",
            defaultValue = "src/main/resources"
    )
    private String resourceDir;

    @Override
    public Integer call() throws Exception {
        Console.info("===================================");
        Console.info("Starting GWT Desktop Dev");
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
            Console.error("Could not find any GWT modules (*.gwt.xml) in src/main/java");
            return 1;
        }

        Console.info("Found " + moduleNames.size() + " GWT module(s):");
        for (String module : moduleNames) {
            Console.info("  - " + module);
        }
        Console.println("");

        String classpath = Utils.buildClasspath(gwtVersion);
        if (classpath == null) {
            Console.error("Could not find gwt-codeserver jar in Maven repository");
            Console.error("Please ensure GWT is installed in your local Maven repository");
            return 1;
        }

        Path resourcePath = Paths.get(resourceDir);
        if (!Files.exists(resourcePath)) {
            Console.error("Resource directory not found: " + resourceDir);
            return 1;
        }

        Path indexPath = resourcePath.resolve("index.html");
        if (!Files.exists(indexPath)) {
            Console.error("No index.html found in: " + resourceDir);
            return 1;
        }

        Console.info("Max heap memory: " + maxMemoryMb + "MB");
        Console.info("Launcher directory: " + launcherDir);
        Console.info("Serving files from: " + resourceDir);
        Console.info("Static file server port: " + port);
        Console.println("");

        HttpServer httpServer = startFileServer(resourcePath);
        Console.info("Static file server started at http://localhost:" + port);
        Console.println("");
        Console.println("=== GWT CodeServer Output ===");
        Console.println("");

        Process codeServer = executeGwtCodeServer(classpath, moduleNames);
        int exitCode = codeServer.waitFor();

        httpServer.stop(0);
        return exitCode;
    }

    private HttpServer startFileServer(Path root) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.equals("/")) {
                requestPath = "/index.html";
            }

            Path filePath = root.resolve(requestPath.substring(1)).normalize();
            if (!filePath.startsWith(root)) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                byte[] body = "404 Not Found".getBytes();
                exchange.sendResponseHeaders(404, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
                return;
            }

            String contentType = resolveContentType(filePath.toString());
            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        return server;
    }

    private String resolveContentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        if (fileName.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private Process executeGwtCodeServer(String classpath, List<String> moduleNames) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Xmx" + maxMemoryMb + "m");
        command.add("-cp");
        command.add(classpath);
        command.add("com.google.gwt.dev.codeserver.CodeServer");
        command.add("-src");
        command.add("src/main/java");
        command.add("-launcherDir");
        command.add(launcherDir);
        command.add("-sourceLevel");
        command.add("17");
        command.add("-logLevel");
        command.add("INFO");
        command.addAll(moduleNames);

        return ProcessExecutor.executeCommandInBackgroundWithOutput(command);
    }
}
