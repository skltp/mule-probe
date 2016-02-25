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
package se.skltp.components.muleprobe.pingforconfiguration

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static se.skltp.components.muleprobe.MuleProbeMuleServer.getAddress
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.InputStream

import javax.ws.rs.core.Response.Status

import org.junit.Before
import org.junit.Test
import org.mule.api.MuleMessage
import org.mule.util.IOUtils
import org.soitoolkit.commons.mule.file.FileUtil
import org.soitoolkit.commons.mule.rest.RestClient
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle

@Slf4j
public class ProbeServiceIntegrationNoServicesTest extends AbstractTestCase {

	private static final String CONFIG_OVERRIDE_FILENAME = "mule-probe-config-override-test-without-services";
	private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("mule-probe-config", CONFIG_OVERRIDE_FILENAME)
	private static final String HTTP_CONNECTOR = "soitoolkit-http-connector"

	private static final String PROBESERVICE_OK_RESULT = rb.getString("PROBE_RETURN_OK_STRING")
	
	private static final String PROBESERVICE_FILE = rb.getString("PROBESERVICE_FILE")
	
	private RestClient restClient

	public ProbeServiceIntegrationNoServicesTest() {

		// Only start up Mule once to make the tests run faster...
		// Set to false if tests interfere with each other when Mule is started
		// only once.
		setDisposeContextPerClass(true);
		
		// override the config-override file to add a test specific file
		System.setProperty("mule.probe.config.override.filename", CONFIG_OVERRIDE_FILENAME);
	}	

	protected String getConfigResources() {
		return "soitoolkit-mule-jms-connector-activemq-embedded.xml," + 
				"mule-probe-common.xml,"+ 
				"teststub-services/pingForConfiguration-teststub-service.xml," +
				"probeService-service.xml"
	}

	@Override
	protected void doSetUp() throws Exception {
		super.doSetUp();
		restClient = new RestClient(muleContext, null)
		FileUtil.initFolder(new File(PROBESERVICE_FILE).getParentFile())
	}
	
	@Before
	public void setUpTest(){
		PingForConfigurationTestProducer.PINGFOR_EXCEPTION = false
		PingForConfigurationTestProducer.PINGFOR_TIMEOUT = false
	}
	
	@Test
	public void probeService_returns_error_when_selected_resource_is_not_found() throws Exception {

		setStatusInProbeFile("OK")

		// Call the http-service with proper input
		MuleMessage response = restClient.doHttpGetRequest_JsonContent(getAddress("PROBESERVICE_INBOUND_URL") + "/probe/unknown")

		// Assert http-status 404
		assertEquals(Integer.toString(Status.NOT_FOUND.getStatusCode()), response.getInboundProperty("http.status"))
	}
	
	@Test
	public void probeService_returns_ok_with_no_resources() throws Exception {
		
		setStatusInProbeFile("OK")
		
		// Call the http-service with proper input
		MuleMessage response = restClient.doHttpGetRequest_JsonContent(getAddress("PROBESERVICE_INBOUND_URL") + "/probe")
		
		// Assert http-status 200
		assertEquals(Integer.toString(Status.OK.getStatusCode()), response.getInboundProperty("http.status"))
		
		// Assert OK return string
		assertEquals(PROBESERVICE_OK_RESULT, getProbeResultAsString(response));
	}

	@Test
	public void probeService_returns_ok_with_no_resources_verbose() throws Exception {
		
		setStatusInProbeFile("OK")
		
		// Call the http-service with proper input
		MuleMessage response = restClient.doHttpGetRequest_JsonContent(getAddress("PROBESERVICE_INBOUND_URL") + "/probe?verbose=true")
		
		// Assert http-status 200
		assertEquals(Integer.toString(Status.OK.getStatusCode()), response.getInboundProperty("http.status"))
		
		def slurper = new JsonSlurper() 
		def result = slurper.parseText(getProbeResultAsString(response))
		
		assert result.probeAvailable == true
		assert result.probeMessage == "Muleprobe probeFile signals OK"
		
	}
	
	@Test
	public void probeService_configured_to_be_down_with_no_resources() throws Exception {

		setStatusInProbeFile("DOWN")

		// Call the http-service with proper input
		MuleMessage response = restClient.doHttpGetRequest_JsonContent(getAddress("PROBESERVICE_INBOUND_URL") + "/probe")

		// Assert http-status 503 SERVICE UNAVAILABLE
		assertEquals(Integer.toString(Status.SERVICE_UNAVAILABLE.getStatusCode()), response.getInboundProperty("http.status"))
	}
	
	@Test
	public void probeService_returns_error_when_probefile_is_missing() throws Exception {
		
		assertTrue(! new File(PROBESERVICE_FILE).exists())
		
		// Call the http-service with proper input
		MuleMessage response = restClient.doHttpGetRequest_JsonContent(getAddress("PROBESERVICE_INBOUND_URL") + "/probe")
		
		// Assert http-status 503 SERVICE UNAVAILABLE
		assertEquals(Integer.toString(Status.SERVICE_UNAVAILABLE.getStatusCode()), response.getInboundProperty("http.status"))
	}

	private void setTestProducerToTimeout() {
		PingForConfigurationTestProducer.PINGFOR_TIMEOUT = true	
	}

	private void setTestProducerToReturnError() {
		PingForConfigurationTestProducer.PINGFOR_EXCEPTION = true
	}

	private String getProbeResultAsString(MuleMessage response) {
		return IOUtils.toString((InputStream)response.getPayload())		
	}
	
	private void setStatusInProbeFile(String status) throws Exception{
		File probeFile = new File(PROBESERVICE_FILE)
        BufferedWriter out = new BufferedWriter(new FileWriter(probeFile))
        out.write(status)
        out.close()
	}
}
