package com.kcom.diameter.dto;

public class RoCCAnswer {

    private String txnId;


    private String returnCode;

    private String returnCodeDescription;


    private String msisdn;

    private String reservedUnits;

    private boolean success = false;

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

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
        return "RoCCAnswer [txnId=" + txnId + ", returnCode=" + returnCode + ", returnCodeDescription="
                + returnCodeDescription + ", msisdn=" + msisdn + ", reservedUnits=" + reservedUnits + ", success="
                + success + "]";
    }
}
