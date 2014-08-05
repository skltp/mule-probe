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

import groovy.util.logging.Slf4j;

import java.text.SimpleDateFormat
import java.util.Date

import javax.jws.*

import org.soitoolkit.commons.mule.util.RecursiveResourceBundle

import se.riv.itintegration.monitoring.rivtabp21.v1.PingForConfigurationResponderInterface
import se.riv.itintegration.monitoring.v1.*
import se.skltp.components.muleprobe.MuleProbeMuleServer

@Slf4j
public class PingForConfigurationTestProducer implements PingForConfigurationResponderInterface{
	
    private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("mule-probe-config")
	
	public static boolean PINGFOR_EXCEPTION = false
	public static boolean PINGFOR_TIMEOUT = false
	
	private SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss")

	@Override
	@WebResult(name = "PingForConfigurationResponse", targetNamespace = "urn:riv:itintegration:monitoring:PingForConfigurationResponder:1", partName = "parameters")
	@WebMethod(operationName = "PingForConfiguration", action = "urn:riv:itintegration:monitoring:PingForConfigurationResponder:1:PingForConfiguration")
	public PingForConfigurationResponseType pingForConfiguration(
			@WebParam(partName = "LogicalAddress", name = "LogicalAddress", targetNamespace = "urn:riv:itintegration:registry:1", header = true) String logicalAddress,
			@WebParam(partName = "parameters", name = "PingForConfiguration", targetNamespace = "urn:riv:itintegration:monitoring:PingForConfigurationResponder:1") PingForConfigurationType parameters) {
		
		if(PINGFOR_EXCEPTION){
			//After exception reset flag to be able to return OK responses in tests
			PINGFOR_EXCEPTION = false
			throw new RuntimeException("Logical address to trigger Exeption was called")
		}else if (PINGFOR_TIMEOUT){
			//After timeout reset flag to be able to return OK responses in tests
			PINGFOR_TIMEOUT = false
			sleep Long.valueOf(rb.getString("SO_TIMEOUT_MS")) + 1000
			log.info "TestProducer force a timeout to happen..."
		}
		
		PingForConfigurationResponseType pingForConfigurationResponse = new PingForConfigurationResponseType()
		pingForConfigurationResponse.pingDateTime = formatter.format(new Date())
		pingForConfigurationResponse.version = "1.0"
		pingForConfigurationResponse.configuration.add(createConfigurationResponse("Applikation", "VP"))
		
		return pingForConfigurationResponse
	}

	private ConfigurationType createConfigurationResponse(String name, String value) {
		ConfigurationType configuration = new ConfigurationType()
		configuration.name = name
		configuration.value = value
		return configuration;
	}
}
