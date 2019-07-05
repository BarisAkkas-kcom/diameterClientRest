package com.kcom.diameter.functional;

import com.kcom.diameter.client.impl.DiameterRoClientFactory;
import com.kcom.diameter.ro.messages.RoCca;
import com.kcom.diameter.ro.messages.RoCcr;
import com.kcom.diameter.ro.messages.composites.ServiceSpecificUnit;
import com.kcom.diameter.ro.messages.composites.SubscriptionId;

import java.util.UUID;

public class RoClientFunctionalTest {

    public static void main(String[] args) {
        DiameterRoClientFactory instance = DiameterRoClientFactory.getInstance("C:\\Users\\akkasb\\Desktop\\baris-workspace\\jDiameter-Implementations" +
            "\\DiameterClientRest\\src\\main\\resources\\client-config.xml","C:\\Users\\akkasb\\Desktop\\baris-workspace\\jDiameter" +
            "-Implementations\\DiameterClientRest\\src\\main\\resources\\dictionary.xml");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        RoCcr roCcr = createCCr();
        RoCca roCca = instance.sendEvent(roCcr);
        System.out.println(roCca.toString());
    }

    private static RoCcr createCCr(){
        RoCcr roCcr = new RoCcr();
        SubscriptionId subsc = new SubscriptionId();
        subsc.setSubscriptionIdData("447700700777");
        roCcr.setSubscriptionId(subsc);
        ServiceSpecificUnit ssu = new ServiceSpecificUnit();
        ssu.setCcServiceSpecificUnits(1);
        roCcr.setRequestedServiceUnit(ssu);
        String serviceContextId = UUID.randomUUID().toString().replaceAll("-", "") + "@mss.mobicents.org";
        roCcr.setServiceContextId(serviceContextId);
        return roCcr;
    }
}
