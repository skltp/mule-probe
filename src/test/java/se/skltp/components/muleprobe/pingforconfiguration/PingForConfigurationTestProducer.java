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
package se.skltp.components.muleprobe.pingforconfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;

import se.riv.itintegration.monitoring.rivtabp21.v1.PingForConfigurationResponderInterface;
import se.riv.itintegration.monitoring.v1.ConfigurationType;
import se.riv.itintegration.monitoring.v1.PingForConfigurationResponseType;
import se.riv.itintegration.monitoring.v1.PingForConfigurationType;
import se.skltp.components.muleprobe.MuleProbeMuleServer;

public class PingForConfigurationTestProducer implements PingForConfigurationResponderInterface{
	
	private static final Logger logger = LoggerFactory.getLogger(PingForConfigurationTestProducer.class);
    private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("mule-probe-config");
	
	public static boolean PINGFOR_EXCEPTION = false;
	public static boolean PINGFOR_TIMEOUT = false;
	
	private SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");

	@Override
	@WebResult(name = "PingForConfigurationResponse", targetNamespace = "urn:riv:itintegration:monitoring:PingForConfigurationResponder:1", partName = "parameters")
	@WebMethod(operationName = "PingForConfiguration", action = "urn:riv:itintegration:monitoring:PingForConfigurationResponder:1:PingForConfiguration")
	public PingForConfigurationResponseType pingForConfiguration(
			@WebParam(partName = "LogicalAddress", name = "LogicalAddress", targetNamespace = "urn:riv:itintegration:registry:1", header = true) String logicalAddress,
			@WebParam(partName = "parameters", name = "PingForConfiguration", targetNamespace = "urn:riv:itintegration:monitoring:PingForConfigurationResponder:1") PingForConfigurationType parameters) {
		
		if(PINGFOR_EXCEPTION){
			throw new RuntimeException("Logical address to trigger Exeption was called");
		}else if (PINGFOR_TIMEOUT){
			forceTimeout();
		}
		
		PingForConfigurationResponseType pingForConfigurationResponse = new PingForConfigurationResponseType();
		pingForConfigurationResponse.setPingDateTime(formatter.format(new Date()));
		pingForConfigurationResponse.setVersion("1.0");
		pingForConfigurationResponse.getConfiguration().add(createConfigurationResponse("Applikation", "VP"));
		
		return pingForConfigurationResponse;
	}

	private ConfigurationType createConfigurationResponse(String name, String value) {
		ConfigurationType configuration = new ConfigurationType();
		configuration.setName(name);
		configuration.setValue(value);
		return configuration;
	}
	
	private void forceTimeout() {
		try {
			logger.info("TestProducer force a timeout to happen...");
			Thread.sleep(Long.valueOf(rb.getString("SERVICE_TIMEOUT_MS")) + 1000);
		} catch (InterruptedException e) {
		}
	}

}
