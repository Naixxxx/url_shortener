package org.com.url_shortener.core.exceptions;

public class LinkExpiredException extends RuntimeException {
  public LinkExpiredException(String message) {
    super(message);
  }
}
