/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.

James G Willmore - LJ Computing - (C) 2023
*/
package net.ljcomputing.jmsspike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.ljcomputing.insurancexml.domain.Address;
import net.ljcomputing.insurancexml.domain.AddressType;
import net.ljcomputing.insurancexml.domain.Addresses;
import net.ljcomputing.insurancexml.domain.Driver;
import net.ljcomputing.insurancexml.domain.Drivers;
import net.ljcomputing.insurancexml.domain.Insured;
import net.ljcomputing.insurancexml.domain.Policy;
import net.ljcomputing.insurancexml.domain.Risk;
import net.ljcomputing.insurancexml.domain.RiskType;
import net.ljcomputing.insurancexml.domain.Risks;
import net.ljcomputing.insurancexml.domain.USState;
import net.ljcomputing.insurancexml.domain.Vehicle;
import net.ljcomputing.jmsspike.model.MyMessage;
import net.ljcomputing.jmsspike.service.JsonService;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class JmsSpikeApplicationTests {
    private static final Logger log = LoggerFactory.getLogger(JmsSpikeApplicationTests.class);

    @Autowired private JmsTemplate jmsTemplate;
    @Autowired private JsonService<MyMessage> myMessageJsonService;
    @Autowired private Jaxb2Marshaller policyMarshaller;

    private Policy getPolicy() {
        Address physicalAddr = new Address();
        physicalAddr.setStreet1("1 Salem Road");
        physicalAddr.setCity("Smithville");
        physicalAddr.setState(USState.NJ);
        physicalAddr.setZipCode("09190");

        Address billingAddr = new Address();
        billingAddr.setType(AddressType.BILLING);
        billingAddr.setStreet1("1 Salem Road");
        billingAddr.setCity("Smithville");
        billingAddr.setState(USState.NJ);
        billingAddr.setZipCode("09190");

        Addresses addresses = new Addresses();
        addresses.getAddresses().add(physicalAddr);
        addresses.getAddresses().add(billingAddr);

        Risk dpRisk = new Risk();
        dpRisk.setName("DP Risk");
        dpRisk.setRiskType(RiskType.DWELLING_PROPERTY);
        dpRisk.setAddress(physicalAddr);

        Vehicle vehicle = new Vehicle();
        vehicle.setMake("Subaru");
        vehicle.setModel("Outback");
        vehicle.setYear(2000);
        vehicle.setVin("5GAKRDED0CJ396612");

        Driver driver = new Driver();
        driver.setGivenName("James");
        driver.setSurname("Willmore");
        driver.setBirthdate(LocalDate.of(1968, 1, 22));
        driver.setDlNumber("DL123 45678 90000");
        driver.setDlState(USState.NJ);

        Drivers drivers = new Drivers();
        drivers.getDrivers().add(driver);

        Risk paRisk = new Risk();
        paRisk.setName("PA Risk");
        paRisk.setRiskType(RiskType.PERSONAL_AUTO);
        paRisk.setDrivers(drivers);
        paRisk.setVehicle(vehicle);

        Risks risks = new Risks();
        risks.getRisks().add(paRisk);
        risks.getRisks().add(dpRisk);

        Insured insured = new Insured();
        insured.setGivenName("James");
        insured.setSurname("Willmore");
        insured.setBirthdate(LocalDate.of(1968, 1, 22));

        insured.setAddresses(addresses);

        Policy policy = new Policy();
        policy.setInsured(insured);
        policy.setRisks(risks);

        return policy;
    }

    @Test
    @Order(1)
    void messageTest() throws JmsException, JsonProcessingException, InterruptedException {
        String queue = "sample";
        MyMessage msg = new MyMessage();

        msg.setMessage("hello");

        String jsonString = myMessageJsonService.toJson(msg);

        log.debug("msg - toString: {}", msg);
        log.debug("jsonString: {}", jsonString);

        jmsTemplate.convertAndSend(queue, jsonString);

        Thread.sleep(3000L);

        Object obj = jmsTemplate.receiveAndConvert(queue);

        log.debug("obj - toString: {}", obj);

        assertNotNull(obj, "obj is null");

        String responseString = obj.toString();
        MyMessage responseMsg = myMessageJsonService.fromJson(responseString);

        log.debug("responseString: {}", responseString);
        log.debug("responseMsg: {}", responseMsg);

        assertEquals(msg, responseMsg, "msg != responseMsg");
    }

    @Test
    @Order(10)
    void insuranceXmlMessageTest() {
        boolean result = false;

        try {
            String queue = "sample";
            Policy msg = getPolicy();

            StringWriter jsonString = new StringWriter();
            policyMarshaller.marshal(msg, new StreamResult(jsonString));

            log.debug("msg - toString: {}", msg);
            log.debug("jsonString: {}", jsonString);

            jmsTemplate.convertAndSend(queue, jsonString.toString());

            Thread.sleep(3000L);

            Object obj = jmsTemplate.receiveAndConvert(queue);

            log.debug("obj - toString: {}", obj);

            assertNotNull(obj, "obj is null");

            String responseString = obj.toString();
            log.debug("responseString: {}", responseString);

            Policy responseMsg =
                    (Policy)
                            policyMarshaller.unmarshal(
                                    new StreamSource(new StringReader(responseString)));
            log.debug("responseMsg: {}", responseMsg);

            assertEquals(msg, responseMsg, "msg != responseMsg");
            result = true;
        } catch (Exception e) {
            log.error("ERROR: ", e);
        }

        assertTrue(result);
    }
}
