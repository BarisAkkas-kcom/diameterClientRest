package com.kcom.diameter.client;

import com.kcom.diameter.exception.DiameterClientException;
import com.kcom.diameter.functional.Utils;
import com.kcom.diameter.functional.ro.AbstractClient;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.auth.events.ReAuthRequest;
import org.jdiameter.api.ro.ClientRoSession;
import org.jdiameter.api.ro.events.RoCreditControlAnswer;
import org.jdiameter.api.ro.events.RoCreditControlRequest;

public abstract class DiameterRoClient extends AbstractClient {

  //protected CountDownLatch countDownLatch = new CountDownLatch(1);

  public final void sendInitial() {
    try {
      ClientRoSession clientRoSession = getSession();
      RoCreditControlRequest initialRequest = createCCR(CC_REQUEST_TYPE_INITIAL, this.ccRequestNumber, clientRoSession);
      ccRequestNumber++;
      clientRoSession.sendCreditControlRequest(initialRequest);
      Utils.printMessage(log, stack.getDictionary(), initialRequest.getMessage(), true);
    }
    catch (Exception e) {
      throw new DiameterClientException(e);
    }
  }

  public final void sendInterim() {
    try {
      ClientRoSession clientRoSession = getSession();
      RoCreditControlRequest interimRequest = createCCR(CC_REQUEST_TYPE_INTERIM, ccRequestNumber, clientRoSession);
      ccRequestNumber++;
      clientRoSession.sendCreditControlRequest(interimRequest);
      Utils.printMessage(log, stack.getDictionary(), interimRequest.getMessage(), true);
    }
    catch (Exception e) {
      throw new DiameterClientException(e);
    }
  }

  public final void sendTermination() {
    try {
      ClientRoSession clientRoSession = getSession();
      RoCreditControlRequest terminateRequest = createCCR(CC_REQUEST_TYPE_TERMINATE, ccRequestNumber, clientRoSession);
      ccRequestNumber++;
      clientRoSession.sendCreditControlRequest(terminateRequest);
      Utils.printMessage(log, stack.getDictionary(), terminateRequest.getMessage(), true);
    }
    catch (Exception e) {
      throw new DiameterClientException(e);
    }
  }

  public final void sendEvent() {
    try {
      ClientRoSession clientRoSession = getSession();
      RoCreditControlRequest eventRequest = createCCR(CC_REQUEST_TYPE_TERMINATE, ccRequestNumber, clientRoSession);
      ccRequestNumber++;
      clientRoSession.sendCreditControlRequest(eventRequest);
      Utils.printMessage(log, stack.getDictionary(), eventRequest.getMessage(), true);
    }
    catch (Exception e) {
      throw new DiameterClientException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.jdiameter.api.cca.ClientCCASessionListener#doCreditControlAnswer( org.jdiameter.api.cca.ClientCCASession,
   * org.jdiameter.api.cca.events.RoCreditControlRequest, org.jdiameter.api.cca.events.JCreditControlAnswer)
   */
  @Override
  public void doCreditControlAnswer(ClientRoSession session, RoCreditControlRequest request, RoCreditControlAnswer answer)
      throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    Utils.printMessage(log, stack.getDictionary(), answer.getMessage(), false);
    switch (answer.getRequestTypeAVPValue()) {
      case CC_REQUEST_TYPE_INITIAL:
        break;

      case CC_REQUEST_TYPE_INTERIM:
        break;

      case CC_REQUEST_TYPE_TERMINATE:
        break;

      case CC_REQUEST_TYPE_EVENT:
        break;

      default:

    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.jdiameter.api.cca.ClientCCASessionListener#doReAuthRequest(org.jdiameter .api.cca.ClientCCASession,
   * org.jdiameter.api.auth.events.ReAuthRequest)
   */
  @Override
  public void doReAuthRequest(ClientRoSession session, ReAuthRequest request)
      throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    fail("Received \"ReAuthRequest\" event, request[" + request + "], on session[" + session + "]", null);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.jdiameter.api.cca.ClientCCASessionListener#doOtherEvent(org.jdiameter .api.app.AppSession,
   * org.jdiameter.api.app.AppRequestEvent, org.jdiameter.api.app.AppAnswerEvent)
   */
  @Override
  public void doOtherEvent(AppSession session, AppRequestEvent request, AppAnswerEvent answer)
      throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
    fail("Received \"Other\" event, request[" + request + "], answer[" + answer + "], on session[" + session + "]", null);
  }
}
