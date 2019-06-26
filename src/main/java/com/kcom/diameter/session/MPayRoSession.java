package com.kcom.diameter.session;

import com.kcom.diameter.dto.RoCCAnswer;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.ro.ClientRoSessionListener;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.client.impl.app.ro.ClientRoSessionImpl;
import org.jdiameter.client.impl.app.ro.IClientRoSessionData;
import org.jdiameter.common.api.app.ro.IClientRoSessionContext;
import org.jdiameter.common.api.app.ro.IRoMessageFactory;

import java.util.concurrent.*;

public class MPayRoSession extends ClientRoSessionImpl implements Future<RoCCAnswer> {

    private volatile RoCCAnswer roCCAnswer = null;
    private volatile boolean cancelled = false;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public MPayRoSession(IClientRoSessionData sessionData, IRoMessageFactory fct, ISessionFactory sf, ClientRoSessionListener lst, IClientRoSessionContext ctx, StateChangeListener<AppSession> stLst) {
        super(sessionData, fct, sf, lst, ctx, stLst);
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
        return roCCAnswer;
    }

    @Override
    public RoCCAnswer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        countDownLatch.await(timeout, unit);
        return roCCAnswer;
    }

    public RoCCAnswer getRoCCAnswer() {
        return roCCAnswer;
    }

    public void setRoCCAnswer(RoCCAnswer roCCAnswer) {
        this.roCCAnswer = roCCAnswer;
    }
}
