package hello.controller;

import hello.DTO.RoCca;
import hello.DTO.RoCcr;
import hello.client.DiameterRoClientHandler;
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
  public ResponseEntity<RoCca> sendCCR(@RequestBody RoCcr eventAuthorizationRequest) {

    logger.info(eventAuthorizationRequest.getMsisdn());

    DiameterRoClientHandler instance = new DiameterRoClientHandler();

    RoCca eventAuthorizationResponse = instance.sendEvent(eventAuthorizationRequest);

    logger.info("Returning msisdn : " + eventAuthorizationResponse.getMsisdn() + ", result code: " + eventAuthorizationResponse.getReturnCode() +
        ", txin : " + eventAuthorizationResponse.getTxnId() + " as a result of Request msisdn : " + eventAuthorizationRequest.getMsisdn()
        + ", txin" + eventAuthorizationRequest.getTransactionId());

    return new ResponseEntity<RoCca>(eventAuthorizationResponse, HttpStatus.OK);
  }
}
