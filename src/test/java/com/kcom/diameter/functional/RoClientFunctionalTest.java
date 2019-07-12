package com.kcom.diameter.functional;

import com.kcom.diameter.client.impl.DiameterRoClientHandler;
import com.kcom.diameter.ro.messages.RoCca;
import com.kcom.diameter.ro.messages.RoCcr;
import com.kcom.diameter.ro.messages.composites.ServiceSpecificUnit;
import com.kcom.diameter.ro.messages.composites.SubscriptionId;

import java.util.UUID;

public class RoClientFunctionalTest {

    public static void main(String[] args) {

        DiameterRoClientHandler instance = new DiameterRoClientHandler();

        RoCcr roCcr = createCCr();
        RoCca roCca = instance.sendEvent(roCcr);
        System.out.println("Result is : " + roCca.toString());
    }

    private static RoCcr createCCr(){
        RoCcr roCcr = new RoCcr();

        SubscriptionId subsc = new SubscriptionId();
        subsc.setSubscriptionIdData("447700700777");
        subsc.setSubscriptionIdType(0);
        roCcr.setSubscriptionId(subsc);

        ServiceSpecificUnit ssu = new ServiceSpecificUnit();
        ssu.setCcServiceSpecificUnits(1);
        roCcr.setRequestedServiceUnit(ssu);

        String serviceContextId = UUID.randomUUID().toString().replaceAll("-", "") + "@kcom.com";
        roCcr.setServiceContextId(serviceContextId);

        roCcr.setCcRequestType(4);
        return roCcr;
    }
}
