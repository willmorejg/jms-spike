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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ljcomputing.jmsspike.model.MyMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;

@SpringBootTest
class JmsSpikeApplicationTests {
    @Autowired private JmsTemplate jmsTemplate;
    @Autowired private ObjectMapper jsonMapper;

    @Test
    void sendMessage() throws JmsException, JsonProcessingException, InterruptedException {
        MyMessage msg = new MyMessage();
        msg.setMessage("hello");
        String queue = "sample";
        jmsTemplate.convertAndSend(queue, jsonMapper.writeValueAsString(msg));
        Thread.sleep(12000L);
        Object obj = jmsTemplate.receiveAndConvert(queue);
        System.out.println("obj: " + jsonMapper.readValue(obj.toString(), MyMessage.class));
    }
}
