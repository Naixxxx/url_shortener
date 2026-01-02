package org.com.url_shortener.core.exceptions;

public class InvalidLimitException extends RuntimeException {
  public InvalidLimitException(String message) {
    super(message);
  }
}
