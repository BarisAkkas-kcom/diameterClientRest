package com.kcom.diameter.client;

import com.kcom.diameter.exception.DiameterClientException;
import com.kcom.diameter.ro.messages.RoCca;
import com.kcom.diameter.ro.messages.RoCcr;

/**
 * Basic class for Ro com.kcom.diameter.client credit-control applications.
 */
public interface IDiameterRoClient {

    /**
     * Sends the Ro Credit Control Request to the Server
     *
     * @param roCcr The Ro Credit Control Request Object
     * @return RoCca
     * @throws DiameterClientException - In case of Runtime Exceptions
     **/
    RoCca sendEvent(RoCcr roCcr) throws DiameterClientException;

}
