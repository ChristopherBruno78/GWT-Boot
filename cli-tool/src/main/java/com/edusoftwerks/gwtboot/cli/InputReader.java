package com.edusoftwerks.gwtboot.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class InputReader {
  private static final BufferedReader reader = new BufferedReader(
    new InputStreamReader(System.in)
  );

  public static String readLine(String prompt) throws IOException {
    Console.print(prompt);
    return reader.readLine().toLowerCase();
  }

  public static String readLineWithDefault(String prompt, String defaultValue)
    throws IOException {
    Console.print(prompt);
    String input = reader.readLine();
    return (input == null || input.trim().isEmpty())
      ? defaultValue
      : input.trim().toLowerCase();
  }

  public static boolean confirm(String prompt, boolean defaultYes)
    throws IOException {
    String defaultStr = defaultYes ? "Y/n" : "y/N";
    Console.print(prompt + " (" + defaultStr + "): ");
    String input = reader.readLine();

    if (input == null || input.trim().isEmpty()) {
      return defaultYes;
    }

    String normalized = input.trim().toLowerCase();
    return normalized.equals("y") || normalized.equals("yes");
  }
}
