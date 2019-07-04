package hello.example;

import org.jdiameter.api.Answer;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.Request;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.app.StateEvent;
import org.jdiameter.api.auth.events.ReAuthAnswer;
import org.jdiameter.api.ro.ClientRoSessionListener;
import org.jdiameter.api.ro.events.RoCreditControlRequest;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.client.impl.app.ro.ClientRoSessionImpl;
import org.jdiameter.client.impl.app.ro.IClientRoSessionData;
import org.jdiameter.common.api.app.ro.IClientRoSessionContext;
import org.jdiameter.common.api.app.ro.IRoMessageFactory;

public class ExampleRoClient extends ClientRoSessionImpl {

  public ExampleRoClient(IClientRoSessionData sessionData, IRoMessageFactory fct, ISessionFactory sf, ClientRoSessionListener lst,
                         IClientRoSessionContext ctx, StateChangeListener<AppSession> stLst) {
    super(sessionData, fct, sf, lst, ctx, stLst);
  }

  public static void main(String[] args) {
  }

  @Override
  public void receivedSuccessMessage(Request request, Answer answer) {

  }

  @Override
  public void timeoutExpired(Request request) {

  }

  @Override
  public Answer processRequest(Request request) {
    return null;
  }

  @Override
  public void sendCreditControlRequest(RoCreditControlRequest roCreditControlRequest) throws InternalException, IllegalDiameterStateException, RouteException
      , OverloadException {

  }

  @Override
  public void sendReAuthAnswer(ReAuthAnswer reAuthAnswer) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {

  }

  @Override
  public boolean handleEvent(StateEvent stateEvent) throws InternalException, OverloadException {
    return false;
  }

  @Override
  public <E> E getState(Class<E> aClass) {
    return null;
  }

  @Override
  public void onTimer(String s) {

  }

  @Override
  public boolean isStateless() {
    return false;
  }
}


