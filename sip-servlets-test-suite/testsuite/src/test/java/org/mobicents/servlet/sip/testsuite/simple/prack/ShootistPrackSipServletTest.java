/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.sip.testsuite.simple.prack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sip.SipProvider;
import static junit.framework.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.NetworkPortAssigner;
import org.mobicents.servlet.sip.SipServletTestCase;
import org.mobicents.servlet.sip.testsuite.ProtocolObjects;
import org.mobicents.servlet.sip.testsuite.TestSipListener;

public class ShootistPrackSipServletTest extends SipServletTestCase {

    private static transient Logger logger = Logger.getLogger(ShootistPrackSipServletTest.class);
    private static final String TRANSPORT = "udp";
    private static final boolean AUTODIALOG = true;
    private static final int TIMEOUT = 10000;
    private static final int DIALOG_TIMEOUT = 40000;
//	private static final int TIMEOUT = 100000000;

    TestSipListener receiver;

    ProtocolObjects receiverProtocolObjects;

    public ShootistPrackSipServletTest(String name) {
        super(name);
        startTomcatOnStartup = false;
        autoDeployOnStartup = false;
    }

    @Override
    public void deployApplication() {
        assertTrue(tomcat.deployContext(
                projectHome + "/sip-servlets-test-suite/applications/shootist-sip-servlet/src/main/sipapp",
                "sip-test-context", "sip-test"));
    }

    @Override
    protected String getDarConfigurationFile() {
        return "file:///" + projectHome + "/sip-servlets-test-suite/testsuite/src/test/resources/"
                + "org/mobicents/servlet/sip/testsuite/simple/shootist-sip-servlet-dar.properties";
    }

    @Override
    protected void setUp() throws Exception {
        containerPort = NetworkPortAssigner.retrieveNextPort();
        super.setUp();
    }

