package com.kcom.diameter.client.impl;

import com.kcom.diameter.client.IDiameterRoClient;
import com.kcom.diameter.dictionary.AvpDictionary;
import com.kcom.diameter.exception.DiameterClientException;
import com.kcom.diameter.helpers.Utils;
import com.kcom.diameter.ro.messages.RoCca;
import com.kcom.diameter.ro.messages.RoCcr;
import com.kcom.diameter.ro.messages.composites.ServiceSpecificUnit;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
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
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.client.impl.StackImpl;
import org.jdiameter.common.api.app.ro.IClientRoSessionContext;
import org.jdiameter.common.impl.app.ro.RoCreditControlRequestImpl;
import org.jdiameter.common.impl.app.ro.RoSessionFactoryImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

public class DiameterRoClientFactory implements IDiameterRoClient, ClientRoSessionListener, IClientRoSessionContext, StateChangeListener<AppSession>, NetworkReqListener, EventListener<Request, Answer>, Future<RoCca> {

    private static final Logger log = Logger.getLogger(DiameterRoClientFactory.class);
    private StackImpl stack;
    private RoSessionFactoryImpl roSessionFactory;
    private ISessionFactory sessionFactory;
    private ClientRoSession clientRoSession;
    private AvpDictionary avpDictionary = AvpDictionary.INSTANCE;
    private long tgppVendorId = 10415;
    private long ccDefaultAuthAppId = 4;
    private ApplicationId applicationId = ApplicationId.createByAuthAppId(tgppVendorId, ccDefaultAuthAppId);

    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private Map<String, RoCcr> inFlightTxns = new HashMap<String, RoCcr>();
    private volatile RoCca roCcaResult = null;
    private volatile boolean cancelled = false;

    private static final int CC_REQUEST_TYPE_EVENT = 4;
    private int ccRequestNumber = 0;

    private static final Map<String, DiameterRoClientFactory> instances = new HashMap<String, DiameterRoClientFactory>();
    private static final String configFile = "/client-config.xml";
    private static final String dictionaryFile = "/dictionary.xml";

    private void configLog4j() {
        InputStream inStreamLog4j = DiameterRoClientFactory.class.getClassLoader().getResourceAsStream("log4j.properties");
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

    public static DiameterRoClientFactory getInstance(String config, String dictionary) {
        DiameterRoClientFactory diameterRoClientFactory = null;
        if (instances.get(config) == null) {
            diameterRoClientFactory = new DiameterRoClientFactory(config, dictionary);
            instances.put(config, diameterRoClientFactory);
        }
        return diameterRoClientFactory;
    }

    private DiameterRoClientFactory(String config, String dictionary) {
        try {
            this.initStack(config, dictionary);
            this.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
            this.destroy();
        }
    }

    private DiameterRoClientFactory() {
        try {
            this.initStack(configFile, dictionaryFile);
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
            log.info("AVP Dictionary successfully parsed.");
            sessionFactory = (ISessionFactory) stack.init(new XMLConfiguration(new FileInputStream(new File(configFile))));
            sessionFactory.registerAppFacory(ClientRoSession.class, new RoSessionFactoryImpl(sessionFactory));
            log.info("Client Config successfully parsed and Session Factory created!");
            Configuration config = stack.getConfiguration();
            log.debug("DIAMETER CONFIG :: \n" + config);
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
            sessionFactory = (ISessionFactory) this.stack.getSessionFactory();
            roSessionFactory = new RoSessionFactoryImpl(sessionFactory);
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

    private Future<RoCca> sendEventAsFuture(RoCcr roCcr) {
        try {
            log.debug("sendEventAsFuture(RoCcr) - START ");
            log.debug("Received: " + roCcr);
            ClientRoSession clientRoSession = (ClientRoSession) roSessionFactory.getNewSession(sessionFactory.getSessionId(), ClientRoSession.class, this.getApplicationId(), null);
            log.debug("clientRoSessionId: " + clientRoSession.getSessionId());
            RoCreditControlRequest roCreditControlRequest = createCCR(ccRequestNumber, clientRoSession, roCcr);
            ccRequestNumber++;
            Utils.printMessage(log, stack.getDictionary(), roCreditControlRequest.getMessage(), true);
            inFlightTxns.put(clientRoSession.getSessionId(), roCcr);
            log.debug("No of inflight requests after adding txin = " + roCcr.getSubscriptionId() + " session id = " + clientRoSession.getSessionId() + "=" + inFlightTxns.size());
            clientRoSession.sendCreditControlRequest(roCreditControlRequest);
            log.debug("sendEventAsFuture(RoCcr) - END ");
            return this;
        } catch (Exception e) {
            throw new DiameterClientException(e);
        }
    }

    @Override
    public RoCca sendEvent(RoCcr roCcr) {
        try {
            log.debug("sendEvent(RoCcr) - START ");
            Future<RoCca> roCCAnswerFuture = sendEventAsFuture(roCcr);
            countDownLatch.await();
            log.debug("sendEvent(RoCcr) - END ");
            return roCCAnswerFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            countDownLatch.countDown();
            return null;
        }
    }

    private RoCreditControlRequest createCCR(int requestNumber, ClientRoSession clientRoSession, RoCcr roCcr)
        throws Exception {
        log.debug("createCCR(ccRequestType,requestNumber, ClientRoSession, RoCcr) - START ");
        RoCreditControlRequest roCreditControlRequest = new RoCreditControlRequestImpl(clientRoSession, "server.kcom.com", "127.0.0.1");
        for (ApplicationId appid : roCreditControlRequest.getMessage().getApplicationIdAvps())
            log.debug("AUTHAPPID : " + appid.getAuthAppId() + " ACCTAPPID" + appid.getAcctAppId() + " VENDORID" + appid.getVendorId());
        AvpSet ccrAvps = roCreditControlRequest.getMessage().getAvps();
        AvpSet subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, 0);
        ccrAvps.addAvp(Avp.CC_REQUEST_TYPE, DiameterRoClientFactory.CC_REQUEST_TYPE_EVENT);
        ccrAvps.addAvp(Avp.CC_REQUEST_NUMBER, requestNumber);
        String subscriptionIdData = roCcr.getSubscriptionId().getSubscriptionIdData();
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, subscriptionIdData, false);

        //DO MORE TO MAP RoCCr Object values to RoCreditControlRequest Object

        log.debug("createCCR(ccRequestType,requestNumber, ClientRoSession, RoCcr) - END ");
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
                RoCcr roCcr = inFlightTxns.get(sessionId);
                if (roCcr != null) {
                    // Build an EventAuth Response
                    RoCca roCca = getRoCCAnswer(roCcr, roCreditControlAnswer);
                    log.debug("About to send: " + roCca);
                    this.roCcaResult = roCca;
                    countDownLatch.countDown();
                    Thread.sleep(50);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                inFlightTxns.remove(clientRoSession.getSessionId());
                log.debug("No of inflight requests after removing " + sessionId + "=" + inFlightTxns.size());
                // Close the session
                clientRoSession = fetchSession(sessionId);
                clientRoSession.release();
            }
        }
        log.debug("doCreditControlAnswer(ClientRoSession, RoCreditControlRequest, RoCreditControlAnswer) - COMPLETE");
    }

