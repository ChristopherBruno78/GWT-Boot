package com.edusoftwerks.gwtboot.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command(
        name = "activity",
        description = "Create a new activity in the current project",
        mixinStandardHelpOptions = true
)
public class ActivityCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The activity name")
    private String activityName;

    @Override
    public Integer call() throws Exception {
        if (activityName == null || activityName.trim().isEmpty()) {
            Console.error("Activity name is required");
            Console.println("Usage: gwt-boot activity <activity-name>");
            Console.println("Example: gwt-boot activity login");
            Console.println("Example: gwt-boot activity user/login");
            return 1;
        }

        // Check if we're in a GWT Boot project
        Path pomPath = Paths.get("pom.xml");
        if (!Files.exists(pomPath)) {
            Console.error("pom.xml not found. Make sure you're in the root of a GWT Boot project.");
            return 1;
        }

        // Extract package name from pom.xml
        String packageName = Utils.extractPackageFromPom(pomPath);
        if (packageName == null) {
            Console.error("Could not determine package from pom.xml");
            return 1;
        }

        // Convert package to directory path
        String packagePath = packageName.replace('.', '/');

        // Define base paths
        Path javaBase = Paths.get("src/main/java", packagePath);
        Path resourcesBase = Paths.get("src/main/resources");

        // Find the main GWT module name
        String mainModuleName = findMainGwtModule(javaBase);
        if (mainModuleName == null) {
            Console.error("Could not find main GWT module (.gwt.xml) in " + javaBase);
            return 1;
        }

        // Parse activity path (e.g., "user/login" or just "login")
        String[] pathParts = activityName.split("/");
        String simpleActivityName = pathParts[pathParts.length - 1];
        String activityClass = capitalize(simpleActivityName);

        // Build the activity package path (e.g., "user.login" or just "login")
        String activityPackagePath = activityName.replace('/', '.');

        Console.info("===================================");
        Console.info("GWT Boot Activity Generator");
        Console.info("===================================");
        Console.println("");
        Console.println("Activity name: " + activityClass);
        Console.println("Package: " + packageName + ".activities.client." + activityPackagePath);
        Console.println("");

        // Define paths for the new structure
        Path activitiesBaseDir = javaBase.resolve("activities");
        Path activitiesClientDir = activitiesBaseDir.resolve("client");
        Path activityDir = activitiesClientDir.resolve(activityName);
        Path controllersDir = javaBase.resolve("controllers");
        Path templatesDir = resourcesBase.resolve("templates").resolve(activityName);

        // Check for existing files
        java.util.List<Path> existingFiles = new java.util.ArrayList<>();

        Path presenterFile = activityDir.resolve(activityClass + "Presenter.java");
        Path viewFile = activityDir.resolve(activityClass + "View.java");
        Path uiXmlFile = activityDir.resolve(activityClass + "View.ui.xml");
        Path gwtXmlFile = activitiesBaseDir.resolve("Activities.gwt.xml");
        Path mainFile = activitiesClientDir.resolve("Main.java");
        Path controllerFile = controllersDir.resolve(activityClass + "Controller.java");
        Path pebFile = templatesDir.resolve("index.peb");

        if (Files.exists(presenterFile)) existingFiles.add(presenterFile);
        if (Files.exists(viewFile)) existingFiles.add(viewFile);
        if (Files.exists(uiXmlFile)) existingFiles.add(uiXmlFile);
        if (Files.exists(controllerFile)) existingFiles.add(controllerFile);
        if (Files.exists(pebFile)) existingFiles.add(pebFile);

        if (!existingFiles.isEmpty()) {
            Console.warning("WARNING: The following files will be overwritten:");
            for (Path file : existingFiles) {
                Console.println("  - " + file);
            }
            Console.println("");

            if (!InputReader.confirm("Continue with generation?", false)) {
                Console.warning("Activity generation cancelled.");
                return 1;
            }
            Console.println("");
        }

        // Create package directories
        Console.info("Creating package structure...");
        Files.createDirectories(activityDir);

        // Create Activities.gwt.xml if it doesn't exist
        if (!Files.exists(gwtXmlFile)) {
            Console.info("Creating Activities.gwt.xml...");
            Files.writeString(gwtXmlFile,
                    String.format("""
                            <module rename-to="a">

                                <inherits name="%s.%s"/>

                                <source path="client"/>

                                <entry-point class="%s.activities.client.Main"/>

                            </module>
                            """, packageName, mainModuleName, packageName)
            );
        }

        // Create or update Main.java
        String route = "/" + activityName;
        String presenterClassName = packageName + ".activities.client." + activityPackagePath + "." + activityClass + "Presenter";
        String addActivityCall = String.format("        addActivity(\"%s\", () -> new %sPresenter().presentView());",
                route, activityClass);

        if (!Files.exists(mainFile)) {
            Console.info("Creating Main.java...");
            Files.writeString(mainFile,
                    String.format("""
                            package %s.activities.client;

                            import %s;
                            import com.cocoawerks.gwt.commons.appkit.client.mvp.ActivityRouter;

                            public class Main extends ActivityRouter {
                                @Override
                                public void initialize() {
                            %s
                                }
                            }
                            """, packageName, presenterClassName, addActivityCall)
            );
        } else {
            Console.info("Updating Main.java with new activity route...");
            updateMainFile(mainFile, presenterClassName, addActivityCall);
        }

        // Create Presenter class
        Console.info("Creating " + activityClass + "Presenter.java...");
        Files.writeString(presenterFile,
                String.format("""
                        package %s.activities.client.%s;

                        import com.cocoawerks.gwt.commons.appkit.client.mvp.Presenter;
                        import com.google.gwt.core.client.GWT;

                        public class %sPresenter extends Presenter<%sView> {

                            public %sPresenter() {
                                super(GWT.create(%sView.class));
                            }

                        }
                        """, packageName, activityPackagePath, activityClass, activityClass, activityClass, activityClass)
        );

        // Create View class
        Console.info("Creating " + activityClass + "View.java...");
        Files.writeString(viewFile,
                String.format("""
                        package %s.activities.client.%s;

                        import com.cocoawerks.gwt.commons.appkit.client.mvp.View;
                        import com.google.gwt.core.client.GWT;
                        import com.google.gwt.uibinder.client.UiBinder;
                        import com.google.gwt.user.client.ui.HTMLPanel;

                        public class %sView extends View<%sPresenter> {

                            interface Binder extends UiBinder<HTMLPanel, %sView> {}

                            private static final Binder Binder = GWT.create(Binder.class);

                            public %sView() {
                                initWidget(Binder.createAndBindUi(this));
                            }

                        }
                        """, packageName, activityPackagePath, activityClass, activityClass, activityClass, activityClass)
        );

        // Create UiBinder template
        Console.info("Creating " + activityClass + "View.ui.xml...");
        Files.writeString(uiXmlFile,
                String.format("""
                        <!DOCTYPE ui:UiBinder SYSTEM "http://dl.google.com/gwt/DTD/xhtml.ent">
                        <ui:UiBinder xmlns:ui="urn:ui:com.google.gwt.uibinder"
                                     xmlns:g="urn:import:com.google.gwt.user.client.ui"
                                     xmlns:a="urn:import:com.cocoawerks.gwt.commons.appkit.client.components"
                                     xmlns:c="urn:import:%s.client.components">

                            <g:HTMLPanel>
                                <!-- Add your widgets here -->
                            </g:HTMLPanel>
                        </ui:UiBinder>
                        """, packageName)
        );

        // Create templates directory and index.peb
        Console.info("Creating templates/" + activityName + "/index.peb...");
        Files.createDirectories(templatesDir);
        Files.writeString(pebFile,
                String.format("""
                        {%% extends "base" %%}

                        {%% block title %%} %s {%% endblock %%}

                        {%% block scripts %%}
                        <script type="text/javascript" src="/a/a.nocache.js"></script>
                        {%% endblock %%}
                        """, capitalize(simpleActivityName))
        );

        // Create Controller class
        Console.info("Creating " + activityClass + "Controller.java...");
        Files.createDirectories(controllersDir);
        Files.writeString(controllerFile,
                String.format("""
                        package %s.controllers;

                        import org.springframework.stereotype.Controller;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RequestMapping;

                        @Controller
                        @RequestMapping("/%s")
                        public class %sController {

                            @GetMapping
                            public String index() {
                                return "%s/index";
                            }
                        }
                        """, packageName, activityName, activityClass, activityName)
        );

        Console.println("");
        Console.success("===================================");
        Console.success("Activity created successfully!");
        Console.success("===================================");
        Console.println("");
        Console.println("Created:");
        Console.println("  - " + presenterFile);
        Console.println("  - " + viewFile);
        Console.println("  - " + uiXmlFile);
        Console.println("  - " + controllerFile);
        if (!existingFiles.contains(gwtXmlFile)) {
            Console.println("  - " + gwtXmlFile);
        }
        if (!existingFiles.contains(mainFile)) {
            Console.println("  - " + mainFile);
        }
        Console.println("  - " + pebFile);
        Console.println("");
        Console.println("Access the activity at: http://localhost:8080/" + activityName);
        Console.println("");

        return 0;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String findMainGwtModule(Path javaBase) throws java.io.IOException {
        if (!Files.exists(javaBase)) {
            return null;
        }

        // Look for .gwt.xml files directly in the base package directory (not in subdirectories)
        try (java.util.stream.Stream<Path> files = Files.list(javaBase)) {
            java.util.Optional<Path> gwtXmlFile = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".gwt.xml"))
                    .findFirst();

            if (gwtXmlFile.isPresent()) {
                String fileName = gwtXmlFile.get().getFileName().toString();
                // Remove .gwt.xml extension to get module name
                return fileName.substring(0, fileName.length() - 8);
            }
        }

        return null;
    }

    private void updateMainFile(Path mainFile, String presenterClassName, String addActivityCall) throws java.io.IOException {
        String content = Files.readString(mainFile);

        // Check if the import already exists
        String importStatement = "import " + presenterClassName + ";";
        boolean hasImport = content.contains(importStatement);

        // Check if the addActivity call already exists
        if (content.contains(addActivityCall.trim())) {
            Console.warning("Activity route already exists in Main.java, skipping update.");
            return;
        }

        // Find the initialize method
        Pattern initializePattern = Pattern.compile(
                "public\\s+void\\s+initialize\\s*\\(\\s*\\)\\s*\\{(.*?)\\n(\\s*)}",
                Pattern.DOTALL
        );
        Matcher matcher = initializePattern.matcher(content);

        if (!matcher.find()) {
            Console.error("Could not find initialize() method in Main.java");
            return;
        }

        int methodStartPos = matcher.start();
        int methodEndPos = matcher.end();
        String initializeBody = matcher.group(1);
        String closingIndent = matcher.group(2);

        // Find the last addActivity call to insert after it
        Pattern addActivityPattern = Pattern.compile("addActivity\\([^)]+\\)[^;]*;");
        Matcher addActivityMatcher = addActivityPattern.matcher(initializeBody);
        int lastAddActivityEnd = -1;
        while (addActivityMatcher.find()) {
            lastAddActivityEnd = addActivityMatcher.end();
        }

        // Build the new initialize body
        String newBody;
        if (lastAddActivityEnd >= 0) {
            // Insert after the last addActivity call
            newBody = initializeBody.substring(0, lastAddActivityEnd) +
                    "\n" + addActivityCall +
                    initializeBody.substring(lastAddActivityEnd);
        } else {
            // No existing addActivity calls, add it to the beginning
            newBody = "\n" + addActivityCall + initializeBody;
        }

        // Reconstruct the entire method with proper formatting
        String newMethod = "public void initialize() {" +
                newBody +
                "\n" + closingIndent + "}";

        // Replace the entire initialize method
        String newContent = content.substring(0, methodStartPos) +
                newMethod +
                content.substring(methodEndPos);

        // Add import if needed
        if (!hasImport) {
            // Find the last import statement
            Pattern importPattern = Pattern.compile("(import\\s+[^;]+;)");
            Matcher importMatcher = importPattern.matcher(newContent);
            int lastImportEnd = -1;
            while (importMatcher.find()) {
                lastImportEnd = importMatcher.end();
            }

            if (lastImportEnd > 0) {
                newContent = newContent.substring(0, lastImportEnd) +
                        "\nimport " + presenterClassName + ";" +
                        newContent.substring(lastImportEnd);
            }
        }

        Files.writeString(mainFile, newContent);
        Console.success("Added activity route to Main.java");
    }
}
