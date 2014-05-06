/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skltp.components.muleprobe.probeservice;

import static org.junit.Assert.assertEquals;
import static se.skltp.components.muleprobe.MuleProbeMuleServer.getAddress;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.file.FileUtil;
import org.soitoolkit.commons.mule.rest.RestClient;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;

import se.skltp.components.muleprobe.pingforconfiguration.PingForConfigurationTestProducer;

public class ProbeServiceIntegrationTest extends AbstractTestCase {

	private static final Logger log = LoggerFactory.getLogger(ProbeServiceIntegrationTest.class);
	private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("mule-probe-config", "mule-probe-config-override");
	private static final String HTTP_CONNECTOR = "soitoolkit-http-connector";
	
	private static final String PROBESERVICE_TESTSTUB_FOLDER = rb.getString("PROBESERVICE_TESTSTUB_FOLDER");

	private RestClient restClient;

	public ProbeServiceIntegrationTest() {

		// Only start up Mule once to make the tests run faster...
		// Set to false if tests interfere with each other when Mule is started
		// only once.
		setDisposeContextPerClass(true);
	}

	protected String getConfigResources() {
		return "soitoolkit-mule-jms-connector-activemq-embedded.xml," + 
				"mule-probe-common.xml,"+ 
				"teststub-services/pingForConfiguration-teststub-service.xml," +
				"probeService-service.xml";
	}

	@Override
	protected void doSetUp() throws Exception {
		super.doSetUp();
		restClient = new RestClient(muleContext, HTTP_CONNECTOR);
		FileUtil.initFolder(new File(PROBESERVICE_TESTSTUB_FOLDER));
	}
	
	@Before
	public void setUpTest(){
		PingForConfigurationTestProducer.PINGFOR_EXCEPTION = false;
		PingForConfigurationTestProducer.PINGFOR_TIMEOUT = false;
	}

	@Test
	public void probeService_returns_ok() throws Exception {
		
		setStatusInProbeFile("OK");
		
		// Call the http-service with proper input
		MuleMessage response = restClient.doHttpPostRequest_JsonContent(getAddress("PROBESERVICE_INBOUND_URL"), "");
		
		// Assert http-status 200
		assertEquals(Integer.toString(Status.OK.getStatusCode()), response.getInboundProperty("http.status"));

		assertEquals("OK", getProbeResultAsString(response));
	}
	
	@Test
	public void probeService_returns_down() throws Exception {
		
		setStatusInProbeFile("DOWN");
		
		// Call the http-service with proper input
		MuleMessage response = restClient.doHttpPostRequest_JsonContent(getAddress("PROBESERVICE_INBOUND_URL"), "");
		
		// Assert http-status 200
		assertEquals(Integer.toString(Status.OK.getStatusCode()), response.getInboundProperty("http.status"));

		assertEquals("DOWN", getProbeResultAsString(response));
	}
	
	@Test
	public void probeService_returns_error_when_file_is_missing() throws Exception {
		
		// Call the http-service with proper input
		MuleMessage response = restClient.doHttpPostRequest_JsonContent(getAddress("PROBESERVICE_INBOUND_URL"), "");
		
		// Assert http-status 500
		assertEquals(Integer.toString(Status.INTERNAL_SERVER_ERROR.getStatusCode()), response.getInboundProperty("http.status"));
		
		assertEquals(rb.getString("PROBESERVICE_ERROR_TEXT"), getProbeResultAsString(response));
	}
	
	@Test
	public void probeService_returns_ok_when_pingforconfiguration_returns_ok() throws Exception {
		
		setStatusInProbeFile("OK");
		
		// Call the http-service with proper input
		MuleMessage response = restClient.doHttpPostRequest_JsonContent(getAddress("PROBESERVICE_INBOUND_URL"), "");
		
		// Assert http-status 500
		assertEquals(Integer.toString(Status.OK.getStatusCode()), response.getInboundProperty("http.status"));
		
		assertEquals("OK", getProbeResultAsString(response));
	}
	
	@Test
	public void probeService_returns_error_when_any_pingforconfiguration_returns_error() throws Exception {
		
		setStatusInProbeFile("OK");
		setTestProducerToReturnError();
		
		// Call the http-service with proper input
		MuleMessage response = restClient.doHttpPostRequest_JsonContent(getAddress("PROBESERVICE_INBOUND_URL"), "");
		
		// Assert http-status 500
		assertEquals(Integer.toString(Status.INTERNAL_SERVER_ERROR.getStatusCode()), response.getInboundProperty("http.status"));
		
		assertEquals(rb.getString("PROBESERVICE_ERROR_TEXT"), getProbeResultAsString(response));
	}
	
	@Test
	public void probeService_returns_error_when_any_pingforconfiguration_timeout() throws Exception {
		
		setStatusInProbeFile("OK");
		setTestProducerToTimeout();
		
		// Call the http-service with proper input
		MuleMessage response = restClient.doHttpPostRequest_JsonContent(getAddress("PROBESERVICE_INBOUND_URL"), "");
		
		// Assert http-status 500
		assertEquals(Integer.toString(Status.INTERNAL_SERVER_ERROR.getStatusCode()), response.getInboundProperty("http.status"));
		
		assertEquals(rb.getString("PROBESERVICE_ERROR_TEXT"), getProbeResultAsString(response));
	}
	
	private void setTestProducerToTimeout() {
		PingForConfigurationTestProducer.PINGFOR_TIMEOUT = true;		
	}

	private void setTestProducerToReturnError() {
		PingForConfigurationTestProducer.PINGFOR_EXCEPTION = true;	
	}

	private String getProbeResultAsString(MuleMessage response) {
		String probeResult = IOUtils.toString((InputStream)response.getPayload());
		return probeResult;
	}
	
	private void setStatusInProbeFile(String status) throws Exception{
		File probeFile = new File(rb.getString("PROBESERVICE_TESTSTUB_FILE"));
        BufferedWriter out = new BufferedWriter(new FileWriter(probeFile));
        out.write(status);
        out.close();
	}
}