    private RoCca getRoCCAnswer(RoCcr roCcr, RoCreditControlAnswer answer) {
        log.debug("getRoCCAnswer(RoCcr, RoCreditControlAnswer) - START");
        RoCca roCca = new RoCca();

        //DO MORE TO MAP RoCca Object values to RoCreditControlAnswer Object

        try {
            int resultCode = answer.getResultCodeAvp().getInteger32();
            roCca.setResultCode(resultCode);
            roCca.setCcRequestNumber(roCcr.getCcRequestNumber());
            roCca.setCcRequestType(roCcr.getCcRequestType());
            roCca.setSessionId(roCcr.getSubscriptionId().getSubscriptionIdData());
            //SET Diameter Success and set transaction Id
            if (resultCode == 2001) {
                AvpSet avps = answer.getMessage().getAvps();
                AvpSet grantedServiceUnit = avps.getAvps(Avp.GRANTED_SERVICE_UNIT);
                log.debug("Granted-Service-Unit(431)=" + grantedServiceUnit);
                Utils.printAvps(log, stack.getDictionary(), grantedServiceUnit);
                int grantedServiceUnits = avps.getAvp(Avp.GRANTED_SERVICE_UNIT).getGrouped().getAvp(Avp.CC_TIME).getInteger32();
                log.debug("grantedServiceUnits=" + grantedServiceUnits);
                ServiceSpecificUnit serviceSpecificUnits = new ServiceSpecificUnit();
                serviceSpecificUnits.setCcServiceSpecificUnits(grantedServiceUnits);
                roCca.setGrantedServiceUnit(serviceSpecificUnits);
            }
        } catch (AvpDataException | InternalException | IllegalDiameterStateException e) {
            throw new DiameterClientException(e);
        }
        log.debug("getRoCCAnswer(RoCcr, RoCreditControlAnswer) - END \n RoCca: \n" + roCca);
        return roCca;
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
    public RoCca get() throws InterruptedException, ExecutionException {
        countDownLatch.await();
        return roCcaResult;
    }

    @Override
    public RoCca get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        countDownLatch.await(timeout, unit);
        return roCcaResult;
    }

    private ApplicationId getApplicationId() {
        return applicationId;
    }

    private ClientRoSession fetchSession(String sessionId) throws InternalException {
        return stack.getSession(sessionId, ClientRoSession.class);
    }

}


