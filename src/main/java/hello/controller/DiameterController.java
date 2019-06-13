package hello.controller;

import hello.DTO.EventAuthorizationRequest;
import hello.DTO.EventAuthorizationResponse;
import hello.client.MPayOcsClient;
import hello.example.ExampleChargingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@ComponentScan
@RestController
public class DiameterController {

    public static final Logger logger = LoggerFactory.getLogger(DiameterController.class);

    final String clientConfig = "client-jdiameter-config.xml";


    @RequestMapping("/sendCCR")
    public ResponseEntity<EventAuthorizationResponse> sendCCR(@RequestBody EventAuthorizationRequest eventAuthorizationRequest) {

        logger.info(eventAuthorizationRequest.getMsisdn());

//        try {
//            ExampleChargingClient eac = new ExampleChargingClient(clientConfig);
//            eac.setUp();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        MPayOcsClient instance = MPayOcsClient.getInstance(clientConfig);

       // EventAuthorizationRequest eventAuthRequest = new EventAuthorizationRequest();
//      eventAuthRequest.setMsisdn("447700700700");
//      eventAuthRequest.setTransactionId(UUID.randomUUID().toString());
//      toTest.sendCcrEvent(eventAuthRequest);

//        eventAuthRequest = new EventAuthorizationRequest();
//        eventAuthRequest.setMsisdn("447700700777");
//        eventAuthRequest.setTransactionId(UUID.randomUUID().toString());
        EventAuthorizationResponse eventAuthorizationResponse = instance.sendEventAuthAndBlock(eventAuthorizationRequest);
        logger.debug("AFTER sendEventAuthAndBlock EXECUTED");

        //EventAuthorizationResponse eventAuthorizationResponse = instance.sendEventAuthAndBlock(eventAuthorizationRequest);
        //EventAuthorizationResponse eventAuthorizationResponse = new EventAuthorizationResponse();


        return new ResponseEntity<EventAuthorizationResponse>(eventAuthorizationResponse, HttpStatus.OK);
    }
}
