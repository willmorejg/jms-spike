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

import com.fasterxml.jackson.core.JsonProcessingException;
import net.ljcomputing.jmsspike.model.MyMessage;
import net.ljcomputing.jmsspike.service.JsonService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;

@SpringBootTest
class JmsSpikeApplicationTests {
    private static final Logger log = LoggerFactory.getLogger(JmsSpikeApplicationTests.class);

    @Autowired private JmsTemplate jmsTemplate;
    @Autowired private JsonService<MyMessage> myMessagJsonService;

    @Test
    void sendMessage() throws JmsException, JsonProcessingException, InterruptedException {
        String queue = "sample";
        MyMessage msg = new MyMessage();

        msg.setMessage("hello");

        String jsonString = myMessagJsonService.toJson(msg);

        log.debug("msg - toString: {}", msg);
        log.debug("jsonString: {}", jsonString);

        jmsTemplate.convertAndSend(queue, jsonString);

        Thread.sleep(3000L);

        Object obj = jmsTemplate.receiveAndConvert(queue);

        log.debug("obj - toString: {}", obj);

        assertNotNull(obj, "obj is null");

        String responseString = obj.toString();
        MyMessage responseMsg = myMessagJsonService.fromJson(responseString);

        log.debug("responseString: {}", responseString);
        log.debug("responseMsg: {}", responseMsg);

        assertEquals(msg, responseMsg, "msg != responseMsg");
    }
}
