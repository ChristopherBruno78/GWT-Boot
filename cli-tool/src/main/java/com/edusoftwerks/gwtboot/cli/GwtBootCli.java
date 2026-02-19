package com.edusoftwerks.gwtboot.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
  name = "gwt-boot",
  description = "GWT Boot CLI - Unified command-line tool for GWT Boot projects",
  mixinStandardHelpOptions = true,
  versionProvider = GwtBootCli.VersionProvider.class,
  subcommands = {
    AppCommand.class,
    ActivityCommand.class,
    ServiceCommand.class,
    ComponentCommand.class,
    DevCommand.class,
    JarCommand.class,
    UninstallCommand.class
  }
)
public class GwtBootCli implements Callable<Integer> {
  private static final String versionInfo =
    "GWT Boot CLI version " + Utils.getVersion();

  static class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
      return new String[] { versionInfo };
    }
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new GwtBootCli()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    Console.info(versionInfo);
    Console.info("");
    Console.info("Usage: gwt-boot <command> [options]");
    Console.info("");
    Console.info("Commands:");
    Console.info(
      "  app <appName>        Generate a new GWT Boot project from archetype"
    );
    Console.info(
      "  activity <name>      Create a new activity in the current project"
    );
    Console.info(
      "  service <name>       Create a new GWT RPC service in the current project"
    );
    Console.info(
      "  component <name>     Create a new UI component in the current project"
    );
    Console.info(
      "  dev [-m <mb>]        Launch GWT CodeServer and Spring Boot for development"
    );
    Console.info(
      "  jar                  Build production JAR with compiled GWT code"
    );
    Console.info(
      "  uninstall            Uninstall GWT Boot CLI from your system"
    );
    Console.info("  help                 Show this help message");
    Console.info("  version              Show version information");
    Console.info("");
    Console.info("Examples:");
    Console.info("  gwt-boot app MyApp");
    Console.info("  gwt-boot activity dashboard");
    Console.info("  gwt-boot service UserAuth");
    Console.info("  gwt-boot component button");
    Console.info("  gwt-boot dev");
    Console.info("  gwt-boot dev -m 4096");
    Console.info("  gwt-boot jar");
    Console.info("");
    return 0;
  }
}
