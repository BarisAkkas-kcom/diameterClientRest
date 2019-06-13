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
package hello.functional;

import org.apache.log4j.Logger;
import org.jdiameter.api.*;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.server.impl.helpers.Parameters;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public abstract class TBase implements EventListener<Request, Answer>, NetworkReqListener, StateChangeListener<AppSession> {

  protected final Logger log = Logger.getLogger(getClass());
  protected boolean passed = true;
  protected List<ErrorHolder> errors = new ArrayList<ErrorHolder>();

  protected String serverRealm;
  protected String clientURI;

  protected StackCreator stack;
  protected ISessionFactory sessionFactory;

  protected ApplicationId applicationId;

  public void init(InputStream configStream, String clientID, ApplicationId appId) throws Exception {
    applicationId = appId;
    stack = new StackCreator();
    stack.init(configStream, this, this, clientID, true); // lets always pass
    sessionFactory = (ISessionFactory) this.stack.getSessionFactory();
    Configuration config = stack.getConfiguration();
    log.debug("DIAMETER CONFIG :: " + config);
    clientURI = config.getStringValue(Parameters.OwnDiameterURI.ordinal(), "aaa://localhost:3868");
    log.debug("OwnDiameterURI=" + clientURI);
    Configuration[] realmTable = config.getChildren(Parameters.RealmTable.ordinal());
    for (Configuration realms : realmTable) {
      Configuration[] realmEntries = realms.getChildren(Parameters.RealmEntry.ordinal());
      for (Configuration realmEntry : realmEntries) {
        serverRealm = realmEntry.getStringValue(Parameters.RealmName.ordinal(), "server.mobicents.org");
        log.debug("RealmName=" + serverRealm);
      }
    }
  }

  protected void fail(String msg, Throwable e) {
    passed = false;
    ErrorHolder eh = new ErrorHolder(msg, e);
    errors.add(eh);
  }

  public boolean isPassed() {
    return passed;
  }

  public List<ErrorHolder> getErrors() {
    return errors;
  }

  public String createErrorReport(List<ErrorHolder> errors) {
    if (errors.size() > 0) {
      StringBuilder sb = new StringBuilder();
      for (int index = 0; index < errors.size(); index++) {
        sb.append(errors.get(index));
        if (index + 1 < errors.size()) {
          sb.append("\n");
        }
      }
      return sb.toString();
    }
    else {
      return "";
    }
  }

  public ApplicationId getApplicationId() {
    return applicationId;
  }

  protected String getClientURI() {
    return clientURI;
  }

  protected String getServerRealmName() {
    return serverRealm;
  }

  public Stack getStack() {
    return this.stack;
  }

  // --------- Default Implementation
  // --------- Depending on class it is overridden or by default makes test fail.
  @Override
  public void receivedSuccessMessage(Request request, Answer answer) {
    fail("Received \"SuccessMessage\" event, request[" + request + "], answer[" + answer + "]", null);
  }

  @Override
  public void timeoutExpired(Request request) {
    fail("Received \"Timoeout\" event, request[" + request + "]", null);
  }

  @Override
  public Answer processRequest(Request request) {
    fail("Received \"Request\" event, request[" + request + "]", null);
    return null;
  }

  // --- State Changes --------------------------------------------------------
  @Override
  public void stateChanged(Enum oldState, Enum newState) {
    // NOP
  }

  @Override
  public void stateChanged(AppSession source, Enum oldState, Enum newState) {
    // NOP
  }

}
