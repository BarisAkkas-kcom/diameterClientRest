package com.kcom.diameter.client;

import com.kcom.diameter.dictionary.AvpDictionary;
import com.kcom.diameter.dto.RoCCAnswer;
import com.kcom.diameter.dto.RoCCRequest;
import com.kcom.diameter.exception.DiameterClientException;
import com.kcom.diameter.functional.TBase;
import com.kcom.diameter.functional.Utils;
import com.kcom.diameter.session.MPayRoSession;
import org.apache.log4j.PropertyConfigurator;
import org.jdiameter.api.Stack;
import org.jdiameter.api.*;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.auth.events.ReAuthRequest;
import org.jdiameter.api.ro.ClientRoSession;
import org.jdiameter.api.ro.ClientRoSessionListener;
import org.jdiameter.api.ro.events.RoCreditControlAnswer;
import org.jdiameter.api.ro.events.RoCreditControlRequest;
import org.jdiameter.common.api.app.ro.IClientRoSessionContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class OCSClient extends TBase implements ClientRoSessionListener, IClientRoSessionContext, Future<RoCCAnswer>, IDiameterRoClient {

    private volatile RoCCAnswer roCCAnswerResult = null;
    private volatile boolean cancelled = false;
    final String clientConfig = "client-jdiameter-config.xml";
    private static final String dictionaryFile = "dictionary.xml";
    private static final Map<String, OCSClient> instances = new HashMap<String, OCSClient>();

    private AvpDictionary dictionary = AvpDictionary.INSTANCE;
    private Map<String, RoCCRequest> inFlightTxns = new HashMap<String, RoCCRequest>();

    @PostConstruct
    private void init() {
        log.info("Initialization started ...");
        configLog4j();
        OCSClient instance = new OCSClient(clientConfig);
        instances.put(clientConfig, instance);
        log.info("Initialization finished ...");
    }

    private void configLog4j() {
        InputStream inStreamLog4j = OCSClient.class.getClassLoader().getResourceAsStream("log4j.properties");
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

    public static OCSClient getInstance(String clientConfigLocation) {
        OCSClient instance = instances.get(clientConfigLocation);
        if (instance == null) {
            instance = new OCSClient(clientConfigLocation);
            instances.put(clientConfigLocation, instance);
        }
        return instance;
    }

    // Private force singletons to be created with getInstance(String clientConfigLocation)
    private OCSClient() {
        super();
    }

    private OCSClient(String clientConfigLocation) {
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
        } catch (Exception e) {
            throw new DiameterClientException("An error occurred while parsing the Diameter Configuration", e);
        }
    }

    public Future<RoCCAnswer> sendEventAsSyncrhonousCall(RoCCRequest roCCRequest) {
        try {
            log.debug("Received: " + roCCRequest);
            ClientRoSession clientRoSession = this.getNewSession();
            log.debug("clientRoSessionId: " + clientRoSession.getSessionId());
            RoCreditControlRequest roCreditControlRequest = createCCR(CC_REQUEST_TYPE_EVENT, ccRequestNumber, clientRoSession, roCCRequest);
            ccRequestNumber++;
            Utils.printMessage(log, stack.getDictionary(), roCreditControlRequest.getMessage(), true);
            inFlightTxns.put(clientRoSession.getSessionId(), roCCRequest);
            log.debug("No of inflight requests after adding txin = " + roCCRequest.getTransactionId() + " session id = " + clientRoSession.getSessionId() + "=" + inFlightTxns.size());
            clientRoSession.sendCreditControlRequest(roCreditControlRequest);
            return this;
        } catch (Exception e) {
            throw new DiameterClientException(e);
        }
    }

    public RoCCAnswer sendEvent(RoCCRequest roCCRequest) {
        try {
            Future<RoCCAnswer> roCCAnswerFuture = sendEventAsSyncrhonousCall(roCCRequest);
            roCCRequest.getCountDownLatch().await();
            //return sendEventAuth(roCCRequest).get(200,TimeUnit.MILLISECONDS);
            return roCCAnswerFuture.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            roCCRequest.getCountDownLatch().countDown();
            //phaser.arriveAndDeregister();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            roCCRequest.getCountDownLatch().countDown();
            //phaser.arriveAndDeregister();
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.jdiameter.api.cca.ClientCCASessionListener#doCreditControlAnswer( org.jdiameter.api.cca.ClientCCASession,
     * org.jdiameter.api.cca.events.RoCreditControlRequest, org.jdiameter.api.cca.events.JCreditControlAnswer)
     */
    @Override
    public void doCreditControlAnswer(ClientRoSession session, RoCreditControlRequest roCreditControlRequest, RoCreditControlAnswer roCreditControlAnswer)
            throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {

        log.debug("inside MPAYOCSClient doCreditControlAnswer ");

        Utils.printMessage(log, super.stack.getDictionary(), roCreditControlAnswer.getMessage(), false);
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
                    roCCRequest.getCountDownLatch().countDown();
                    Thread.sleep(50);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // Remove the in-flight txn
                inFlightTxns.remove(session.getSessionId());
                log.debug("No of inflight requests after removing " + sessionId + "=" + inFlightTxns.size());
                // Close the session
                ClientRoSession clientRoSession = fetchSession(sessionId);
                clientRoSession.release();
            }
        }
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

    public ClientRoSession fetchSession(String sessionId) throws InternalException {
        return stack.getSession(sessionId, MPayRoSession.class);
    }

    public ClientRoSession getNewSession() {
        try {
            return sessionFactory.getNewAppSession(sessionFactory.getSessionId(), getApplicationId(), MPayRoSession.class, (Object) null);
        } catch (InternalException e) {
            throw new DiameterClientException(e);
        }
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
        } catch (InternalException e) {
            throw new DiameterClientException(e);
        }
        if (peers.size() == 1) {
            // ok
            log.debug("Diameter Stack :: Peers OK (1) :: " + peers.iterator().next());
        } else if (peers.size() > 1) {
            // works better with replicated, since disconnected peers are also listed
            boolean foundConnected = false;
            for (Peer p : peers) {
                if (p.getState(PeerState.class).equals(PeerState.OKAY)) {
                    if (foundConnected) {
                        throw new DiameterClientException("Diameter Stack :: Wrong number of connected peers :: " + peers);
                    }
                    foundConnected = true;
                }
            }
        } else {
            throw new DiameterClientException("Diameter Stack :: Wrong number (0) of connected peers :: " + peers);
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
    public RoCCAnswer get() throws InterruptedException, ExecutionException {
        //Thread.sleep(200);
        //countDownLatch.await();
        // phaser.arriveAndAwaitAdvance();
        return roCCAnswerResult;
    }

    @Override
    public RoCCAnswer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        //countDownLatch.await(timeout, unit);
        //phaser.arriveAndAwaitAdvance();
        return roCCAnswerResult;
    }
}
