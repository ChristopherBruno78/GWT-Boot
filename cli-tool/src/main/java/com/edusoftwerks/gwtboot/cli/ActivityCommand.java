package com.edusoftwerks.gwtboot.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

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
            Console.println("Example: gwt-boot activity dashboard");
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

        // Capitalize activity name for class names
        String activityClass = capitalize(activityName);

        Console.info("===================================");
        Console.info("GWT Boot Activity Generator");
        Console.info("===================================");
        Console.println("");
        Console.println("Activity name: " + activityClass);
        Console.println("Package: " + packageName + ".activities." + activityName);
        Console.println("");

        // Check for existing files
        Path activityBaseDir = javaBase.resolve("activities").resolve(activityName);
        Path templatesDir = resourcesBase.resolve("templates").resolve(activityName);

        java.util.List<Path> existingFiles = new java.util.ArrayList<>();

        Path presenterFile = activityBaseDir.resolve("client").resolve(activityClass + "Presenter.java");
        Path viewFile = activityBaseDir.resolve("client").resolve(activityClass + "View.java");
        Path uiXmlFile = activityBaseDir.resolve("client").resolve(activityClass + "View.ui.xml");
        Path controllerFile = activityBaseDir.resolve(activityClass + "Controller.java");
        Path gwtXmlFile = activityBaseDir.resolve(activityClass + ".gwt.xml");
        Path pebFile = templatesDir.resolve("index.peb");

        if (Files.exists(presenterFile)) existingFiles.add(presenterFile);
        if (Files.exists(viewFile)) existingFiles.add(viewFile);
        if (Files.exists(uiXmlFile)) existingFiles.add(uiXmlFile);
        if (Files.exists(controllerFile)) existingFiles.add(controllerFile);
        if (Files.exists(gwtXmlFile)) existingFiles.add(gwtXmlFile);
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
        Files.createDirectories(activityBaseDir.resolve("client"));
        Files.createDirectories(activityBaseDir);

        // Create GWT module file
        Console.info("Creating " + activityClass + ".gwt.xml...");
        Files.writeString(gwtXmlFile,
                String.format("""
                        <module rename-to="%s">
                            <inherits name="%s.%s"/>
                        
                            <source path="client"/>
                        
                            <entry-point class="%s.activities.%s.client.%sPresenter" />
                        
                        </module>
                        """, activityName, packageName, mainModuleName, packageName, activityName, activityClass)
        );

        // Create Presenter class
        Console.info("Creating " + activityClass + "Presenter.java...");
        Files.writeString(presenterFile,
                String.format("""
                        package %s.activities.%s.client;
                        
                        import com.google.gwt.core.client.GWT;
                        import com.google.gwt.user.client.mvp.Presenter;
                        
                        public class %sPresenter extends Presenter<%sView> {
                        
                            public %sPresenter() {
                                super(GWT.create(%sView.class));
                            }
                        
                        }
                        """, packageName, activityName, activityClass, activityClass, activityClass, activityClass)
        );

        // Create View class
        Console.info("Creating " + activityClass + "View.java...");
        Files.writeString(viewFile,
                String.format("""
                                package %s.activities.%s.client;
                                
                                import com.google.gwt.core.client.GWT;
                                import com.google.gwt.uibinder.client.UiBinder;
                                import com.google.gwt.user.client.mvp.View;
                                import com.google.gwt.user.client.ui.HTMLPanel;
                              
                                public class %sView extends View<%sPresenter> { 
                                
                                    interface Binder extends UiBinder<HTMLPanel, %sView> {}
                        
                                    private static final Binder Binder = GWT.create(Binder.class);
                                    
                                     public %sView() {
                                        initWidget(Binder.createAndBindUi(this));
                                    }
                                    
                                }
                                """, packageName, activityName, activityClass, activityClass, activityClass, activityClass)
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

        // Create Controller class
        Console.info("Creating " + activityClass + "Controller.java...");
        Files.writeString(controllerFile,
                String.format("""
                        package %s.activities.%s;
                        
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
                        """, packageName, activityName, activityName, activityClass, activityName)
        );

        // Create templates directory and index.peb
        Console.info("Creating templates/" + activityName + "/index.peb...");
        Files.createDirectories(templatesDir);
        Files.writeString(pebFile,
                String.format("""
                            {%% extends "base" %%}
                        
                                {%% block title %%} %s {%% endblock %%}
                        
                            {%% block scripts %%}
                            <script type="text/javascript" src="/%s/%s.nocache.js"></script>
                            {%% endblock %%}
                        """, capitalize(activityName), activityName, activityName)
        );

        Console.println("");
        Console.success("===================================");
        Console.success("Activity created successfully!");
        Console.success("===================================");
        Console.println("");
        Console.println("Created:");
        Console.println("  - " + activityBaseDir.resolve("client").resolve(activityClass + "Presenter.java"));
        Console.println("  - " + activityBaseDir.resolve("client").resolve(activityClass + "View.java"));
        Console.println("  - " + activityBaseDir.resolve("client").resolve(activityClass + "View.ui.xml"));
        Console.println("  - " + activityBaseDir.resolve(activityClass + "Controller.java"));
        Console.println("  - " + activityBaseDir.resolve(activityClass + ".gwt.xml"));
        Console.println("  - " + templatesDir.resolve("index.html"));
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
}
