package com.kcom.diameter.client.impl;

import com.kcom.diameter.client.IDiameterRoClient;
import com.kcom.diameter.dictionary.AvpDictionary;
import com.kcom.diameter.dto.RoCCAnswer;
import com.kcom.diameter.dto.RoCCRequest;
import com.kcom.diameter.exception.DiameterClientException;
import org.apache.log4j.Logger;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.Stack;
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
import org.jdiameter.common.impl.app.ro.RoSessionFactoryImpl;
import org.jdiameter.server.impl.helpers.Parameters;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DiameterRoClientFactory implements IDiameterRoClient, ClientRoSessionListener, IClientRoSessionContext, StateChangeListener<AppSession>, EventListener<Request, Answer>, NetworkReqListener {

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

    protected static final Set<Long> temporaryErrorCodes;

    private static final long DIAMETER_UNABLE_TO_DELIVER = 3002L;
    private static final long DIAMETER_TOO_BUSY = 3004L;
    private static final long DIAMETER_LOOP_DETECTED = 3005L;

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

    public Stack getStack() {
        return stack;
    }

    private void initStack(String configFile, String dictionaryFile) throws InterruptedException {
        try {
            System.out.println("initStack() - START ");
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
            System.out.println("initStack() - COMPLETE ");
        } catch (Exception e) {
            e.printStackTrace();
            this.destroy();
        }
    }

    private void initStack(String configFile) throws InterruptedException {
        try {
            System.out.println("initStack() - START ");
            stack = new StackImpl();
            XMLConfiguration xmlConfig = new XMLConfiguration(new FileInputStream(new File(configFile)));
            stack.init(xmlConfig);
            sessionFactory = new SessionFactoryImpl((IContainer) stack);
            clientRoSessionData = new ClientRoSessionDataLocalImpl();
            //Give time for Stack to stabilise
            Thread.sleep(500L);
            System.out.println("initStack() - COMPLETE ");
        } catch (Exception e) {
            e.printStackTrace();
            this.destroy();
        }
    }

    private void start() {
        try {
            System.out.println("start() - START ");
            roSessionFactory = new RoSessionFactoryImpl(this.sessionFactory);
            roSessionFactory.setStateListener(this);
            roSessionFactory.setClientSessionListener(this);
            roSessionFactory.setClientContextListener(this);
            sessionFactory.registerAppFacory(ClientRoSession.class, roSessionFactory);
            stack.start();
            System.out.println("start() - COMPLETE ");
        } catch (IllegalDiameterStateException | InternalException e) {
            e.printStackTrace();
            this.destroy();
        }

    }

    private void destroy() {
        System.out.println("destroy() - START ");
        if (stack != null) {
            //Give time for Stack to stop
            try {
                stack.stop(5000L, TimeUnit.MILLISECONDS, 0);
                stack.destroy();
                System.out.println("destroy() - COMPLETE ");
            } catch (IllegalDiameterStateException | InternalException e) {
                e.printStackTrace();
            }
        }

    }

    public RoCreditControlAnswer sendEvent(RoCreditControlRequest roCreditControlRequest) throws DiameterClientException {
        try {
            System.out.println("sendEvent(RoCreditControlRequest) - START ");
            clientRoSession = sessionFactory.getNewAppSession(this.sessionFactory.getSessionId(), applicationId, clientRoSession.getClass(), (Object[]) null);
            clientRoSession.sendCreditControlRequest(roCreditControlRequest);
            System.out.println("sendEvent(RoCreditControlRequest) - COMPLETE ");
        } catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException e) {
            e.printStackTrace();
            this.destroy();
        }
        return null;
    }


    private boolean isProvisional(long resultCode) {
        return resultCode >= 1000 && resultCode < 2000;
    }

    private boolean isSuccess(long resultCode) {
        return resultCode >= 2000 && resultCode < 3000;
    }

    private boolean isFailure(long code) {
        return (!isProvisional(code) && !isSuccess(code) && ((code >= 3000 && code < 6000)) && !temporaryErrorCodes.contains(code));
    }

    @Override
    public void doCreditControlAnswer(ClientRoSession clientRoSession, RoCreditControlRequest roCreditControlRequest, RoCreditControlAnswer roCreditControlAnswer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {

        System.out.println("doCreditControlAnswer(ClientRoSession, RoCreditControlRequest, RoCreditControlAnswer) - START : \nRoCreditControlRequest:\n" + roCreditControlRequest + "\nRoCreditControlAnswer:\n" + roCreditControlAnswer);
        System.out.println("doCreditControlAnswer(ClientRoSession, RoCreditControlRequest, RoCreditControlAnswer) - COMPLETE");
    }

    @Override
    public void doReAuthRequest(ClientRoSession clientRoSession, ReAuthRequest reAuthRequest) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {

    }

    @Override
    public void doOtherEvent(AppSession appSession, AppRequestEvent appRequestEvent, AppAnswerEvent appAnswerEvent) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {

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
        System.out.println("receivedSuccessMessage(Request,Answer) - START : \nRequest:\n" + request + "\nAnswer:\n" + answer);
        System.out.println("receivedSuccessMessage(Request,Answer) - COMPLETE");

    }

    @Override
    public void timeoutExpired(Request request) {
        System.out.println("timeoutExpired(Request) - START: \nRequest:\n" + request);
    }

    @Override
    public Answer processRequest(Request request) {
        return null;
    }

    @Override
    public RoCCAnswer sendEvent(RoCCRequest roCCRequest) {
        return null;
    }
}


