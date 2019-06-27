package com.kcom.diameter.client.impl;

import com.kcom.diameter.client.IDiameterRoClient;
import com.kcom.diameter.dictionary.AvpDictionary;
import com.kcom.diameter.dto.RoCCAnswer;
import com.kcom.diameter.dto.RoCCRequest;
import com.kcom.diameter.exception.DiameterClientException;
import com.kcom.diameter.helpers.Utils;
import org.apache.log4j.Logger;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.*;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.auth.events.ReAuthRequest;
import org.jdiameter.api.ro.ClientRoSession;
import org.jdiameter.api.ro.ClientRoSessionListener;
import org.jdiameter.api.ro.events.RoCreditControlAnswer;
import org.jdiameter.api.ro.events.RoCreditControlRequest;
import org.jdiameter.client.api.IContainer;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.client.impl.SessionFactoryImpl;
import org.jdiameter.client.impl.StackImpl;
import org.jdiameter.client.impl.app.ro.ClientRoSessionDataLocalImpl;
import org.jdiameter.client.impl.helpers.XMLConfiguration;
import org.jdiameter.common.api.app.ro.IClientRoSessionContext;
import org.jdiameter.common.impl.app.ro.RoCreditControlRequestImpl;
import org.jdiameter.common.impl.app.ro.RoSessionFactoryImpl;
import org.jdiameter.server.impl.helpers.Parameters;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.*;

@Component
public class DiameterRoClientFactory implements IDiameterRoClient, ClientRoSessionListener, IClientRoSessionContext, StateChangeListener<AppSession>, NetworkReqListener, EventListener<Request, Answer>, Future<RoCCAnswer> {

    public static final Logger log = Logger.getLogger(DiameterRoClientFactory.class);

    private String clientURI;
    private String serverRealm;
    private StackImpl stack;
    private RoSessionFactoryImpl roSessionFactory;
    private ISessionFactory sessionFactory;
    private ClientRoSession clientRoSession;
    private AvpDictionary avpDictionary = AvpDictionary.INSTANCE;
    private long tgppVendorId = 10415;
    private long ccDefaultAuthAppId = 4;
    private ApplicationId applicationId = ApplicationId.createByAuthAppId(tgppVendorId, ccDefaultAuthAppId);
    ClientRoSessionDataLocalImpl clientRoSessionData;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private Map<String, RoCCRequest> inFlightTxns = new HashMap<String, RoCCRequest>();
    private volatile RoCCAnswer roCCAnswerResult = null;
    private volatile boolean cancelled = false;
    protected static final Set<Long> temporaryErrorCodes;

    private static final long DIAMETER_UNABLE_TO_DELIVER = 3002L;
    private static final long DIAMETER_TOO_BUSY = 3004L;
    private static final long DIAMETER_LOOP_DETECTED = 3005L;
    protected static final int CC_REQUEST_TYPE_EVENT = 4;
    protected int ccRequestNumber = 0;

    static {
        HashSet<Long> tmp = new HashSet<Long>();
        tmp.add(DIAMETER_UNABLE_TO_DELIVER);
        tmp.add(DIAMETER_TOO_BUSY);
        tmp.add(DIAMETER_LOOP_DETECTED);
        temporaryErrorCodes = Collections.unmodifiableSet(tmp);
    }

    private static final Map<String, DiameterRoClientFactory> instances = new HashMap<String, DiameterRoClientFactory>();

    public static DiameterRoClientFactory getInstance(String clientConfigLocation, String dictionaryFile) {
        DiameterRoClientFactory instance = instances.get(clientConfigLocation);
        if (instance == null) {
            instance = new DiameterRoClientFactory(clientConfigLocation, dictionaryFile);
            instances.put(clientConfigLocation, instance);
        }
        return instance;
    }

    public static DiameterRoClientFactory getInstance(String clientConfigLocation) {
        DiameterRoClientFactory instance = instances.get(clientConfigLocation);
        if (instance == null) {
            instance = new DiameterRoClientFactory(clientConfigLocation);
            instances.put(clientConfigLocation, instance);
        }
        return instance;
    }