    public void testShootistPrack() throws Exception {
        receiverProtocolObjects = new ProtocolObjects(
                "sender", "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);

        int myPort = NetworkPortAssigner.retrieveNextPort();
        Map<String, String> ctxAtts = new HashMap();
        ctxAtts.put("testPort", String.valueOf(myPort));
        ctxAtts.put("servletContainerPort", String.valueOf(containerPort));
        ctxAtts.put("prack", "true");
        receiver = new TestSipListener(myPort, containerPort, receiverProtocolObjects, false);

        SipProvider senderProvider = receiver.createProvider();

        senderProvider.addSipListener(receiver);

        receiverProtocolObjects.start();
        tomcat.startTomcat();
        deployShootist(ctxAtts, null);
        Thread.sleep(TIMEOUT);
        assertTrue(receiver.isPrackReceived());
        assertTrue(receiver.getByeReceived());
    }

    public void testShootistPrackEarlyMediaChange() throws Exception {
        receiverProtocolObjects = new ProtocolObjects(
                "sender", "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);

        int myPort = NetworkPortAssigner.retrieveNextPort();
        Map<String, String> ctxAtts = new HashMap();
        ctxAtts.put("testPort", String.valueOf(myPort));
        ctxAtts.put("servletContainerPort", String.valueOf(containerPort));
        ctxAtts.put("prack", "true");
        ctxAtts.put("testErrorResponse", "true");
        receiver = new TestSipListener(myPort, containerPort, receiverProtocolObjects, false);
        receiver.setTimeToWaitBeforeBye(DIALOG_TIMEOUT);
        LinkedList<Integer> responses = new LinkedList<Integer>();
        responses.add(180);
        responses.add(180);
        responses.add(183);
        responses.add(183);
        responses.add(183);
        receiver.setProvisionalResponsesToSend(responses);
        SipProvider senderProvider = receiver.createProvider();

        senderProvider.addSipListener(receiver);

        receiverProtocolObjects.start();
        tomcat.startTomcat();
        deployShootist(ctxAtts, null);
        Thread.sleep(DIALOG_TIMEOUT + TIMEOUT);
        assertTrue(receiver.isPrackReceived());
        List<String> allMessagesContent = receiver.getAllMessagesContent();

        assertNotNull(receiver.getMessageRequest());
        assertTrue("earlyMedia", receiver.getMessageRequest().getHeader("EarlyMediaResponses").toString().contains("3"));
        assertTrue("earlyMedia", receiver.getMessageRequest().getHeader("EarlyMedia180Responses").toString().contains("2"));
        assertTrue(allMessagesContent.size() >= 2);
        assertTrue("sipSessionReadyToInvalidate", allMessagesContent.contains("sipSessionReadyToInvalidate"));
        assertTrue("sipAppSessionReadyToInvalidate", allMessagesContent.contains("sipAppSessionReadyToInvalidate"));
    }

    public void testShootistPrackIsAnyLocalAddress() throws Exception {
        receiverProtocolObjects = new ProtocolObjects(
                "sender", "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);

        int myPort = NetworkPortAssigner.retrieveNextPort();
        Map<String, String> ctxAtts = new HashMap();
        ctxAtts.put("testPort", String.valueOf(myPort));
        ctxAtts.put("servletContainerPort", String.valueOf(containerPort));
        ctxAtts.put("prack", "true");
        receiver = new TestSipListener(myPort, containerPort, receiverProtocolObjects, false);
        SipProvider senderProvider = receiver.createProvider();

        senderProvider.addSipListener(receiver);

        receiverProtocolObjects.start();
        tomcat.removeConnector(sipConnector);
        sipIpAddress = "0.0.0.0";
        tomcat.addSipConnector(serverName, sipIpAddress, containerPort, listeningPointTransport);
        tomcat.startTomcat();
        deployShootist(ctxAtts, null);
        Thread.sleep(TIMEOUT);
        assertTrue(receiver.isPrackReceived());
        assertTrue(receiver.getByeReceived());
    }

    public void testShootistPrackCancel() throws Exception {
        receiverProtocolObjects = new ProtocolObjects(
                "sender", "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);

        int myPort = NetworkPortAssigner.retrieveNextPort();
        Map<String, String> ctxAtts = new HashMap();
        ctxAtts.put("testPort", String.valueOf(myPort));
        ctxAtts.put("servletContainerPort", String.valueOf(containerPort));
        ctxAtts.put("prack", "true");
        ctxAtts.put("cancel", "true");
        receiver = new TestSipListener(myPort, containerPort, receiverProtocolObjects, false);
        SipProvider senderProvider = receiver.createProvider();

        senderProvider.addSipListener(receiver);

        receiverProtocolObjects.start();
        tomcat.startTomcat();
        deployShootist(ctxAtts, null);
        Thread.sleep(TIMEOUT);
        assertTrue(receiver.isPrackReceived());
        assertTrue(receiver.isCancelReceived());
    }

    public void testShootistPrackCallerSendsBye() throws Exception {
        receiverProtocolObjects = new ProtocolObjects(
                "sender", "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);

        int myPort = NetworkPortAssigner.retrieveNextPort();
        Map<String, String> ctxAtts = new HashMap();
        ctxAtts.put("testPort", String.valueOf(myPort));
        ctxAtts.put("servletContainerPort", String.valueOf(containerPort));
        ctxAtts.put("prack", "true");
        receiver = new TestSipListener(myPort, containerPort, receiverProtocolObjects, true);
        SipProvider senderProvider = receiver.createProvider();
        senderProvider.addSipListener(receiver);
        receiverProtocolObjects.start();
        tomcat.startTomcat();
        deployShootist(ctxAtts, null);
        Thread.sleep(TIMEOUT);
        assertTrue(receiver.getOkToByeReceived());
    }

    public void testShootistPrackCallerTestRoute() throws Exception {
        receiverProtocolObjects = new ProtocolObjects(
                "sender", "gov.nist", TRANSPORT, AUTODIALOG, null, null, null);

        int myPort = NetworkPortAssigner.retrieveNextPort();
        Map<String, String> ctxAtts = new HashMap();
        ctxAtts.put("testPort", String.valueOf(myPort));
        ctxAtts.put("servletContainerPort", String.valueOf(containerPort));
        ctxAtts.put("prack", "true");
        ctxAtts.put("route", "sip:127.0.0.1:" + myPort);
        receiver = new TestSipListener(myPort, containerPort, receiverProtocolObjects, true);
        SipProvider senderProvider = receiver.createProvider();

        senderProvider.addSipListener(receiver);
        receiver.setAddRecordRouteForResponses(true);
        receiverProtocolObjects.start();
        tomcat.startTomcat();
        deployShootist(ctxAtts, null);
        Thread.sleep(TIMEOUT);
        assertTrue(receiver.getOkToByeReceived());
    }

    @Override
    protected void tearDown() throws Exception {
        receiverProtocolObjects.destroy();
        logger.info("Test completed");
        super.tearDown();
    }
}
