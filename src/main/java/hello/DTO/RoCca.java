package hello.DTO;

public class RoCca {

  private String txnId;

  // private String version;

  private String returnCode;

  private String returnCodeDescription;

  // private String authorizationNumber;

  private String msisdn;

  private String reservedUnits;

  private boolean success = false;

  public String getTxnId() {
    return txnId;
  }

  public void setTxnId(String txnId) {
    this.txnId = txnId;
  }

//  public String getVersion() {
//    return version;
//  }
//
//  public void setVersion(String version) {
//    this.version = version;
//  }

  public String getReturnCode() {
    return returnCode;
  }

  public void setReturnCode(String returnCode) {
    this.returnCode = returnCode;
  }

  public String getReturnCodeDescription() {
    return returnCodeDescription;
  }

  public void setReturnCodeDescription(String returnCodeDescription) {
    this.returnCodeDescription = returnCodeDescription;
  }

//  public String getAuthorizationNumber() {
//    return authorizationNumber;
//  }
//
//  public void setAuthorizationNumber(String authorizationNumber) {
//    this.authorizationNumber = authorizationNumber;
//  }

  public String getMsisdn() {
    return msisdn;
  }

  public void setMsisdn(String msisdn) {
    this.msisdn = msisdn;
  }

  public String getReservedUnits() {
    return reservedUnits;
  }

  public void setReservedUnits(String reservedUnits) {
    this.reservedUnits = reservedUnits;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @Override
  public String toString() {
    return "RoCca [txnId=" + txnId + ", returnCode=" + returnCode + ", returnCodeDescription="
        + returnCodeDescription + ", msisdn=" + msisdn + ", reservedUnits=" + reservedUnits + ", success="
        + success + "]";
  }
}
