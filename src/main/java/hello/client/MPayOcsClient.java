package hello.client;

import hello.DTO.EventAuthorizationRequest;
import hello.DTO.EventAuthorizationResponse;
import hello.dictionary.AvpDictionary;
import hello.functional.Utils;
import org.apache.log4j.PropertyConfigurator;
import org.jdiameter.api.*;
import org.jdiameter.api.Stack;
import org.jdiameter.api.ro.ClientRoSession;
import org.jdiameter.api.ro.events.RoCreditControlAnswer;
import org.jdiameter.api.ro.events.RoCreditControlRequest;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

@Component
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MPayOcsClient extends DiameterRoClient implements Future <EventAuthorizationResponse> {

  private volatile EventAuthorizationResponse result = null;
  private volatile boolean cancelled = false;
  //private CountDownLatch countDownLatch;
  //final Phaser phaser = new Phaser(1);

  final String clientConfig = "client-jdiameter-config.xml";
  private static final String dictionaryFile = "dictionary.xml";
  private static final Map<String, MPayOcsClient> instances = new HashMap<String, MPayOcsClient>();

  private AvpDictionary dictionary = AvpDictionary.INSTANCE;
  private Map<String, EventAuthorizationRequest> inFlightTxns = new HashMap<String, EventAuthorizationRequest>();

  @PostConstruct
  private void init() {
    log.info("Initialization started ...");
    configLog4j();
    MPayOcsClient instance = new MPayOcsClient(clientConfig);
    instances.put(clientConfig, instance);
    log.info("Initialization finished ...");
  }

  private void configLog4j() {
    InputStream inStreamLog4j = MPayOcsClient.class.getClassLoader().getResourceAsStream("log4j.properties");
    Properties propertiesLog4j = new Properties();
    try {
      propertiesLog4j.load(inStreamLog4j);
      PropertyConfigurator.configure(propertiesLog4j);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      if (inStreamLog4j != null) {
        try {
          inStreamLog4j.close();
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    log.debug("log4j configured");
  }

  public static MPayOcsClient getInstance(String clientConfigLocation) {
    MPayOcsClient instance = instances.get(clientConfigLocation);
    if (instance == null) {
      instance = new MPayOcsClient(clientConfigLocation);
      instances.put(clientConfigLocation, instance);
    }
    return instance;
  }

  // Private force singletons to be created with getInstance(String clientConfigLocation)
  private MPayOcsClient() {
    super();
  }

  private MPayOcsClient(String clientConfigLocation) {
    super();
    // Parse dictionary, it is used for user friendly info.

    try {
      dictionary.parseDictionary(this.getClass().getClassLoader().getResourceAsStream(dictionaryFile));
    } catch (Exception e) {
      e.printStackTrace();
    }
    log.info("AVP Dictionary successfully parsed.");

    try {
      init(this.getClass().getClassLoader().getResourceAsStream(clientConfigLocation), "CLIENT");
      start(Mode.ANY_PEER, 30000, TimeUnit.SECONDS);
      Stack stack = getStack();
      printStackInfo(stack);
    }
    catch (Exception e) {
      throw new MPayDiameterClientException("An error occurred while parsing the Diameter Configuration", e);
    }
  }

  public final void sendEvent(EventAuthorizationRequest eventAuthRequest) {
    try {
      log.debug("Received: " + eventAuthRequest);
      ClientRoSession clientRoSession = getSession();
      log.debug("clientRoSessionId: " + clientRoSession.getSessionId());
      RoCreditControlRequest eventRequest = createCCR(CC_REQUEST_TYPE_EVENT, ccRequestNumber, clientRoSession, eventAuthRequest);
      ccRequestNumber++;
      Utils.printMessage(log, stack.getDictionary(), eventRequest.getMessage(), true);
      inFlightTxns.put(clientRoSession.getSessionId(), eventAuthRequest);
      log.debug("No of inflight requests after adding txin = " + eventAuthRequest.getTransactionId() + " session id = " + clientRoSession.getSessionId() + "=" + inFlightTxns.size());
      clientRoSession.sendCreditControlRequest(eventRequest);
    }
    catch (Exception e) {
      throw new MPayDiameterClientException(e);
    }
  }

  private Future<EventAuthorizationResponse> sendEventAuth(EventAuthorizationRequest eventAuthRequest ) {
    try {
      log.debug("Received: " + eventAuthRequest);
      ClientRoSession clientRoSession = getSession();
      log.debug("clientRoSessionId: " + clientRoSession.getSessionId());
      RoCreditControlRequest eventRequest = createCCR(CC_REQUEST_TYPE_EVENT, ccRequestNumber, clientRoSession, eventAuthRequest);
      ccRequestNumber++;
      Utils.printMessage(log, stack.getDictionary(), eventRequest.getMessage(), true);
      inFlightTxns.put(clientRoSession.getSessionId(), eventAuthRequest);
      log.debug("No of inflight requests after adding txin = " + eventAuthRequest.getTransactionId() + " session id = " + clientRoSession.getSessionId() + "=" + inFlightTxns.size());
      clientRoSession.sendCreditControlRequest(eventRequest);
      return this;
    }
    catch (Exception e) {
      throw new MPayDiameterClientException(e);
    }
  }

  public final EventAuthorizationResponse sendEventAuthAndBlock(EventAuthorizationRequest eventAuthRequest) {
    try {
      //phaser.register();
      //countDownLatch = new CountDownLatch(1);
      //countDownLatch = eventAuthRequest.getCountDownLatch();
      Future<EventAuthorizationResponse> er = sendEventAuth(eventAuthRequest);
      eventAuthRequest.getCountDownLatch().await();
      //return sendEventAuth(eventAuthRequest).get(200,TimeUnit.MILLISECONDS);
      return er.get();
    } catch (InterruptedException e) {
      e.printStackTrace();
      eventAuthRequest.getCountDownLatch().countDown();
      //phaser.arriveAndDeregister();
      return null;
    } catch (ExecutionException e ) {
      e.printStackTrace();
      eventAuthRequest.getCountDownLatch().countDown();
      //phaser.arriveAndDeregister();
      return null;
    }
  }

  protected RoCreditControlRequest createCCR(int ccRequestType, int requestNumber, ClientRoSession ccaSession, EventAuthorizationRequest eventAuthRequest)
      throws Exception {
    RoCreditControlRequest ccr = createCCR(ccRequestType, requestNumber, ccaSession);

    for (ApplicationId appid : ccr.getMessage().getApplicationIdAvps())
         log.debug("AUTHAPPID : " + appid.getAuthAppId() + " ACCTAPPID" + appid.getAcctAppId() + " VENDORID" + appid.getVendorId());

    // AVPs present by default: Origin-Host, Origin-Realm, Session-Id,
    // Vendor-Specific-Application-Id, Destination-Realm
         AvpSet ccrAvps = ccr.getMessage().getAvps();

    // *[ Subscription-Id ]
    // 8.46. Subscription-Id AVP
    //
    // The Subscription-Id AVP (AVP Code 443) is used to identify the end
    // user's subscription and is of type Grouped. The Subscription-Id AVP
    // includes a Subscription-Id-Data AVP that holds the identifier and a
    // Subscription-Id-Type AVP that defines the identifier type.
    //
    // It is defined as follows (per the grouped-avp-def of RFC 3588
    // [DIAMBASE]):
    //
    // Subscription-Id ::= < AVP Header: 443 >
    // { Subscription-Id-Type }
    // { Subscription-Id-Data }
    AvpSet subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID);

    // 8.47. Subscription-Id-Type AVP
    //
    // The Subscription-Id-Type AVP (AVP Code 450) is of type Enumerated,
    // and it is used to determine which type of identifier is carried by
    // the Subscription-Id AVP.
    //
    // This specification defines the following subscription identifiers.
    // However, new Subscription-Id-Type values can be assigned by an IANA
    // designated expert, as defined in section 12. A server MUST implement
    // all the Subscription-Id-Types required to perform credit
    // authorization for the services it supports, including possible future
    // values. Unknown or unsupported Subscription-Id-Types MUST be treated
    // according to the 'M' flag rule, as defined in [DIAMBASE].
    //
    // END_USER_E164 0
    // The identifier is in international E.164 format (e.g., MSISDN),
    // according to the ITU-T E.164 numbering plan defined in [E164] and
    // [CE164].
    //
    // END_USER_IMSI 1
    // The identifier is in international IMSI format, according to the
    // ITU-T E.212 numbering plan as defined in [E212] and [CE212].
    //
    // END_USER_SIP_URI 2
    // The identifier is in the form of a SIP URI, as defined in [SIP].
    //
    // END_USER_NAI 3
    // The identifier is in the form of a Network Access Identifier, as
    // defined in [NAI].
    //
    // END_USER_PRIVATE 4
    // The Identifier is a credit-control server private identifier.
    subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, 0);

    // 8.48. Subscription-Id-Data AVP
    //
    // The Subscription-Id-Data AVP (AVP Code 444) is used to identify the
    // end user and is of type UTF8String. The Subscription-Id-Type AVP
    // defines which type of identifier is used.
    String subscriptionIdData = eventAuthRequest.getMsisdn();
    subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, subscriptionIdData, false);

    return ccr;
  }

  private EventAuthorizationResponse getEventAuthorizationResponse(EventAuthorizationRequest eventAuthRequest, RoCreditControlAnswer answer) {
    EventAuthorizationResponse eventAuthResponse = new EventAuthorizationResponse();
    eventAuthResponse.setMsisdn(eventAuthRequest.getMsisdn());
    try {
      final long resultCode = answer.getResultCodeAvp().getUnsigned32();
      eventAuthResponse.setReturnCode(String.valueOf(resultCode));
      eventAuthResponse.setSuccess(resultCode == 2001);
      eventAuthResponse.setTxnId(eventAuthRequest.getTransactionId());
      if (resultCode == 2001) {
        AvpSet avps = answer.getMessage().getAvps();
        AvpSet grantedServiceUnit = avps.getAvps(Avp.GRANTED_SERVICE_UNIT);
        log.debug("Granted-Service-Unit(431)=" + grantedServiceUnit);
        Utils.printAvps(log, stack.getDictionary(), grantedServiceUnit);
        //AvpSet ccTimeSet = avps.getAvp(Avp.GRANTED_SERVICE_UNIT).getGrouped().getAvps(Avp.CC_TIME);
        //log.debug("CC-Time(420)=" + ccTime);
        //long serviceSpecificUnits = ccTime.getUnsigned64();
        long serviceSpecificUnits = avps.getAvp(Avp.GRANTED_SERVICE_UNIT).getGrouped().getAvp(Avp.CC_TIME).getInteger32();
        log.debug("serviceSpecificUnits=" + serviceSpecificUnits);
        String serviceSpecificUnitsString = String.valueOf(serviceSpecificUnits);
        eventAuthResponse.setReservedUnits(serviceSpecificUnitsString);
      }
    }
    catch (AvpDataException | InternalException | IllegalDiameterStateException e) {
      throw new MPayDiameterClientException(e);
    }
    return eventAuthResponse;
  }

  /*
   * (non-Javadoc)
   * @see org.jdiameter.api.cca.ClientCCASessionListener#doCreditControlAnswer( org.jdiameter.api.cca.ClientCCASession,
   * org.jdiameter.api.cca.events.RoCreditControlRequest, org.jdiameter.api.cca.events.JCreditControlAnswer)
   */
  @Override
  public void doCreditControlAnswer(ClientRoSession session, RoCreditControlRequest request, RoCreditControlAnswer answer)
          throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {

    log.debug("inside MPAYOCSClient doCreditControlAnswer ");

    Utils.printMessage(log, super.stack.getDictionary(), answer.getMessage(), false);
    String sessionId = "";

    synchronized (this) {
      try {
        sessionId = answer.getMessage().getSessionId();
        EventAuthorizationRequest eventAuthRequest = inFlightTxns.get(sessionId);
        if (eventAuthRequest != null) {
          // Build an EventAuth Response
          EventAuthorizationResponse eventAuthResponse = getEventAuthorizationResponse(eventAuthRequest, answer);
          log.debug("About to send: " + eventAuthResponse);
          this.result = eventAuthResponse;
          //countDownLatch = eventAuthRequest.getCountDownLatch();
          eventAuthRequest.getCountDownLatch().countDown();
          Thread.sleep(50);
        }

      }
       /*catch (InterruptedException e) {
        e.printStackTrace();
      }*/ catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        // Remove the in-flight txn

        //phaser.arriveAndDeregister();
        inFlightTxns.remove(session.getSessionId());
        log.debug("No of inflight requests after removing " + sessionId + "=" + inFlightTxns.size());
        // Close the session
        ClientRoSession clientRoSession = fetchSession(sessionId);
        clientRoSession.release();
      }
    }
  }

  @Override
  protected int getChargingUnitsTime() {
    return 1;
  }

  @Override
  protected String getServiceContextId() {
    return "32270@3gpp.org";
  }

  private void printStackInfo(Stack stack) {
    // Print info about application
    Set<org.jdiameter.api.ApplicationId> appIds = stack.getMetaData().getLocalPeer().getCommonApplications();

    log.info("Diameter Stack :: Supporting " + appIds.size() + " applications.");
    for (org.jdiameter.api.ApplicationId x : appIds) {
      log.info("Diameter Stack  :: Common :: " + x);
    }

    List<Peer> peers;
    try {
      peers = stack.unwrap(PeerTable.class).getPeerTable();
    }
    catch (InternalException e) {
      throw new MPayDiameterClientException(e);
    }
    if (peers.size() == 1) {
      // ok
      log.debug("Diameter Stack :: Peers OK (1) :: " + peers.iterator().next());
    }
    else if (peers.size() > 1) {
      // works better with replicated, since disconnected peers are also listed
      boolean foundConnected = false;
      for (Peer p : peers) {
        if (p.getState(PeerState.class).equals(PeerState.OKAY)) {
          if (foundConnected) {
            throw new MPayDiameterClientException("Diameter Stack :: Wrong number of connected peers :: " + peers);
          }
          foundConnected = true;
        }
      }
    }
    else {
      throw new MPayDiameterClientException("Diameter Stack :: Wrong number (0) of connected peers :: " + peers);
    }
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (isDone()) {
      return false;
    } else {
     // countDownLatch.countDown();
      //phaser.arriveAndDeregister();
      cancelled = true;
      return !isDone();
    }
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public boolean isDone() {
    //return countDownLatch.getCount() == 0;
    //return phaser.isTerminated();
    return false;
  }

  @Override
  public EventAuthorizationResponse get() throws InterruptedException, ExecutionException {
    //Thread.sleep(200);
    //countDownLatch.await();
   // phaser.arriveAndAwaitAdvance();
    return result;
  }

  @Override
  public EventAuthorizationResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    //countDownLatch.await(timeout, unit);
    //phaser.arriveAndAwaitAdvance();
    return result;
  }
}
