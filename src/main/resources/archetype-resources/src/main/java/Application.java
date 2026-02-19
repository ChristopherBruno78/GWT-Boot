package ${package};

import java.io.File;
import java.util.Objects;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.servlet.context.ServletComponentScan;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Component;

@SpringBootApplication
@ServletComponentScan
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class Application implements ApplicationRunner {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Component
  public static class EmbeddedServletContainerConfig
    implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
      File jsStaticDirectory = new File(
        Objects.requireNonNull(getClass().getResource("/")).getFile(),
        "static"
      );
      if (jsStaticDirectory.exists()) {
        System.out.println(
          "Static directory: " + jsStaticDirectory.getAbsolutePath()
        );
        // You have to set a document root here, otherwise RemoteServiceServlet will fail to find the
        // corresponding serializationPolicyFilePath on a temporary web server started by spring boot application:
        // servlet.getServletContext().getResourceAsStream(serializationPolicyFilePath) returns null.
        // This has impact that java.io.Serializable cannot be used in RPC, only IsSerializable works.
        factory.setDocumentRoot(jsStaticDirectory);
      }
    }
  }

  @Autowired
  BootStrap bootStrap;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    bootStrap.init();
  }
}
