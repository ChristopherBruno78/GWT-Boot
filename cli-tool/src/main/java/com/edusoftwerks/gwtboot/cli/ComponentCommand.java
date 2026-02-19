package com.edusoftwerks.gwtboot.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

@Command(
        name = "component",
        description = "Create a new UI component in the current project",
        mixinStandardHelpOptions = true
)
public class ComponentCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The component name")
    private String componentName;

    @Override
    public Integer call() throws Exception {
        if (componentName == null || componentName.trim().isEmpty()) {
            Console.error("Component name is required");
            Console.println("Usage: gwt-boot component <component-name>");
            Console.println("Example: gwt-boot component button");
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

        // Find the main GWT module name
        String mainModuleName = findMainGwtModule(javaBase);
        if (mainModuleName == null) {
            Console.error("Could not find main GWT module (.gwt.xml) in " + javaBase);
            return 1;
        }

        // Capitalize component name for class names
        String componentClass = capitalize(componentName);
        String componentNameLower = componentName.toLowerCase();

        Console.info("===================================");
        Console.info("GWT Boot Component Generator");
        Console.info("===================================");
        Console.println("");
        Console.println("Component name: " + componentClass);
        Console.println("Package: " + packageName + ".client.components");
        Console.println("");

        // Check for existing files
        Path componentsDir = javaBase.resolve("client").resolve("components");
        Path resourcesDir = componentsDir.resolve("resources").resolve(componentNameLower);

        java.util.List<Path> existingFiles = new java.util.ArrayList<>();

        Path javaFile = componentsDir.resolve(componentClass + ".java");
        Path cssFile = resourcesDir.resolve("style.css");
        Path uiXmlFile = resourcesDir.resolve(componentClass + ".ui.xml");

        if (Files.exists(javaFile)) existingFiles.add(javaFile);
        if (Files.exists(cssFile)) existingFiles.add(cssFile);
        if (Files.exists(uiXmlFile)) existingFiles.add(uiXmlFile);

        if (!existingFiles.isEmpty()) {
            Console.warning("WARNING: The following files will be overwritten:");
            for (Path file : existingFiles) {
                Console.println("  - " + file);
            }
            Console.println("");

            if (!InputReader.confirm("Continue with generation?", false)) {
                Console.warning("Component generation cancelled.");
                return 1;
            }
            Console.println("");
        }

        // Create component directories
        Console.info("Creating component structure...");
        Files.createDirectories(componentsDir);
        Files.createDirectories(resourcesDir);

        // Ensure <source path="client" /> is in the main GWT module
        Path gwtXmlPath = javaBase.resolve(mainModuleName + ".gwt.xml");
        if (Files.exists(gwtXmlPath)) {
            ensureClientSourcePath(gwtXmlPath);
        }

        // Create Java component class
        Console.info("Creating " + componentClass + ".java...");
        Files.writeString(javaFile,
                String.format("""
                                package %s.client.components;
                                
                                import com.google.gwt.core.client.GWT;
                                import com.google.gwt.resources.client.ClientBundle;
                                import com.google.gwt.resources.client.StyleResource;
                                import com.google.gwt.uibinder.client.UiBinder;
                                import com.google.gwt.uibinder.client.UiTemplate;
                                import com.google.gwt.user.client.ui.Composite;
                                import com.google.gwt.user.client.ui.Widget;
                                
                                public class %s extends Composite {
                                
                                    interface Resources extends ClientBundle {
                                        Resources INSTANCE = GWT.create(Resources.class);
                                
                                        @Source("resources/%s/style.css")
                                        StyleResource style();
                                
                                    }
                                
                                    @UiTemplate("resources/%s/%s.ui.xml")
                                    interface %sUiBinder extends UiBinder<HTMLPanel, %s> {}
                                
                                    private static final %sUiBinder uiBinder = GWT.create(%sUiBinder.class);
                                
                                    static {
                                        Resources.INSTANCE.style().ensureInjected();
                                    }
                                
                                    public %s() {
                                        initWidget(uiBinder.createAndBindUi(this));
                                    }
                                }
                                """, packageName, componentClass, componentNameLower, componentNameLower, componentClass,
                        componentClass, componentClass, componentClass, componentClass, componentClass)
        );

        // Create CSS file
        Console.info("Creating style.css...");
        Files.writeString(cssFile,
                """
                        /* Styles for component */
                        """
        );

        // Create UiBinder XML file
        Console.info("Creating " + componentClass + ".ui.xml...");
        Files.writeString(uiXmlFile,
                """
                        <!DOCTYPE ui:UiBinder SYSTEM "http://dl.google.com/gwt/DTD/xhtml.ent">
                        <ui:UiBinder xmlns:ui="urn:ui:com.google.gwt.uibinder"
                                     xmlns:g="urn:import:com.google.gwt.user.client.ui">
                        
                            <g:HTMLPanel>
                                <!-- Add your widgets here -->
                            </g:HTMLPanel>
                        </ui:UiBinder>
                        """
        );

        Console.println("");
        Console.success("===================================");
        Console.success("Component created successfully!");
        Console.success("===================================");
        Console.println("");
        Console.println("Created:");
        Console.println("  - " + javaFile);
        Console.println("  - " + uiXmlFile);
        Console.println("  - " + cssFile);
        Console.println("");
        Console.println("Import in your code:");
        Console.println("  import " + packageName + ".client.components." + componentClass + ";");
        Console.println("");

        return 0;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String findMainGwtModule(Path javaBase) throws IOException {
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

    private void ensureClientSourcePath(Path gwtXmlPath) throws IOException {
        String content = Files.readString(gwtXmlPath);

        // Check if <source path="client" /> already exists
        Pattern pattern = Pattern.compile("<source\\s+path=[\"']client[\"']\\s*/>");
        if (pattern.matcher(content).find()) {
            // Already exists, nothing to do
            return;
        }

        // Find the position to insert (after last <inherits> or <source> tag, or before </module>)
        int insertPos = content.lastIndexOf("</module>");
        if (insertPos == -1) {
            Console.warning("Could not find </module> tag in " + gwtXmlPath);
            return;
        }

        // Build the new content with proper indentation
        String beforeModule = content.substring(0, insertPos);
        String afterModule = content.substring(insertPos);

        // Add the source path with proper indentation
        String newContent = beforeModule + "\n    <source path=\"client\" />\n\n" + afterModule;

        Files.writeString(gwtXmlPath, newContent);
        Console.info("Added <source path=\"client\" /> to " + gwtXmlPath.getFileName());
    }
}
