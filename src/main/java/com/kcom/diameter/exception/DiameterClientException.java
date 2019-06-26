package com.kcom.diameter.exception;

public class DiameterClientException extends RuntimeException {

  public DiameterClientException() {
    super();
  }

  public DiameterClientException(String message) {
    super(message);
  }

  public DiameterClientException(Throwable cause) {
    super(cause);
  }

  public DiameterClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public DiameterClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
