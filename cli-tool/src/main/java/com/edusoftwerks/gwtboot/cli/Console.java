package com.edusoftwerks.gwtboot.cli;

public class Console {
  // ANSI color codes
  private static final String RED = "\u001B[0;31m";
  private static final String GREEN = "\u001B[0;32m";
  private static final String YELLOW = "\u001B[1;33m";
  private static final String BLUE = "\u001B[0;34m";
  private static final String RESET = "\u001B[0m";

  private static boolean colorsEnabled = true;

  public static void setColorsEnabled(boolean enabled) {
    colorsEnabled = enabled;
  }

  public static void error(String message) {
    if (colorsEnabled) {
      System.err.println(RED + "Error: " + message + RESET);
    } else {
      System.err.println("Error: " + message);
    }
  }

  public static void success(String message) {
    if (colorsEnabled) {
      System.out.println(GREEN + message + RESET);
    } else {
      System.out.println(message);
    }
  }

  public static void info(String message) {
    if (colorsEnabled) {
      System.out.println(BLUE + message + RESET);
    } else {
      System.out.println(message);
    }
  }

  public static void warning(String message) {
    if (colorsEnabled) {
      System.out.println(YELLOW + message + RESET);
    } else {
      System.out.println(message);
    }
  }

  public static void println(String message) {
    System.out.println(message);
  }

  public static void print(String message) {
    System.out.print(message);
  }
}
