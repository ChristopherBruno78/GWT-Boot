package com.edusoftwerks.gwtboot.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@Command(
        name = "service",
        description = "Create a new GWT RPC service in the current project",
        mixinStandardHelpOptions = true
)
public class ServiceCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The service name")
    private String serviceName;

    @Override
    public Integer call() throws Exception {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            Console.error("Service name is required");
            Console.println("Usage: gwt-boot service <service-name>");
            Console.println("Example: gwt-boot service UserAuth");
            return 1;
        }

        // Capitalize service name for class names
        String serviceClass = capitalize(serviceName);

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

        Console.info("===================================");
        Console.info("GWT RPC Service Generator");
        Console.info("===================================");
        Console.println("");
        Console.println("Service name: " + serviceClass);
        Console.println("Package: " + packageName);
        Console.println("");

        String serviceNameLowerCase = serviceName.toLowerCase();
        // Check for existing files
        Path sharedServicesDir = javaBase.resolve("shared/services").resolve(serviceNameLowerCase);
        Path servicesDir = javaBase.resolve("services");

        java.util.List<Path> existingFiles = new java.util.ArrayList<>();

        Path serviceFile = sharedServicesDir.resolve(serviceClass + "Service.java");
        Path asyncFile = sharedServicesDir.resolve(serviceClass + "ServiceAsync.java");
        Path implFile = servicesDir.resolve(serviceClass + "ServiceImpl.java");

        if (Files.exists(serviceFile)) existingFiles.add(serviceFile);
        if (Files.exists(asyncFile)) existingFiles.add(asyncFile);
        if (Files.exists(implFile)) existingFiles.add(implFile);

        if (!existingFiles.isEmpty()) {
            Console.warning("WARNING: The following files will be overwritten:");
            for (Path file : existingFiles) {
                Console.println("  - " + file);
            }
            Console.println("");

            if (!InputReader.confirm("Continue with generation?", false)) {
                Console.warning("Service generation cancelled.");
                return 1;
            }
            Console.println("");
        }

        // Create package directories
        Console.info("Creating package structure...");
        Files.createDirectories(sharedServicesDir);
        Files.createDirectories(servicesDir);

        // Create Service interface
        Console.info("Creating " + serviceClass + "Service.java...");

        Files.writeString(serviceFile,
                String.format("""
                                package %s.shared.services.%s;
                                
                                import com.google.gwt.core.shared.GWT;
                                import com.google.gwt.user.client.rpc.RemoteService;
                                import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
                                
                                @RemoteServiceRelativePath("../%s/service")
                                public interface %sService extends RemoteService {
                                
                                    // Add your service methods here
                                    // Example:
                                    // String exampleMethod(String parameter);
                                
                                    class Instance {
                                        private static final %sServiceAsync INSTANCE = GWT.create(
                                          %sService.class
                                        );
                                
                                        public static %sServiceAsync getInstance() {
                                          return INSTANCE;
                                        }
                                      }
                                
                                }
                                """, packageName, serviceNameLowerCase, serviceNameLowerCase, serviceClass,
                        serviceClass, serviceClass, serviceClass)
        );

        // Create Async interface
        Console.info("Creating " + serviceClass + "ServiceAsync.java...");
        Files.writeString(asyncFile,
                String.format("""
                        package %s.shared.services.%s;
                        
                        import com.google.gwt.user.client.rpc.AsyncCallback;
                        
                        public interface %sServiceAsync {
                        
                            // Add async versions of your service methods here
                            // Example:
                            // void exampleMethod(String parameter, AsyncCallback<String> callback);
                        
                        }
                        """, packageName, serviceNameLowerCase, serviceClass)
        );

        // Create Service implementation
        Console.info("Creating " + serviceClass + "ServiceImpl.java...");
        Files.writeString(implFile,
                String.format("""
                                package %s.services;
                                
                                import %s.shared.services.%s.%sService;
                                import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;
                                import jakarta.servlet.annotation.WebServlet;
                                import org.springframework.stereotype.Service;
                                
                                @WebServlet("/%s/service")
                                @Service
                                public class %sServiceImpl extends RemoteServiceServlet implements %sService {
                                
                                    // Implement your service methods here
                                    // Example:
                                    // @Override
                                    // public String exampleMethod(String parameter) {
                                    //     return "Response: " + parameter;
                                    // }
                                
                                }
                                """, packageName, packageName, serviceName, serviceClass,
                        serviceName.toLowerCase(), serviceClass, serviceClass)
        );

        Console.println("");
        Console.success("===================================");
        Console.success("Service created successfully!");
        Console.success("===================================");
        Console.println("");
        Console.println("Created:");
        Console.println("  - " + sharedServicesDir.resolve(serviceClass + "Service.java"));
        Console.println("  - " + sharedServicesDir.resolve(serviceClass + "ServiceAsync.java"));
        Console.println("  - " + servicesDir.resolve(serviceClass + "ServiceImpl.java"));
        Console.println("");
        Console.println("Next steps:");
        Console.println("  1. Add your service methods to " + serviceClass + "Service.java");
        Console.println("  2. Add corresponding async methods to " + serviceClass + "ServiceAsync.java");
        Console.println("  3. Implement the methods in " + serviceClass + "ServiceImpl.java");
        Console.println("");
        Console.println("Service endpoint: /" + serviceName.toLowerCase() + "/service");
        Console.println("");

        return 0;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
