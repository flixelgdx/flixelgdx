/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Utility class for obtaining Git information about the current repository Flixel is being run in.
 */
public final class FlixelGitUtil {

  /**
   * A record holding all info about the current Git repository.
   *
   * @param commit The current commit.
   * @param branch The current branch.
   * @param remoteUrl The repository that the game is being worked on.
   * @param isModified Does the current repository have any changes made to it?
   */
  public record RepoInfo(String commit, String branch, String remoteUrl, String isModified) {

    @Override
    public String commit() {
      if (commit == null || commit.isBlank()) {
        return "Could not determine Git commit.";
      }
      return commit.length() > 7 ? commit.substring(0, 8) : commit;
    }

    @Override
    public String branch() {
      if (branch == null || branch.isBlank()) {
        return "Could not determine Git branch.";
      }
      return branch;
    }

    @Override
    public String remoteUrl() {
      if (remoteUrl == null || remoteUrl.isBlank()) {
        return "Could not determine Git remote URL.";
      }
      return remoteUrl;
    }

    @Override
    public String isModified() {
      return (isModified != null && !isModified.isBlank()) ? "Yes" : "No";
    }
  }

  /**
   * Gets basic Git info about the current app running.
   *
   * @return A {@code RepoInfo} record with basic info.
   */
  public static RepoInfo getRepoInfo() {
    String commit = runGitCommand("rev-parse", "HEAD");
    String branch = runGitCommand("rev-parse", "--abbrev-ref", "HEAD");
    String remoteUrl = runGitCommand("config", "--get", "remote.origin.url");
    String isModified = runGitCommand("status", "--porcelain");
    return new RepoInfo(commit, branch, remoteUrl, isModified);
  }

  private static String runGitCommand(String... args) {
    try {
      String[] command = new String[args.length + 1];
      command[0] = "git";
      System.arraycopy(args, 0, command, 1, args.length);
      ProcessBuilder pb = new ProcessBuilder(command);
      pb.redirectErrorStream(true);
      Process process = pb.start();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        return reader.readLine();
      }
    } catch (Exception e) {
      return null;
    }
  }

  private FlixelGitUtil() {
  }
}