    private DiameterRoClientFactory(String configFile, String dictionaryFile) {
        try {
            this.initStack(configFile, dictionaryFile);
            this.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
            this.destroy();
        }
    }

    private DiameterRoClientFactory(String configFile) {
        try {
            this.initStack(configFile);
            this.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
            this.destroy();
        }
    }

    public StackImpl getStack() {
        return stack;
    }

    private void initStack(String configFile, String dictionaryFile) throws InterruptedException {
        try {
            log.debug("initStack() - START ");
            stack = new StackImpl();
            avpDictionary.parseDictionary(dictionaryFile);
            XMLConfiguration xmlConfig = new XMLConfiguration(new FileInputStream(new File(configFile)));
            stack.init(xmlConfig);
            sessionFactory = new SessionFactoryImpl((IContainer) stack);
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
            //Give time for Stack to stabilise
            Thread.sleep(500L);
            log.debug("initStack() - COMPLETE ");
        } catch (Exception e) {
            e.printStackTrace();
            this.destroy();
        }
    }

    private void initStack(String configFile) throws InterruptedException {
        try {
            log.debug("initStack() - START ");
            stack = new StackImpl();
            XMLConfiguration xmlConfig = new XMLConfiguration(new FileInputStream(new File(configFile)));
            stack.init(xmlConfig);
            sessionFactory = new SessionFactoryImpl((IContainer) stack);
            clientRoSessionData = new ClientRoSessionDataLocalImpl();
            //Give time for Stack to stabilise
            Thread.sleep(500L);
            log.debug("initStack() - COMPLETE ");
        } catch (Exception e) {
            e.printStackTrace();
            this.destroy();
        }
    }

    private void start() {
        try {
            log.debug("start() - START ");
            roSessionFactory = new RoSessionFactoryImpl(this.sessionFactory);
            roSessionFactory.setStateListener(this);
            roSessionFactory.setClientSessionListener(this);
            roSessionFactory.setClientContextListener(this);
            sessionFactory.registerAppFacory(ClientRoSession.class, roSessionFactory);
            stack.start();
            log.debug("start() - COMPLETE ");
        } catch (IllegalDiameterStateException | InternalException e) {
            e.printStackTrace();
            this.destroy();
        }

    }

    private void destroy() {
        log.debug("destroy() - START ");
        if (stack != null) {
            //Give time for Stack to stop
            try {
                stack.stop(5000L, TimeUnit.MILLISECONDS, 0);
                stack.destroy();
                log.debug("destroy() - COMPLETE ");
            } catch (IllegalDiameterStateException | InternalException e) {
                e.printStackTrace();
            }
        }

    }

    public Future<RoCCAnswer> sendEventAsSyncrhonousCall(RoCCRequest roCCRequest) {
        try {
            log.debug("sendEventAsSyncrhonousCall(RoCCRequest) - START ");
            log.debug("Received: " + roCCRequest);
            ClientRoSession clientRoSession = (ClientRoSession) roSessionFactory.getNewSession(sessionFactory.getSessionId(), ClientRoSession.class, this.getApplicationId(), null);
            log.debug("clientRoSessionId: " + clientRoSession.getSessionId());
            RoCreditControlRequest roCreditControlRequest = createCCR(CC_REQUEST_TYPE_EVENT, ccRequestNumber, clientRoSession, roCCRequest);
            ccRequestNumber++;
            Utils.printMessage(log, stack.getDictionary(), roCreditControlRequest.getMessage(), true);
            inFlightTxns.put(clientRoSession.getSessionId(), roCCRequest);
            log.debug("No of inflight requests after adding txin = " + roCCRequest.getTransactionId() + " session id = " + clientRoSession.getSessionId() + "=" + inFlightTxns.size());
            clientRoSession.sendCreditControlRequest(roCreditControlRequest);
            log.debug("sendEventAsSyncrhonousCall(RoCCRequest) - END ");
            return this;
        } catch (Exception e) {
            throw new DiameterClientException(e);
        }
    }

