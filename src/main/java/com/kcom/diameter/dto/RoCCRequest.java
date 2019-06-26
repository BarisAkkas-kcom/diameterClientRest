package com.kcom.diameter.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class RoCCRequest {

  @JsonIgnore
  private final CountDownLatch countDownLatch = new CountDownLatch(1);

  public CountDownLatch getCountDownLatch() {
    return countDownLatch;
  }

//  public void setCountDownLatch(CountDownLatch countDownLatch) {
//    this.countDownLatch = countDownLatch;
//  }

  private String transactionId = null;

  private String msisdn = null;

  private String balanceNotificationRequired = null;

  private String balanceNotificationMethod = null;

  private String requestingEntity = null;

  private String retailerName = "";

  private String information = "";

  private Date eventTime = null;

  private String direction = null;

  private String directionString = null;

  private String eventType = null;

  private String eventSubType = null;

  private String expirationTime = null;

  private String origType = null;

  private String origAddress = null;

  private String destType = null;

  private String destAddress = null;

  private String volume = null;

  private String duration = null;

  private String location = null;

  private String bearerServiceType = null;

  private String reversalAllowed = null;

  private String reversalAllowedString = null;

  private String authorisationMethod = null;

  private String callType = null;

  private String externalPrice = null;

  private String typeOfNumber = null;

  private String reservationMethod = null;

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public String getMsisdn() {
    return msisdn;
  }

  public void setMsisdn(String msisdn) {
    this.msisdn = msisdn;
  }

  public String getBalanceNotificationRequired() {
    return balanceNotificationRequired;
  }

  public void setBalanceNotificationRequired(String balanceNotificationRequired) {
    this.balanceNotificationRequired = balanceNotificationRequired;
  }

  public String getBalanceNotificationMethod() {
    return balanceNotificationMethod;
  }

  public void setBalanceNotificationMethod(String balanceNotificationMethod) {
    this.balanceNotificationMethod = balanceNotificationMethod;
  }

  public String getRequestingEntity() {
    return requestingEntity;
  }

  public void setRequestingEntity(String requestingEntity) {
    this.requestingEntity = requestingEntity;
  }

  public String getRetailerName() {
    return retailerName;
  }

  public void setRetailerName(String retailerName) {
    this.retailerName = retailerName;
  }

  public String getInformation() {
    return information;
  }

  public void setInformation(String information) {
    this.information = information;
  }

  public Date getEventTime() {
    return eventTime;
  }

  public void setEventTime(Date eventTime) {
    this.eventTime = eventTime;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public String getDirectionString() {
    return directionString;
  }

  public void setDirectionString(String directionString) {
    this.directionString = directionString;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getEventSubType() {
    return eventSubType;
  }

  public void setEventSubType(String eventSubType) {
    this.eventSubType = eventSubType;
  }

  public String getExpirationTime() {
    return expirationTime;
  }

  public void setExpirationTime(String expirationTime) {
    this.expirationTime = expirationTime;
  }

  public String getOrigType() {
    return origType;
  }

  public void setOrigType(String origType) {
    this.origType = origType;
  }

  public String getOrigAddress() {
    return origAddress;
  }

  public void setOrigAddress(String origAddress) {
    this.origAddress = origAddress;
  }

  public String getDestType() {
    return destType;
  }

  public void setDestType(String destType) {
    this.destType = destType;
  }

  public String getDestAddress() {
    return destAddress;
  }

  public void setDestAddress(String destAddress) {
    this.destAddress = destAddress;
  }

  public String getVolume() {
    return volume;
  }

  public void setVolume(String volume) {
    this.volume = volume;
  }

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getBearerServiceType() {
    return bearerServiceType;
  }

  public void setBearerServiceType(String bearerServiceType) {
    this.bearerServiceType = bearerServiceType;
  }

  public String getReversalAllowed() {
    return reversalAllowed;
  }

  public void setReversalAllowed(String reversalAllowed) {
    this.reversalAllowed = reversalAllowed;
  }

  public String getReversalAllowedString() {
    return reversalAllowedString;
  }

  public void setReversalAllowedString(String reversalAllowedString) {
    this.reversalAllowedString = reversalAllowedString;
  }

  public String getAuthorisationMethod() {
    return authorisationMethod;
  }

  public void setAuthorisationMethod(String authorisationMethod) {
    this.authorisationMethod = authorisationMethod;
  }

  public String getCallType() {
    return callType;
  }

  public void setCallType(String callType) {
    this.callType = callType;
  }

  public String getExternalPrice() {
    return externalPrice;
  }

  public void setExternalPrice(String externalPrice) {
    this.externalPrice = externalPrice;
  }

  public String getTypeOfNumber() {
    return typeOfNumber;
  }

  public void setTypeOfNumber(String typeOfNumber) {
    this.typeOfNumber = typeOfNumber;
  }

  public String getReservationMethod() {
    return reservationMethod;
  }

  public void setReservationMethod(String reservationMethod) {
    this.reservationMethod = reservationMethod;
  }

  @Override
  public String toString() {
    return "RoCCRequest [transactionId=" + transactionId + ", msisdn=" + msisdn + ", balanceNotificationRequired=" + balanceNotificationRequired
            + ", balanceNotificationMethod=" + balanceNotificationMethod + ", requestingEntity=" + requestingEntity + ", retailerName=" + retailerName
            + ", information=" + information + ", eventTime=" + eventTime + ", direction=" + direction + ", directionString=" + directionString + ", eventType="
            + eventType + ", eventSubType=" + eventSubType + ", expirationTime=" + expirationTime + ", origType=" + origType + ", origAddress=" + origAddress
            + ", destType=" + destType + ", destAddress=" + destAddress + ", volume=" + volume + ", duration=" + duration + ", location=" + location
            + ", bearerServiceType=" + bearerServiceType + ", reversalAllowed=" + reversalAllowed + ", reversalAllowedString=" + reversalAllowedString
            + ", authorisationMethod=" + authorisationMethod + ", callType=" + callType + ", externalPrice=" + externalPrice + ", typeOfNumber=" + typeOfNumber
            + ", reservationMethod=" + reservationMethod + "]";
  }
}
