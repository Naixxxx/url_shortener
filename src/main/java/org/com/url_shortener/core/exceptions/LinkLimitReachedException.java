package org.com.url_shortener.core.exceptions;

public class LinkLimitReachedException extends RuntimeException {
  public LinkLimitReachedException(String message) {
    super(message);
  }
}
