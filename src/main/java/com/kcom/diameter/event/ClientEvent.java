 /*
  * TeleStax, Open Source Cloud Communications
  * Copyright 2011-2016, TeleStax Inc. and individual contributors
  * by the @authors tag.
  *
  * This program is free software: you can redistribute it and/or modify
  * under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation; either version 3 of
  * the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>
  *
  * This file incorporates work covered by the following copyright and
  * permission notice:
  *
  *   JBoss, Home of Professional Open Source
  *   Copyright 2007-2011, Red Hat, Inc. and individual contributors
  *   by the @authors tag. See the copyright.txt in the distribution for a
  *   full listing of individual contributors.
  *
  *   This is free software; you can redistribute it and/or modify it
  *   under the terms of the GNU Lesser General Public License as
  *   published by the Free Software Foundation; either version 2.1 of
  *   the License, or (at your option) any later version.
  *
  *   This software is distributed in the hope that it will be useful,
  *   but WITHOUT ANY WARRANTY; without even the implied warranty of
  *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  *   Lesser General Public License for more details.
  *
  *   You should have received a copy of the GNU Lesser General Public
  *   License along with this software; if not, write to the Free
  *   Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  *   02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */

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

     Type type;
     AppRequestEvent request;
     AppAnswerEvent answer;

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