    @Override
    public RoCCAnswer sendEvent(RoCCRequest roCCRequest) {
        try {
            log.debug("sendEvent(RoCCRequest) - START ");
            Future<RoCCAnswer> roCCAnswerFuture = sendEventAsSyncrhonousCall(roCCRequest);
            roCCRequest.getCountDownLatch().await();
            log.debug("sendEvent(RoCCRequest) - END ");
            return roCCAnswerFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            roCCRequest.getCountDownLatch().countDown();
            return null;
        }
    }

    protected RoCreditControlRequest createCCR(int ccRequestType, int requestNumber, ClientRoSession ccaSession, RoCCRequest roCCRequest)
            throws Exception {
        log.debug("createCCR(ccRequestType,requestNumber, ClientRoSession, RoCCRequest) - START ");
        RoCreditControlRequest roCreditControlRequest = new RoCreditControlRequestImpl(ccaSession.getSessions().get(0)
                .createRequest(RoCreditControlRequest.code, getApplicationId(), getServerRealmName()));
        for (ApplicationId appid : roCreditControlRequest.getMessage().getApplicationIdAvps())
            log.debug("AUTHAPPID : " + appid.getAuthAppId() + " ACCTAPPID" + appid.getAcctAppId() + " VENDORID" + appid.getVendorId());
        AvpSet ccrAvps = roCreditControlRequest.getMessage().getAvps();
        AvpSet subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, 0);
        String subscriptionIdData = roCCRequest.getMsisdn();
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, subscriptionIdData, false);
        log.debug("createCCR(ccRequestType,requestNumber, ClientRoSession, RoCCRequest) - END ");
        return roCreditControlRequest;
    }

    @Override
    public void doCreditControlAnswer(ClientRoSession clientRoSession, RoCreditControlRequest roCreditControlRequest, RoCreditControlAnswer roCreditControlAnswer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {

        log.debug("doCreditControlAnswer(ClientRoSession, RoCreditControlRequest, RoCreditControlAnswer) - START : \nRoCreditControlRequest:\n" + roCreditControlRequest + "\nRoCreditControlAnswer:\n" + roCreditControlAnswer);

        Utils.printMessage(log, stack.getDictionary(), roCreditControlAnswer.getMessage(), false);
        String sessionId = "";

        synchronized (this) {
            try {
                sessionId = roCreditControlAnswer.getMessage().getSessionId();
                RoCCRequest roCCRequest = inFlightTxns.get(sessionId);
                if (roCCRequest != null) {
                    // Build an EventAuth Response
                    RoCCAnswer roCCAnswer = getRoCCAnswer(roCCRequest, roCreditControlAnswer);
                    log.debug("About to send: " + roCCAnswer);
                    this.roCCAnswerResult = roCCAnswer;
                    //countDownLatch = eventAuthRequest.getCountDownLatch();
                    roCCRequest.getCountDownLatch().countDown();
                    Thread.sleep(50);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // Remove the in-flight txn

                //phaser.arriveAndDeregister();
                inFlightTxns.remove(clientRoSession.getSessionId());
                log.debug("No of inflight requests after removing " + sessionId + "=" + inFlightTxns.size());
                // Close the session
                clientRoSession = fetchSession(sessionId);
                clientRoSession.release();
            }
        }
        log.debug("doCreditControlAnswer(ClientRoSession, RoCreditControlRequest, RoCreditControlAnswer) - COMPLETE");
    }

    private RoCCAnswer getRoCCAnswer(RoCCRequest roCCRequest, RoCreditControlAnswer answer) {
        log.debug("getRoCCAnswer(RoCCRequest, RoCreditControlAnswer) - START");
        RoCCAnswer roCCAnswer = new RoCCAnswer();
        roCCAnswer.setMsisdn(roCCRequest.getMsisdn());
        try {
            final long resultCode = answer.getResultCodeAvp().getUnsigned32();
            roCCAnswer.setReturnCode(String.valueOf(resultCode));
            roCCAnswer.setSuccess(resultCode == 2001);
            roCCAnswer.setTxnId(roCCRequest.getTransactionId());
            if (resultCode == 2001) {
                AvpSet avps = answer.getMessage().getAvps();
                AvpSet grantedServiceUnit = avps.getAvps(Avp.GRANTED_SERVICE_UNIT);
                log.debug("Granted-Service-Unit(431)=" + grantedServiceUnit);
                Utils.printAvps(log, stack.getDictionary(), grantedServiceUnit);
                long serviceSpecificUnits = avps.getAvp(Avp.GRANTED_SERVICE_UNIT).getGrouped().getAvp(Avp.CC_TIME).getInteger32();
                log.debug("serviceSpecificUnits=" + serviceSpecificUnits);
                String serviceSpecificUnitsString = String.valueOf(serviceSpecificUnits);
                roCCAnswer.setReservedUnits(serviceSpecificUnitsString);
            }
        } catch (AvpDataException | InternalException | IllegalDiameterStateException e) {
            throw new DiameterClientException(e);
        }
        log.debug("getRoCCAnswer(RoCCRequest, RoCreditControlAnswer) - END \n RoCCAnswer: \n" + roCCAnswer);
        return roCCAnswer;
    }

    @Override
    public void doReAuthRequest(ClientRoSession session, ReAuthRequest request) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {

    }

    @Override
    public void doOtherEvent(AppSession session, AppRequestEvent request, AppAnswerEvent answer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {

    }

    @Override
    public int getDefaultDDFHValue() {
        return 0;
    }

    @Override
    public void grantAccessOnDeliverFailure(ClientRoSession clientCCASessionImpl, Message request) {

    }

    @Override
    public void denyAccessOnDeliverFailure(ClientRoSession clientCCASessionImpl, Message request) {

    }

    @Override
    public void grantAccessOnTxExpire(ClientRoSession clientCCASessionImpl) {

    }

    @Override
    public void denyAccessOnTxExpire(ClientRoSession clientCCASessionImpl) {

    }

    @Override
    public void grantAccessOnFailureMessage(ClientRoSession clientCCASessionImpl) {

    }

    @Override
    public void denyAccessOnFailureMessage(ClientRoSession clientCCASessionImpl) {

    }

    @Override
    public void indicateServiceError(ClientRoSession clientCCASessionImpl) {

    }

    @Override
    public long getDefaultTxTimerValue() {
        return 0;
    }

    @Override
    public void txTimerExpired(ClientRoSession session) {

    }

    @Override
    public int getDefaultCCFHValue() {
        return 0;
    }

    @Override
    @Deprecated
    public void stateChanged(Enum anEnum, Enum anEnum1) {

    }

    @Override
    public void stateChanged(AppSession appSession, Enum anEnum, Enum anEnum1) {

    }

    @Override
    public void receivedSuccessMessage(Request request, Answer answer) {
        log.debug("receivedSuccessMessage(Request,Answer) - START : \nRequest:\n" + request + "\nAnswer:\n" + answer);
        log.debug("receivedSuccessMessage(Request,Answer) - COMPLETE");

    }

    @Override
    public void timeoutExpired(Request request) {
        log.debug("timeoutExpired(Request) - START: \nRequest:\n" + request);
    }

    @Override
    public Answer processRequest(Request request) {
        return null;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone()) {
            return false;
        } else {
            countDownLatch.countDown();
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
        return countDownLatch.getCount() == 0;
    }

    @Override
    public RoCCAnswer get() throws InterruptedException, ExecutionException {
        countDownLatch.await();
        return roCCAnswerResult;
    }

    @Override
    public RoCCAnswer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        countDownLatch.await(timeout, unit);
        return roCCAnswerResult;
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

    public ClientRoSession fetchSession(String sessionId) throws InternalException {
        return stack.getSession(sessionId, ClientRoSession.class);
    }

}


