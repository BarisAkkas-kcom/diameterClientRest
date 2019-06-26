package com.kcom.diameter.session;

import com.kcom.diameter.dto.RoCCAnswer;
import com.kcom.diameter.exception.DiameterClientException;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.ro.ClientRoSessionListener;
import org.jdiameter.api.ro.events.RoCreditControlRequest;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.client.impl.app.ro.ClientRoSessionImpl;
import org.jdiameter.client.impl.app.ro.IClientRoSessionData;
import org.jdiameter.common.api.app.ro.IClientRoSessionContext;
import org.jdiameter.common.api.app.ro.IRoMessageFactory;

import java.util.concurrent.*;

public class MPayRoSession extends ClientRoSessionImpl implements Future<RoCCAnswer> {

    private volatile RoCCAnswer result = null;
    private volatile boolean cancelled = false;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public MPayRoSession(IClientRoSessionData sessionData, IRoMessageFactory fct, ISessionFactory sf, ClientRoSessionListener lst, IClientRoSessionContext ctx, StateChangeListener<AppSession> stLst) {
        super(sessionData, fct, sf, lst, ctx, stLst);
    }

   // public MPayRoSession(ClientRoSessionImpl clientRoSession){
        //clientRoSession.get
   // }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    private Future<RoCCAnswer> sendEventAuth(RoCreditControlRequest eventAuthRequest ) {
        try {
             sendCreditControlRequest(eventAuthRequest);
            return this;
        }
        catch (Exception e) {
            throw new DiameterClientException(e);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone()) {
            return false;
        } else {
            countDownLatch.countDown();
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
        return countDownLatch.getCount() == 0;
        //return phaser.isTerminated();
    }

    @Override
    public RoCCAnswer get() throws InterruptedException, ExecutionException {
        countDownLatch.await();
        // phaser.arriveAndAwaitAdvance();
        return result;
    }

    @Override
    public RoCCAnswer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        countDownLatch.await(timeout, unit);
        //phaser.arriveAndAwaitAdvance();
        return result;
    }

    public RoCCAnswer getResult() {
        return result;
    }

    public void setResult(RoCCAnswer result) {
        this.result = result;
    }
}
