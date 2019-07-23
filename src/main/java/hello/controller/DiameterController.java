package hello.controller;


import com.kcom.diameter.client.impl.DiameterRoClientHandler;
import com.kcom.diameter.ro.messages.RoCca;
import com.kcom.diameter.ro.messages.RoCcr;
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

    logger.info(eventAuthorizationRequest.getSubscriptionId().getSubscriptionIdData());

    DiameterRoClientHandler instance = new DiameterRoClientHandler();

    RoCca eventAuthorizationResponse = instance.sendEvent(eventAuthorizationRequest);

//    logger.info("Returning msisdn : " + eventAuthorizationResponse.get + ", result code: " + eventAuthorizationResponse.getResultCode() +
//       " as a result of Request msisdn : " + eventAuthorizationRequest.getMsisdn()
//        + ", txin" + eventAuthorizationRequest.getTransactionId());

    return new ResponseEntity<RoCca>(eventAuthorizationResponse, HttpStatus.OK);
  }
}
