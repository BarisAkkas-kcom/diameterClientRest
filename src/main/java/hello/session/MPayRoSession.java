package hello.session;

import hello.DTO.EventAuthorizationResponse;
import hello.client.MPayDiameterClientException;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.ro.ClientRoSessionListener;
import org.jdiameter.api.ro.events.RoCreditControlRequest;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.client.impl.app.ro.ClientRoSessionImpl;
import org.jdiameter.client.impl.app.ro.IClientRoSessionData;
import org.jdiameter.common.api.app.ro.IClientRoSessionContext;
import org.jdiameter.common.api.app.ro.IRoMessageFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MPayRoSession extends ClientRoSessionImpl implements Future<EventAuthorizationResponse> {

  private final CountDownLatch countDownLatch = new CountDownLatch(1);
  private volatile EventAuthorizationResponse result = null;
  private volatile boolean cancelled = false;

  public MPayRoSession(IClientRoSessionData sessionData, IRoMessageFactory fct, ISessionFactory sf, ClientRoSessionListener lst, IClientRoSessionContext ctx,
                       StateChangeListener<AppSession> stLst) {
    super(sessionData, fct, sf, lst, ctx, stLst);
  }

  // public MPayRoSession(ClientRoSessionImpl clientRoSession){
  //clientRoSession.get
  // }

  public CountDownLatch getCountDownLatch() {
    return countDownLatch;
  }

  private Future<EventAuthorizationResponse> sendEventAuth(RoCreditControlRequest eventAuthRequest) {
    try {
      sendCreditControlRequest(eventAuthRequest);
      return this;
    } catch (Exception e) {
      throw new MPayDiameterClientException(e);
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
  public EventAuthorizationResponse get() throws InterruptedException, ExecutionException {
    countDownLatch.await();
    // phaser.arriveAndAwaitAdvance();
    return result;
  }

  @Override
  public EventAuthorizationResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    countDownLatch.await(timeout, unit);
    //phaser.arriveAndAwaitAdvance();
    return result;
  }

  public EventAuthorizationResponse getResult() {
    return result;
  }

  public void setResult(EventAuthorizationResponse result) {
    this.result = result;
  }
}
