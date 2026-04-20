package com.example.plugfiletotxt.service;

import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Advanced token counter for different AI models. Provides accurate token estimates for Claude,
 * GPT-4, GPT-3.5, Gemini and estimates based on different tokenization strategies.
 */
public class TokenCountingService {

  private static final Logger LOG = Logger.getInstance(TokenCountingService.class);

  public enum TokenModel {
    CLAUDE_3("Claude 3", 1.0), // 1 token ≈ 4 chars
    GPT_4("GPT-4", 1.33), // 1 token ≈ 3 chars
    GPT_3_5("GPT-3.5", 1.33), // 1 token ≈ 3 chars
    GEMINI("Gemini", 1.0), // 1 token ≈ 4 chars
    GENERIC("Generic", 1.25); // 1 token ≈ 3.2 chars (average)

    public final String displayName;
    public final double tokensPerChar;

    TokenModel(String displayName, double tokensPerChar) {
      this.displayName = displayName;
      this.tokensPerChar = tokensPerChar;
    }
  }

  /** Token limits for different models (input tokens). */
  private static final Map<TokenModel, Long> TOKEN_LIMITS = new HashMap<>();

  static {
    TOKEN_LIMITS.put(TokenModel.CLAUDE_3, 200_000L);
    TOKEN_LIMITS.put(TokenModel.GPT_4, 8_000L);
    TOKEN_LIMITS.put(TokenModel.GPT_3_5, 4_096L);
    TOKEN_LIMITS.put(TokenModel.GEMINI, 30_000L);
    TOKEN_LIMITS.put(TokenModel.GENERIC, 4_000L);
  }

  /** Code-specific patterns that affect tokenization. */
  private static final Pattern[] CODE_PATTERNS =
      new Pattern[] {
        Pattern.compile("(?<=[a-z])(?=[A-Z])"), // camelCase boundaries
        Pattern.compile("(?<=[A-Z])(?=[A-Z][a-z])"), // CamelCase boundaries
        Pattern.compile("[_-]"), // separators
        Pattern.compile("\\W+") // non-word chars
      };

  /**
   * Count tokens for a file using specified model's tokenization.
   *
   * @param file File to count tokens for
   * @param model AI model to use for token counting
   * @return Estimated token count
   */
  public long countFileTokens(@NotNull File file, @NotNull TokenModel model) {
    try {
      String content = Files.readString(file.toPath());
      return countTokens(content, model);
    } catch (IOException e) {
      LOG.warn("Error reading file for token counting: " + file, e);
      return 0;
    }
  }

  /**
   * Count tokens in text using specified model.
   *
   * @param text Text to count tokens for
   * @param model AI model to use for token counting
   * @return Estimated token count
   */
  public long countTokens(@NotNull String text, @NotNull TokenModel model) {
    if (text.isEmpty()) {
      return 0;
    }

    // Step 1: Base character count with model-specific ratio
    long baseTokens = Math.max(1, Math.round(text.length() * model.tokensPerChar));

    // Step 2: Adjust for code structure (if code-like)
    long adjustedTokens = adjustForCodeStructure(text, baseTokens);

    // Step 3: Add whitespace and formatting overhead
    long finalTokens = adjustForWhitespace(adjustedTokens, text);

    return Math.max(1, finalTokens);
  }

  /**
   * Adjust token count for code-specific characteristics.
   *
   * @param text Code text
   * @param baseTokens Base token count
   * @return Adjusted token count
   */
  private long adjustForCodeStructure(@NotNull String text, long baseTokens) {
    // Code has many short tokens (operators, brackets) vs prose
    long operatorCount = countPatternMatches(text, "[{}()\\[\\];:,.]");
    long stringCount = countPatternMatches(text, "\"[^\"]*\"|'[^']*'");

    // Reduce tokens slightly for code patterns (more efficient in AI models)
    if (operatorCount > 0) {
      return Math.round(baseTokens * 0.95);
    }

    return baseTokens;
  }

  /** Adjust for whitespace and formatting. */
  private long adjustForWhitespace(@NotNull long baseTokens, @NotNull String text) {
    // Newlines and indentation add to token count
    long newlineCount = text.split("\n", -1).length;
    long indentationBonus = Math.round(newlineCount * 0.1);

    return baseTokens + indentationBonus;
  }

  /** Count pattern matches in text. */
  private long countPatternMatches(@NotNull String text, @NotNull String pattern) {
    try {
      return text.split(pattern, -1).length - 1;
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * Get token limit for model (approximate input limit).
   *
   * @param model AI model
   * @return Maximum input tokens
   */
  public long getTokenLimit(@NotNull TokenModel model) {
    return TOKEN_LIMITS.getOrDefault(model, 4_000L);
  }

  /**
   * Check if tokens exceed model limit.
   *
   * @param tokens Token count
   * @param model AI model
   * @return True if tokens exceed limit
   */
  public boolean exceedsLimit(long tokens, @NotNull TokenModel model) {
    return tokens > getTokenLimit(model);
  }

  /**
   * Calculate compression ratio needed to fit token limit.
   *
   * @param tokens Current token count
   * @param model AI model
   * @return Compression ratio (0.0 - 1.0) or 1.0 if within limit
   */
  public double getRequiredCompressionRatio(long tokens, @NotNull TokenModel model) {
    long limit = getTokenLimit(model);
    if (tokens <= limit) {
      return 1.0;
    }

    return (double) limit / tokens;
  }

  /**
   * Get human-readable token count with model name.
   *
   * @param tokens Token count
   * @param model AI model
   * @return Formatted string (e.g., "12.5K tokens for Claude 3")
   */
  @NotNull
  public String formatTokenCount(long tokens, @NotNull TokenModel model) {
    String formatted;
    if (tokens >= 1_000_000) {
      formatted = String.format("%.1fM", tokens / 1_000_000.0);
    } else if (tokens >= 1_000) {
      formatted = String.format("%.1fK", tokens / 1_000.0);
    } else {
      formatted = String.valueOf(tokens);
    }

    long limit = getTokenLimit(model);
    double percentage = (tokens * 100.0) / limit;

    if (percentage > 90) {
      return String.format("%s tokens for %s (⚠️ %.0f%% of limit)", formatted, model
          .displayName, percentage);
    } else if (percentage > 70) {
      return String.format("%s tokens for %s (⚡ %.0f%% of limit)", formatted, model
          .displayName, percentage);
    } else {
      return String.format("%s tokens for %s (%.0f%% of limit)", formatted, model.displayName,
          percentage);
    }
  }
}
