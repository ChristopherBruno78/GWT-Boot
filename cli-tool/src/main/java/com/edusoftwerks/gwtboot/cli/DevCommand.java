package com.edusoftwerks.gwtboot.cli;

import com.sun.net.httpserver.HttpServer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

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

    @CommandLine.Parameters(
            index = "0",
            description = "Output directory for server JS code (default: target/classes/static)",
            defaultValue = "target/classes/static"
    )
    private String launcherDir;

    @Option(
            names = {"--static-server"},
            description = "Launch an embedded HTTP server for desktop (non-Spring Boot) development"
    )
    private boolean staticServer;

    @Option(
            names = {"-p", "--port"},
            description = "Port for the embedded HTTP server (default: ${DEFAULT-VALUE})",
            defaultValue = "8080"
    )
    private int port;

    @Option(
            names = {"-r", "--resources"},
            description = "Resource directory to serve via embedded HTTP server (default: ${DEFAULT-VALUE})",
            defaultValue = "src/main/resources"
    )
    private String resourceDir;

    @Override
    public Integer call() throws Exception {
        Console.info("===================================");
        Console.info("Starting GWT CodeServer");
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

        HttpServer httpServer = null;

        if (staticServer) {
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

            Path modulePath = Paths.get(launcherDir);
            Files.createDirectories(modulePath);
            httpServer = startFileServer(resourcePath, modulePath);
        }

        Console.info("Max heap memory: " + maxMemoryMb + "MB");
        Console.info("Launcher directory: " + launcherDir);
        Console.println("");
        Console.println("=== GWT CodeServer Output ===");
        Console.println("");

        Process codeServer = startGwtCodeServer(classpath, moduleNames);

        // Read CodeServer output line by line, forwarding to console
        boolean serverInfoPrinted = !staticServer;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(codeServer.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (!serverInfoPrinted && line.contains("The code server is ready")) {
                    Console.println("");
                    Console.info("===================================");
                    Console.info("Static server started at http://localhost:" + port);
                    Console.info("Serving files from: " + resourceDir);
                    Console.info("GWT modules from: " + launcherDir);
                    Console.info("===================================");
                    serverInfoPrinted = true;
                }
            }
        }

        int exitCode = codeServer.waitFor();

        if (httpServer != null) {
            httpServer.stop(0);
        }

        return exitCode;
    }

    private HttpServer startFileServer(Path resourceRoot, Path moduleRoot) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.equals("/")) {
                requestPath = "/index.html";
            }

            String relativePath = requestPath.substring(1);

            // Try module output directory first (target/classes/static) for GWT JS
            Path filePath = moduleRoot.resolve(relativePath).normalize();
            if (filePath.startsWith(moduleRoot) && Files.exists(filePath) && !Files.isDirectory(filePath)) {
                serveFile(exchange, filePath);
                return;
            }

            // Fall back to resource directory (src/main/resources) for index.html, css, etc.
            filePath = resourceRoot.resolve(relativePath).normalize();
            if (!filePath.startsWith(resourceRoot)) {
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

            serveFile(exchange, filePath);
        });
        server.start();
        return server;
    }

    private void serveFile(com.sun.net.httpserver.HttpExchange exchange, Path filePath) throws IOException {
        String contentType = resolveContentType(filePath.toString());
        byte[] bytes = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
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

    private Process startGwtCodeServer(String classpath, List<String> moduleNames) throws IOException {
        // Clean the CodeServer work directory to force a fresh compile
        Path workDir = Paths.get("target", "gwt-codeserver");
        if (Files.exists(workDir)) {
            Console.info("Cleaning CodeServer work directory...");
            try (var walker = Files.walk(workDir)) {
                walker.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.delete(p); } catch (IOException ignored) {}
                        });
            }
        }
        Files.createDirectories(workDir);

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
        command.add("-workDir");
        command.add(workDir.toString());
        command.add("-sourceLevel");
        command.add("17");
        command.add("-logLevel");
        command.add("INFO");
        command.addAll(moduleNames);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        return pb.start();
    }
}
