//package hello.client;
//
//import com.kcom.diameter.ro.messages.RoCca;
//import com.kcom.diameter.ro.messages.RoCcr;
//import com.kcom.diameter.ro.messages.composites.ServiceSpecificUnit;
//import hello.dictionary.AvpDictionary;
//import hello.functional.Utils;
//import org.apache.log4j.Logger;
//import org.jdiameter.api.Answer;
//import org.jdiameter.api.ApplicationId;
//import org.jdiameter.api.Avp;
//import org.jdiameter.api.AvpDataException;
//import org.jdiameter.api.AvpSet;
//import org.jdiameter.api.Configuration;
//import org.jdiameter.api.EventListener;
//import org.jdiameter.api.IllegalDiameterStateException;
//import org.jdiameter.api.InternalException;
//import org.jdiameter.api.Mode;
//import org.jdiameter.api.Peer;
//import org.jdiameter.api.PeerState;
//import org.jdiameter.api.PeerTable;
//import org.jdiameter.api.Request;
//import org.jdiameter.api.Session;
//import org.jdiameter.api.Stack;
//import org.jdiameter.api.ro.events.RoCreditControlRequest;
//import org.jdiameter.client.api.ISessionFactory;
//import org.jdiameter.client.impl.StackImpl;
//import org.jdiameter.server.impl.helpers.Parameters;
//import org.jdiameter.server.impl.helpers.XMLConfiguration;
//
//import java.io.InputStream;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.UUID;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//
//public class DiameterRoClientHandler implements EventListener<Request, Answer> {
//
//  private static String dictionaryFile = "dictionary.xml";
//  private static final Map<String, DiameterRoClientHandler> instances = new HashMap<String, DiameterRoClientHandler>();
//  private final CountDownLatch countDownLatch = new CountDownLatch(1);
//
//  public static String clientConfig = "client-jdiameter-config.xml";
//  private static XMLConfiguration config;
//  private RoCca result = null;
//  private static AvpDictionary dictionary = AvpDictionary.INSTANCE;
//  private static Map<String, RoCcr> inFlightTxns = new HashMap<String, RoCcr>();
//  private static StackImpl stack;
//  private static ISessionFactory sessionFactory;
//  private static int ccRequestNumber;
//
//  private static long vendorId = 10415;
//  private static int authAppId = 4;
//  private static ApplicationId applicationID = ApplicationId.createByAuthAppId(10415, 4);
//  private static String realmName = "client.kcom.com";
//  private static String clientURI;
//  private static String serverRealm = "server.kcom.com";
//
//  protected static final Logger log = Logger.getLogger(DiameterRoClientHandler.class);
//
//  public DiameterRoClientHandler(){
//  }
//
//  public DiameterRoClientHandler(String config, String dictionary){
//    clientConfig = config;
//    dictionaryFile = dictionary;
//  }
//
//  protected synchronized StackImpl getStack() {
//    if (stack == null) {
//      stack = initStack();
//    }
//
//    return stack;
//  }
//
//  private synchronized StackImpl initStack() {
//
//    try {
//      dictionary.parseDictionary(DiameterRoClientHandler.class.getClassLoader().getResourceAsStream(dictionaryFile));
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//    log.info("AVP Dictionary successfully parsed.");
//
//    try {
//      stack = new StackImpl();
//      log.info("clientConfig : " + clientConfig);
//      InputStream configStream = DiameterRoClientHandler.class.getClassLoader().getResourceAsStream(clientConfig);
//      config = new XMLConfiguration(configStream);
//      stack.init(config);
//      sessionFactory = (ISessionFactory)stack.getSessionFactory();
//
//      Configuration config = stack.getConfiguration();
//      log.debug("DIAMETER CONFIG :: " + config);
//      clientURI = config.getStringValue(Parameters.OwnDiameterURI.ordinal(), "aaa://localhost:3868");
//      log.debug("OwnDiameterURI=" + clientURI);
//      realmName = config.getStringValue(Parameters.OwnRealm.ordinal(), "client.kcom.com");
//      log.debug("realmName=" + realmName);
////            Configuration[] realmTable = config.getChildren(Parameters.RealmTable.ordinal());
////            for (Configuration realms : realmTable) {
////                Configuration[] realmEntries = realms.getChildren(Parameters.RealmEntry.ordinal());
////                for (Configuration realmEntry : realmEntries) {
////                    serverRealm = realmEntry.getStringValue(Parameters.RealmName.ordinal(), "server.mobicents.org");
////                    log.debug("RealmName=" + serverRealm);
////                }
////            }
//
////            Configuration[] appIds = config.getChildren(Parameters.ApplicationId.ordinal());
////            log.debug(appIds.length);
////            for (Configuration appId : appIds){
////                Long vendorId = appId.getLongValue(Parameters.VendorId.ordinal(),100);
////                log.debug("vendorId " + vendorId);
////                break;
////            }
//
//
//
//      Thread.sleep(500L);
//      stack.start(Mode.ANY_PEER, 30000, TimeUnit.SECONDS);
//      //Stack stack = getStack();
//      printStackInfo(stack);
//      Thread.sleep(3000L);
//      return stack;
//    } catch (Exception e) {
//      throw new DiameterClientException("An error occurred while parsing the Diameter Configuration", e);
//    }
//  }
//
////    private void start() {
////        try {
////            log.debug("start() - START ");
////            sessionFactory = (ISessionFactory) this.stack.getSessionFactory();
////            roSessionFactory = new RoSessionFactoryImpl(sessionFactory);
////            roSessionFactory.setStateListener(this);
////            roSessionFactory.setClientSessionListener(this);
////            roSessionFactory.setClientContextListener(this);
////            sessionFactory.registerAppFacory(ClientRoSession.class, roSessionFactory);
////            stack.start();
////            log.debug("start() - COMPLETE ");
////        } catch (IllegalDiameterStateException | InternalException e) {
////            e.printStackTrace();
////            this.destroy();
////        }
////    }
//
//  private void destroy() {
//    log.debug("destroy() - START ");
//    if (stack != null) {
//      //Give time for Stack to stop
//      try {
//        stack.stop(5000L, TimeUnit.MILLISECONDS, 0);
//        stack.destroy();
//        log.debug("destroy() - COMPLETE ");
//      } catch (IllegalDiameterStateException | InternalException e) {
//        e.printStackTrace();
//      }
//    }
//
//  }
//
////    private void configLog4j() {
////        InputStream inStreamLog4j = DiameterRoClientHandler.class.getClassLoader().getResourceAsStream("log4j.properties");
////        Properties propertiesLog4j = new Properties();
////        try {
////            propertiesLog4j.load(inStreamLog4j);
////            PropertyConfigurator.configure(propertiesLog4j);
////        } catch (Exception e) {
////            e.printStackTrace();
////        } finally {
////            if (inStreamLog4j != null) {
////                try {
////                    inStreamLog4j.close();
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
////            }
////        }
////        log.debug("log4j configured");
////    }
//
//  private void sendEventAsFuture(RoCcr roCcr) {
//    try {
//      log.debug("Received: " + roCcr);
//      stack = getStack();
//      Session clientRoSession = sessionFactory.getNewSession();
//      log.debug("clientRoSessionId: " + clientRoSession.getSessionId());
//      Request eventRequest = createCCR(ccRequestNumber, clientRoSession, roCcr);
//      ccRequestNumber++;
//      Utils.printMessage(log, getStack().getDictionary(), eventRequest, true);
//      inFlightTxns.put(clientRoSession.getSessionId(), roCcr);
//      log.debug("No of inflight requests after adding txin = " + roCcr.getSubscriptionId() + " session id = " + clientRoSession.getSessionId() +
//          "=" + inFlightTxns.size());
//      clientRoSession.send(eventRequest,this,10000,TimeUnit.MILLISECONDS);
//      //return this;
//    } catch (Exception e) {
//      throw new DiameterClientException(e);
//    }
//  }
//
//  public RoCca sendEvent(RoCcr roCcr) {
//    try {
//      sendEventAsFuture(roCcr);
//      return this.get();
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//      this.countDownLatch.countDown();
//      return null;
//    } catch (ExecutionException e) {
//      e.printStackTrace();
//      this.countDownLatch.countDown();
//      return null;
//    }
//  }
//
//  private Request createCCR(int requestNumber, Session ccaSession, RoCcr roCcr)
//      throws Exception {
//    //RoCreditControlRequest ccr = createCCR(ccRequestType, requestNumber, ccaSession);
//
//    // Create Credit-Control-Request
////    RoCreditControlRequest ccr = new RoCreditControlRequestImpl(ccaSession.getSessions().get(0)
////        .createRequest(RoCreditControlRequest.code, applicationID, realmName));
//    Request ccr =  ccaSession.createRequest(RoCreditControlRequest.code, applicationID, realmName);
//
//    // AVPs present by default: Origin-Host, Origin-Realm, Session-Id,
//    // Vendor-Specific-Application-Id, Destination-Realm
//    AvpSet ccrAvps = ccr.getAvps();
//
//    // Add remaining AVPs ... from RFC 4006:
//    // <CCR> ::= < Diameter Header: 272, REQ, PXY >
//    // < Session-Id >
//    // ccrAvps.addAvp(Avp.SESSION_ID, s.getSessionId());
//
//    // { Origin-Host }
//    ccrAvps.removeAvp(Avp.ORIGIN_HOST);
//    ccrAvps.addAvp(Avp.ORIGIN_HOST, clientURI, true);
//
//    // { Origin-Realm }
//    // ccrAvps.addAvp(Avp.ORIGIN_REALM, realmName, true);
//
//    // { Destination-Realm }
//    // ccrAvps.addAvp(Avp.DESTINATION_REALM, realmName, true);
//
//    // { Auth-Application-Id }
//    // ccrAvps.addAvp(Avp.AUTH_APPLICATION_ID, 4);
//
//    // { Service-Context-Id }
//    // 8.42. Service-Context-Id AVP
//    //
//    // The Service-Context-Id AVP is of type UTF8String (AVP Code 461) and
//    // contains a unique identifier of the Diameter credit-control service
//    // specific document that applies to the request (as defined in section
//    // 4.1.2). This is an identifier allocated by the service provider, by
//    // the service element manufacturer, or by a standardization body, and
//    // MUST uniquely identify a given Diameter credit-control service
//    // specific document. The format of the Service-Context-Id is:
//    //
//    // "service-context" "@" "domain"
//    //
//    // service-context = Token
//    //
//    // The Token is an arbitrary string of characters and digits.
//    //
//    // 'domain' represents the entity that allocated the Service-Context-Id.
//    // It can be ietf.org, 3gpp.org, etc., if the identifier is allocated by
//    // a standardization body, or it can be the FQDN of the service provider
//    // (e.g., provider.example.com) or of the vendor (e.g.,
//    // vendor.example.com) if the identifier is allocated by a private
//    // entity.
//    //
//    // This AVP SHOULD be placed as close to the Diameter header as
//    // possible.
//    //
//    // Service-specific documents that are for private use only (i.e., to
//    // one provider's own use, where no interoperability is deemed useful)
//    // may define private identifiers without need of coordination.
//    // However, when interoperability is wanted, coordination of the
//    // identifiers via, for example, publication of an informational RFC is
//    // RECOMMENDED in order to make Service-Context-Id globally available.
//
//
//    String serviceContextId = roCcr.getServiceContextId();
//    if (serviceContextId == null) {
//      serviceContextId = UUID.randomUUID().toString().replaceAll("-", "") + "@kcom.com";
//    }
//    ccrAvps.addAvp(Avp.SERVICE_CONTEXT_ID, serviceContextId, false);
//
//    // { CC-Request-Type }
//    // 8.3. CC-Request-Type AVP
//    //
//    // The CC-Request-Type AVP (AVP Code 416) is of type Enumerated and
//    // contains the reason for sending the credit-control request message.
//    // It MUST be present in all Credit-Control-Request messages. The
//    // following values are defined for the CC-Request-Type AVP:
//    //
//    // INITIAL_REQUEST 1
//    // An Initial request is used to initiate a credit-control session,
//    // and contains credit control information that is relevant to the
//    // initiation.
//    //
//    // UPDATE_REQUEST 2
//    // An Update request contains credit-control information for an
//    // existing credit-control session. Update credit-control requests
//    // SHOULD be sent every time a credit-control re-authorization is
//    // needed at the expiry of the allocated quota or validity time.
//    // Further, additional service-specific events MAY trigger a
//    // spontaneous Update request.
//    //
//    // TERMINATION_REQUEST 3
//    // A Termination request is sent to terminate a credit-control
//    // session and contains credit-control information relevant to the
//    // existing session.
//    //
//    // EVENT_REQUEST 4
//    // An Event request is used when there is no need to maintain any
//    // credit-control session state in the credit-control server. This
//    // request contains all information relevant to the service, and is
//    // the only request of the service. The reason for the Event request
//    // is further detailed in the Requested-Action AVP. The Requested-
//    // Action AVP MUST be included in the Credit-Control-Request message
//    // when CC-Request-Type is set to EVENT_REQUEST.
//    ccrAvps.addAvp(Avp.CC_REQUEST_TYPE, roCcr.getCcRequestType());
//
//    // { CC-Request-Number }
//    // 8.2. CC-Request-Number AVP
//    //
//    // The CC-Request-Number AVP (AVP Code 415) is of type Unsigned32 and
//    // identifies this request within one session. As Session-Id AVPs are
//    // globally unique, the combination of Session-Id and CC-Request-Number
//    // AVPs is also globally unique and can be used in matching credit-
//    // control messages with confirmations. An easy way to produce unique
//    // numbers is to set the value to 0 for a credit-control request of type
//    // INITIAL_REQUEST and EVENT_REQUEST and to set the value to 1 for the
//    // first UPDATE_REQUEST, to 2 for the second, and so on until the value
//    // for TERMINATION_REQUEST is one more than for the last UPDATE_REQUEST.
//    ccrAvps.addAvp(Avp.CC_REQUEST_NUMBER, requestNumber);
//
//    // [ Destination-Host ]
//    ccrAvps.removeAvp(Avp.DESTINATION_HOST);
//    // ccrAvps.addAvp(Avp.DESTINATION_HOST, ccRequestType == 2 ?
//    // serverURINode1 : serverURINode1, false);
//
//    // [ User-Name ]
//    // [ CC-Sub-Session-Id ]
//    // [ Acct-Multi-Session-Id ]
//    // [ Origin-State-Id ]
//    // [ Event-Timestamp ]
//
//    // [ Service-Identifier ]
//    // [ Termination-Cause ]
//
//    // [ Requested-Service-Unit ]
//    // 8.18. Requested-Service-Unit AVP
//    //
//    // The Requested-Service-Unit AVP (AVP Code 437) is of type Grouped and
//    // contains the amount of requested units specified by the Diameter
//    // credit-control client. A server is not required to implement all the
//    // unit types, and it must treat unknown or unsupported unit types as
//    // invalid AVPs.
//    //
//    // The Requested-Service-Unit AVP is defined as follows (per the
//    // grouped-avp-def of RFC 3588 [DIAMBASE]):
//    //
//    // Requested-Service-Unit ::= < AVP Header: 437 >
//    // [ CC-Time ]
//    // [ CC-Money ]
//    // [ CC-Total-Octets ]
//    // [ CC-Input-Octets ]
//    // [ CC-Output-Octets ]
//    // [ CC-Service-Specific-Units ]
//    // *[ AVP ]
//    AvpSet rsuAvp = ccrAvps.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
//
//    // 8.21. CC-Time AVP
//    //
//    // The CC-Time AVP (AVP Code 420) is of type Unsigned32 and indicates
//    // the length of the requested, granted, or used time in seconds.
//    rsuAvp.addAvp(Avp.CC_TIME, roCcr.getRequestedServiceUnit().getCcServiceSpecificUnits());
//
//    // [ Requested-Action ]
//    // *[ Used-Service-Unit ]
//    // 8.19. Used-Service-Unit AVP
//    //
//    // The Used-Service-Unit AVP is of type Grouped (AVP Code 446) and
//    // contains the amount of used units measured from the point when the
//    // service became active or, if interim interrogations are used during
//    // the session, from the point when the previous measurement ended.
//    //
//    // The Used-Service-Unit AVP is defined as follows (per the grouped-
//    // avp-def of RFC 3588 [DIAMBASE]):
//    //
//    // Used-Service-Unit ::= < AVP Header: 446 >
//    // [ Tariff-Change-Usage ]
//    // [ CC-Time ]
//    // [ CC-Money ]
//    // [ CC-Total-Octets ]
//    // [ CC-Input-Octets ]
//    // [ CC-Output-Octets ]
//    // [ CC-Service-Specific-Units ]
//    // *[ AVP ]
//
//    for (ApplicationId appid : ccr.getApplicationIdAvps()) {
//      log.debug("AUTHAPPID : " + appid.getAuthAppId() + " ACCTAPPID : " + appid.getAcctAppId() + " VENDORID : " + appid.getVendorId());
//    }
//
//    // *[ Subscription-Id ]
//    // 8.46. Subscription-Id AVP
//    //
//    // The Subscription-Id AVP (AVP Code 443) is used to identify the end
//    // user's subscription and is of type Grouped. The Subscription-Id AVP
//    // includes a Subscription-Id-Data AVP that holds the identifier and a
//    // Subscription-Id-Type AVP that defines the identifier type.
//    //
//    // It is defined as follows (per the grouped-avp-def of RFC 3588
//    // [DIAMBASE]):
//    //
//    // Subscription-Id ::= < AVP Header: 443 >
//    // { Subscription-Id-Type }
//    // { Subscription-Id-Data }
//    AvpSet subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID);
//
//    // 8.47. Subscription-Id-Type AVP
//    //
//    // The Subscription-Id-Type AVP (AVP Code 450) is of type Enumerated,
//    // and it is used to determine which type of identifier is carried by
//    // the Subscription-Id AVP.
//    //
//    // This specification defines the following subscription identifiers.
//    // However, new Subscription-Id-Type values can be assigned by an IANA
//    // designated expert, as defined in section 12. A server MUST implement
//    // all the Subscription-Id-Types required to perform credit
//    // authorization for the services it supports, including possible future
//    // values. Unknown or unsupported Subscription-Id-Types MUST be treated
//    // according to the 'M' flag rule, as defined in [DIAMBASE].
//    //
//    // END_USER_E164 0
//    // The identifier is in international E.164 format (e.g., MSISDN),
//    // according to the ITU-T E.164 numbering plan defined in [E164] and
//    // [CE164].
//    //
//    // END_USER_IMSI 1
//    // The identifier is in international IMSI format, according to the
//    // ITU-T E.212 numbering plan as defined in [E212] and [CE212].
//    //
//    // END_USER_SIP_URI 2
//    // The identifier is in the form of a SIP URI, as defined in [SIP].
//    //
//    // END_USER_NAI 3
//    // The identifier is in the form of a Network Access Identifier, as
//    // defined in [NAI].
//    //
//    // END_USER_PRIVATE 4
//    // The Identifier is a credit-control server private identifier.
//    subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, roCcr.getSubscriptionId().getSubscriptionIdType());
//
//    // 8.48. Subscription-Id-Data AVP
//    //
//    // The Subscription-Id-Data AVP (AVP Code 444) is used to identify the
//    // end user and is of type UTF8String. The Subscription-Id-Type AVP
//    // defines which type of identifier is used.
//    String subscriptionIdData = roCcr.getSubscriptionId().getSubscriptionIdData();
//    subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, subscriptionIdData, false);
//
//    return ccr;
//  }
//
//  private RoCca getEventAuthorizationResponse(RoCcr roCcr, Answer answer) {
//    RoCca roCca = new RoCca();
//    //roCca.setMsisdn(roCcr.getMsisdn());
//    try {
//      int resultCode = answer.getResultCode().getInteger32();
//      roCca.setResultCode(resultCode);
//      roCca.setCcRequestType(roCcr.getCcRequestType());
//      //roCca.setSessionId(roCcr.getSubscriptionId().getSubscriptionIdData());
//      if (resultCode == 2001) {
//        AvpSet avps = answer.getAvps();
//        AvpSet grantedServiceUnit = avps.getAvps(Avp.GRANTED_SERVICE_UNIT);
//        log.debug("Granted-Service-Unit(431)=" + grantedServiceUnit);
//        Utils.printAvps(log, stack.getDictionary(), grantedServiceUnit);
//        int grantedServiceUnits = avps.getAvp(Avp.GRANTED_SERVICE_UNIT).getGrouped().getAvp(Avp.CC_TIME).getInteger32();
//        log.debug("grantedServiceUnits=" + grantedServiceUnits);
//        ServiceSpecificUnit serviceSpecificUnits = new ServiceSpecificUnit();
//        serviceSpecificUnits.setCcServiceSpecificUnits(grantedServiceUnits);
//        roCca.setGrantedServiceUnit(serviceSpecificUnits);
//      }
//    } catch (AvpDataException | IllegalDiameterStateException e) {
//      throw new DiameterClientException(e);
//    }
//    return roCca;
//  }
//
//  private static void printStackInfo(Stack stack) {
//    // Print info about application
//    Set<org.jdiameter.api.ApplicationId> appIds = stack.getMetaData().getLocalPeer().getCommonApplications();
//
//    log.info("Diameter Stack :: Supporting " + appIds.size() + " applications.");
//    for (org.jdiameter.api.ApplicationId x : appIds) {
//      log.info("Diameter Stack  :: Common :: " + x);
//    }
//
//    List<Peer> peers;
//    try {
//      peers = stack.unwrap(PeerTable.class).getPeerTable();
//    } catch (InternalException e) {
//      throw new DiameterClientException(e);
//    }
//    if (peers.size() == 1) {
//      // ok
//      log.debug("Diameter Stack :: Peers OK (1) :: " + peers.iterator().next());
//    } else if (peers.size() > 1) {
//      // works better with replicated, since disconnected peers are also listed
//      boolean foundConnected = false;
//      for (Peer p : peers) {
//        if (p.getState(PeerState.class).equals(PeerState.OKAY)) {
//          if (foundConnected) {
//            throw new DiameterClientException("Diameter Stack :: Wrong number of connected peers :: " + peers);
//          }
//          foundConnected = true;
//        }
//      }
//    } else {
//      throw new DiameterClientException("Diameter Stack :: Wrong number (0) of connected peers :: " + peers);
//    }
//  }
//
//  public RoCca get() throws InterruptedException, ExecutionException {
//    //Thread.sleep(200);
//    this.countDownLatch.await();
//    log.debug("Latch released, returning result : " + result);
//    return this.result;
//  }
//
//  @Override
//  public void receivedSuccessMessage(Request request, Answer answer) {
//    log.info("inside receivedSuccessMessage");
//
//    try {
//      Utils.printMessage(log, getStack().getDictionary(), answer, false);
//    } catch (IllegalDiameterStateException e) {
//      e.printStackTrace();
//    }
//    String sessionId = "";
//
//    try {
//      sessionId = answer.getSessionId();
//      RoCcr roCcr = inFlightTxns.get(sessionId);
//      if (roCcr != null) {
//        // Build an EventAuth Response
//        RoCca roCca = getEventAuthorizationResponse(roCcr, answer);
//        log.debug("About to send: " + roCca);
//        this.result = roCca;
//        this.countDownLatch.countDown();
//      }
//    } finally {
//      inFlightTxns.remove(sessionId);
//      log.debug("No of inflight requests after removing " + sessionId + "=" + inFlightTxns.size());
//      // Close the session
//      Session clientRoSession = null;
//      try {
//        clientRoSession = getStack().getSession(sessionId, Session.class);
//      } catch (InternalException e) {
//        e.printStackTrace();
//      }
//      clientRoSession.release();
//    }
//  }
//
//  @Override
//  public void timeoutExpired(Request request) {
//    log.info("timeoutExpired");
//  }
//}
