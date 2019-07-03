package com.kcom.diameter.event;

import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.StateEvent;
import org.jdiameter.api.ro.events.RoCreditControlAnswer;
import org.jdiameter.api.ro.events.RoCreditControlRequest;

public class ClientEvent implements StateEvent {

    public enum Type {
        SEND_EVENT_REQUEST, RECEIVE_EVENT_ANSWER;
    }

    private Type type;
    private AppRequestEvent request;
    private AppAnswerEvent answer;

    public ClientEvent(Type type) {
        this.type = type;
    }

    public ClientEvent(Type type, AppRequestEvent request, AppAnswerEvent answer) {
        this.type = type;
        this.answer = answer;
        this.request = request;
    }

    public ClientEvent(boolean isRequest, RoCreditControlRequest request, RoCreditControlAnswer answer) throws AvpDataException {
        this.answer = answer;
        this.request = request;
        if (isRequest) {
            type = Type.SEND_EVENT_REQUEST;
        } else
            type = Type.RECEIVE_EVENT_ANSWER;
    }

    @Override
    public Enum getType() {
        return type;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    @Override
    public Object getData() {
        return request != null ? request : answer;
    }

    @Override
    public void setData(Object data) {
    }

    public AppEvent getRequest() {
        return request;
    }

    public AppEvent getAnswer() {
        return answer;
    }

    @Override
    public <E> E encodeType(Class<E> eClass) {
        return eClass == ClientEvent.Type.class ? (E) type : null;
    }
}
