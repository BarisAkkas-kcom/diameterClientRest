package com.kcom.diameter.client;


import com.kcom.diameter.dto.RoCCAnswer;
import com.kcom.diameter.dto.RoCCRequest;
import com.kcom.diameter.exception.DiameterClientException;

/**
 * Basic class for Ro com.kcom.diameter.client credit-control applications.
 */
public interface IDiameterRoClient {

    /**
     * Sends the Ro Credit Control Request to the Server
     *
     * @param roCCRequest The Ro Credit Control Request Object
     * @return RoCCAnswer
     * @throws DiameterClientException - In case of Runtime Exceptions
     **/
    RoCCAnswer sendEvent(RoCCRequest roCCRequest) throws DiameterClientException;

}
