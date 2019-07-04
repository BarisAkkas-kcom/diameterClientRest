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
package hello.example;

import hello.DTO.EventAuthorizationRequest;
import hello.DTO.EventAuthorizationResponse;
import hello.client.MPayOcsClient;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdiameter.api.DisconnectCause;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

/**
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class ExampleChargingClient {

  private static final Logger log = Logger.getLogger(ExampleChargingClient.class);

  static {
    // configure logging.
    //configLog4j();
  }

  private MPayOcsClient client;
  private String clientConfigLocation;

  public ExampleChargingClient(String clientConfigLocation) throws Exception {
    super();
    log.debug("ExampleChargingClient using configuraton: " + clientConfigLocation);

    this.clientConfigLocation = clientConfigLocation;
  }

  private static void configLog4j() {
    InputStream inStreamLog4j = ExampleChargingClient.class.getClassLoader().getResourceAsStream("log4j.properties");
    Properties propertiesLog4j = new Properties();
    try {
      propertiesLog4j.load(inStreamLog4j);
      PropertyConfigurator.configure(propertiesLog4j);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (inStreamLog4j != null) {
        try {
          inStreamLog4j.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    log.debug("log4j configured");
  }

  public static void main(String[] args) {
    try {
      final String clientConfig = "client-jdiameter-config.xml";
      ExampleChargingClient toTest = new ExampleChargingClient(clientConfig);
      toTest.setUp();

      EventAuthorizationRequest eventAuthRequest = new EventAuthorizationRequest();
//      eventAuthRequest.setMsisdn("447700700700");
//      eventAuthRequest.setTransactionId(UUID.randomUUID().toString());
//      toTest.sendCcrEvent(eventAuthRequest);

      eventAuthRequest = new EventAuthorizationRequest();
      eventAuthRequest.setMsisdn("447700700777");
      eventAuthRequest.setTransactionId(UUID.randomUUID().toString());
      toTest.sendCcrEvent(eventAuthRequest);
      log.debug("eventAuthRequest is SENT");
//      eventAuthRequest = new EventAuthorizationRequest();
//      eventAuthRequest.setMsisdn("447700700799");
//      eventAuthRequest.setTransactionId(UUID.randomUUID().toString());
//      toTest.sendCcrEvent(eventAuthRequest);

      waitForMessages();
      log.debug("WAIT COMPLETED");
      toTest.tearDown();
      log.debug("System is Exiting");
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void waitForMessages() {
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void setUp() throws Exception {
    try {
      this.client = MPayOcsClient.getInstance(clientConfigLocation);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public void tearDown() {
    if (this.client != null) {
      try {
        this.client.stop(DisconnectCause.REBOOTING);
      } catch (Exception e) {
        e.printStackTrace();
      }
      this.client = null;
    }
  }

  public EventAuthorizationResponse sendCcrEvent(EventAuthorizationRequest eventAuthRequest) throws Exception {
    client.sendEvent(eventAuthRequest);

    return null;
  }
}
