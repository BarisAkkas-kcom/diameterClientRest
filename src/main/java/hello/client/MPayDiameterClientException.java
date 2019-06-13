package hello.client;

public class MPayDiameterClientException extends DiameterClientException {

  public MPayDiameterClientException() {
    super();
  }

  public MPayDiameterClientException(String message) {
    super(message);
  }

  public MPayDiameterClientException(Throwable cause) {
    super(cause);
  }

  public MPayDiameterClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public MPayDiameterClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
