/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates, and individual
 * contributors as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package hello.functional.ro.base;

import hello.functional.Utils;
import hello.functional.ro.AbstractClient;
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

/**
 * Base implementation of Client
 *
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class Client extends AbstractClient {

  protected boolean sentINITIAL;
  protected boolean sentINTERIM;
  protected boolean sentTERMINATE;
  protected boolean sentEVENT;
  protected boolean receiveINITIAL;
  protected boolean receiveINTERIM;
  protected boolean receiveTERMINATE;
  protected boolean receiveEVENT;

  /**
   *
   */
  public Client() {
  }

  public String sendInitial() throws Exception {
    ClientRoSession clientRoSession = getSession();
    RoCreditControlRequest initialRequest = createCCR(CC_REQUEST_TYPE_INITIAL, ccRequestNumber, clientRoSession);
    ccRequestNumber++;
    clientRoSession.sendCreditControlRequest(initialRequest);
    Utils.printMessage(log, stack.getDictionary(), initialRequest.getMessage(), true);
    sentINITIAL = true;
    return clientRoSession.getSessionId();
  }

  public String sendInterim() throws Exception {
    if (!receiveINITIAL) {
      throw new Exception();
    }
    ClientRoSession clientRoSession = getSession();
    RoCreditControlRequest interimRequest = createCCR(CC_REQUEST_TYPE_INTERIM, ccRequestNumber, clientRoSession);
    ccRequestNumber++;
    clientRoSession.sendCreditControlRequest(interimRequest);
    Utils.printMessage(log, stack.getDictionary(), interimRequest.getMessage(), true);
    sentINTERIM = true;
    return clientRoSession.getSessionId();
  }

  public String sendTermination() throws Exception {
    if (!receiveINTERIM) {
      throw new Exception();
    }
    ClientRoSession clientRoSession = getSession();
    RoCreditControlRequest terminateRequest = createCCR(CC_REQUEST_TYPE_TERMINATE, ccRequestNumber, clientRoSession);
    ccRequestNumber++;
    clientRoSession.sendCreditControlRequest(terminateRequest);
    Utils.printMessage(log, stack.getDictionary(), terminateRequest.getMessage(), true);
    sentTERMINATE = true;
    return clientRoSession.getSessionId();
  }

  public String sendEvent() throws Exception {
    ClientRoSession clientRoSession = getSession();
    RoCreditControlRequest eventRequest = createCCR(CC_REQUEST_TYPE_TERMINATE, ccRequestNumber, clientRoSession);
    ccRequestNumber++;
    clientRoSession.sendCreditControlRequest(eventRequest);
    Utils.printMessage(log, stack.getDictionary(), eventRequest.getMessage(), true);
    sentEVENT = true;
    return clientRoSession.getSessionId();
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
    try {
      Utils.printMessage(log, stack.getDictionary(), answer.getMessage(), false);
      switch (answer.getRequestTypeAVPValue()) {
        case CC_REQUEST_TYPE_INITIAL:
          if (receiveINITIAL) {
            fail("Received INITIAL more than once!", null);
          }
          receiveINITIAL = true;
          break;

        case CC_REQUEST_TYPE_INTERIM:
          if (receiveINTERIM) {
            fail("Received INTERIM more than once!", null);
          }
          receiveINTERIM = true;
          break;

        case CC_REQUEST_TYPE_TERMINATE:
          if (receiveTERMINATE) {
            fail("Received TERMINATE more than once!", null);
          }
          receiveTERMINATE = true;
          break;

        case CC_REQUEST_TYPE_EVENT:
          if (receiveEVENT) {
            fail("Received EVENT more than once!", null);
          }
          receiveEVENT = true;
          break;

        default:

      }
    } catch (Exception e) {
      e.printStackTrace();
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

  // ------------ getters for some vars;

  public boolean isSentINITIAL() {
    return sentINITIAL;
  }

  public boolean isSentEVENT() {
    return sentEVENT;
  }

  public boolean isReceiveEVENT() {
    return receiveEVENT;
  }

  public boolean isSentINTERIM() {
    return sentINTERIM;
  }

  public boolean isSentTERMINATE() {
    return sentTERMINATE;
  }

  public boolean isReceiveINITIAL() {
    return receiveINITIAL;
  }

  public boolean isReceiveINTERIM() {
    return receiveINTERIM;
  }

  public boolean isReceiveTERMINATE() {
    return receiveTERMINATE;
  }

  // ------------ getters for some vars;

  @Override
  protected int getChargingUnitsTime() {
    return 10;
  }

  @Override
  protected String getServiceContextId() {
    return "tralalalal ID";
  }
}
