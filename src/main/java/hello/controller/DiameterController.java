package hello.controller;

import hello.DTO.EventAuthorizationRequest;
import hello.DTO.EventAuthorizationResponse;
import hello.client.MPayOcsClient;
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
  public ResponseEntity<EventAuthorizationResponse> sendCCR(@RequestBody EventAuthorizationRequest eventAuthorizationRequest) {

    logger.info(eventAuthorizationRequest.getMsisdn());

    MPayOcsClient instance = MPayOcsClient.getInstance(clientConfig);

    EventAuthorizationResponse eventAuthorizationResponse = instance.sendEventAuthAndBlock(eventAuthorizationRequest);

    logger.info("Returning msisdn : " + eventAuthorizationResponse.getMsisdn() + ", result code: " + eventAuthorizationResponse.getReturnCode() +
        ", txin : " + eventAuthorizationResponse.getTxnId() + " as a result of Request msisdn : " + eventAuthorizationRequest.getMsisdn()
        + ", txin" + eventAuthorizationRequest.getTransactionId());

    return new ResponseEntity<EventAuthorizationResponse>(eventAuthorizationResponse, HttpStatus.OK);
  }
}
