package com.kcom.diameter.controller;

import com.kcom.diameter.client.IDiameterRoClient;
import com.kcom.diameter.client.impl.DiameterRoClientFactory;
import com.kcom.diameter.dto.RoCCAnswer;
import com.kcom.diameter.dto.RoCCRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiameterController {

    public static final Logger logger = LoggerFactory.getLogger(DiameterController.class);

    final String clientConfig = "client-jdiameter-config.xml";


    @RequestMapping("/sendCCR")
    public ResponseEntity<RoCCAnswer> sendCCR(@RequestBody RoCCRequest roCCRequest) {

        logger.info(roCCRequest.getMsisdn());

        IDiameterRoClient diameterRoClient = DiameterRoClientFactory.getInstance(clientConfig);

        RoCCAnswer roCCAnswer = diameterRoClient.sendEvent(roCCRequest);

        logger.info("Returning msisdn : " + roCCAnswer.getMsisdn() + ", result code: " + roCCAnswer.getReturnCode() +
                ", txin : " + roCCAnswer.getTxnId() + " as a result of Request msisdn : " + roCCRequest.getMsisdn()
                + ", txin" + roCCRequest.getTransactionId());

        return new ResponseEntity<RoCCAnswer>(roCCAnswer, HttpStatus.OK);
    }
}
