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
package se.skltp.components.muleprobe;

import groovy.util.logging.*

import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

import org.apache.commons.httpclient.*
import org.apache.commons.httpclient.methods.*
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle

@Slf4j
@Path("/")
public class ProbeStatusService {
	
	final String payload = 
		'''<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:urn="urn:riv:itintegration:registry:1" xmlns:urn1="urn:riv:itintegration:monitoring:PingForConfigurationResponder:1">
			   <soapenv:Header>
			      <urn:LogicalAddress>?</urn:LogicalAddress>
			   </soapenv:Header>
			   <soapenv:Body>
			      <urn1:PingForConfiguration>
			         <urn1:serviceContractNamespace>?</urn1:serviceContractNamespace>
			         <urn1:logicalAddress>?</urn1:logicalAddress>
			      </urn1:PingForConfiguration>
			   </soapenv:Body>
			</soapenv:Envelope>
		'''
	
	final RecursiveResourceBundle rrb = new RecursiveResourceBundle("mule-probe-config","mule-probe-config-override")
	
	final String probeFilePath = rrb.getString('PROBESERVICE_FILE')
	
	final String downCriteria = rrb.getString('PROBE_DOWN_CRITERIA')
	
	final String defaultConnectionTimeout = rrb.getString("CONNECTION_TIMEOUT_MS")
	
	final String defaultResponseTimeout = rrb.getString("SO_TIMEOUT_MS")
	
	final String defaultOkReturnString = rrb.getString("PROBE_RETURN_OK_STRING");
	
	def probeFile = new File(probeFilePath)
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/probe")
	public Response getStatus(@DefaultValue("false") @QueryParam("verbose") boolean verbose) {
		
		log.debug "getStatus() called on all resources behind probe using verbose=$verbose"
			
		//Get all services to check status on
		List<ProcessingStatus> servicesToProcess = getServicesToProcess()
		
		//Check if probe is down
		for (serviceToProcess in servicesToProcess) {
			if(!serviceToProcess.probeAvailable){
				return Response.status(Status.SERVICE_UNAVAILABLE).entity(servicesToProcess).build();
			}
		}
		
		//Do the status check on all components behind the probe
		for (serviceToProcess in servicesToProcess) {
			doPost(serviceToProcess)
		}
		
		//If any service calls reports failure, return the complete result and status service unavailable
		for (serviceToProcess in servicesToProcess) {
			if(!serviceToProcess.serviceAvailable){
				log.error "Resource with name: $serviceToProcess.name, is not available for service"
				return Response.status(Status.SERVICE_UNAVAILABLE).entity(servicesToProcess).build();
			}
		}
		
		//No service reports unavailable, return OK
		if(!verbose) {
			return Response.ok(defaultOkReturnString).build()
		} else {
			return Response.ok(servicesToProcess).build()
		}
		
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/probe/{name}")
	public Response getStatusOnName(@PathParam("name") String name, @DefaultValue("false") @QueryParam("verbose") boolean verbose) {
		
		log.debug "getStatus() called on resource name=$name using verbose=$verbose"
		
		//Get service to check status on
		def serviceToProcess = getServiceToProcess("probe.ping.$name")
		
		//Check if the requested resource is available in properties file
		if(serviceToProcess.name == null){
			log.error "Requested resource with name: $name, was not found in list of resources in property file"
			return Response.status(Status.NOT_FOUND).entity(serviceToProcess).build()
		}
		
		if(!serviceToProcess.probeAvailable){
			return Response.status(Status.SERVICE_UNAVAILABLE).entity(serviceToProcess).build();
		}
	
		//Probe is up, lets check status on selected resource
		doPost(serviceToProcess)
		
		if(!serviceToProcess.serviceAvailable){
			log.error "Requested resource with name: $name, is not avaialable for service"
			return Response.status(Status.SERVICE_UNAVAILABLE).entity(serviceToProcess).build()
		}
		
		/*
		 * Check which type of response that we should return, simple string or JSON
		 */
		if(!verbose) {
			return Response.ok(defaultOkReturnString).build()
		} else {	
			return Response.ok(serviceToProcess).build()
		}
	}
		
	/*
	 * Get all services to process
	 */
	List<ProcessingStatus> getServicesToProcess(){	
		
		List<ProcessingStatus> servicesToProcess = new ArrayList<ProcessingStatus>()
		
		//We would like to exclude e.g probe.ping.vp.connecttimeout and probe.ping.vp.responsetimeout
		//and only get probe.ping.vp
		for(String key : rrb.properties.stringPropertyNames()) {
			if(key.startsWith("probe.ping") && !key.contains("timeout")){
				servicesToProcess.add(getServiceToProcess(key))
			} 
		}
		
		return servicesToProcess
	}
	
	/*
	 * Get the service to ping by naming convention probe.ping.<name>=<name>;<url>, e.g
	 * probe.ping.vp=vp;http://path/to/vp/pingForConfiguration
	 */
	ProcessingStatus getServiceToProcess(String key){
		
		log.debug "Get service to process using property key: $key"
		
		def serviceToProcess = new ProcessingStatus()
		
		addProbeStatus(serviceToProcess)
		
		String property = rrb.properties.getProperty(key)
		
		//Name and URL split by ;
		if(property != null){
			def nameAndUrl = property.tokenize(";")
			serviceToProcess.name = nameAndUrl[0]
			serviceToProcess.url = nameAndUrl[1]
		}
		
		return serviceToProcess
	}
	
	void addConnectTimeout(ProcessingStatus prStatus){
		
		def connectTimeout = rrb.getString("probe.ping." + prStatus.name + ".connecttimeout")
		
		if(connectTimeout == null){
			connectTimeout = defaultConnectionTimeout
		}
		prStatus.connecttimeout = connectTimeout
	}
	
	void addResponseTimeout(ProcessingStatus prStatus){
		
		def responseTimeout = rrb.getString("probe.ping." + prStatus.name + ".responsetimeout")
		
		if(responseTimeout == null){
			responseTimeout = defaultResponseTimeout
		}
		prStatus.responsetimeout = responseTimeout
	}
	
	/*
	 * Determine the status on the probe it self. Status of the probe itself
	 * is set in the file at probeFilePath.
	 */
	void addProbeStatus(ProcessingStatus prStatus){
		
		if(!probeFile.exists()){
			prStatus.probeMessage = "Configured probeFile $probeFilePath does not exist, muleprobe signals unavailable when file is missing"
			prStatus.probeAvailable = false
			return
		}
		
		//Read status from probeFilePath
		String probeStatus = probeFile.text
		
		if(probeStatus ==  null || downCriteria.equals(probeStatus.trim())){
			prStatus.probeMessage = "Muleprobe probeFile signals $probeStatus, no check against producers will be performed"
			prStatus.probeAvailable = false
		}else{
			prStatus.probeMessage = "Muleprobe probeFile signals $probeStatus"
			prStatus.probeAvailable = true
		}
		
		log.info("Muleprobe signals serviceAvailable {} and message {}", prStatus.probeAvailable, prStatus.probeMessage)
	}
	
	//Do HTTP Post on the selected resource
	void doPost(ProcessingStatus serviceToProcess){
		
		addConnectTimeout(serviceToProcess)
		addResponseTimeout(serviceToProcess)
		
		def client = new HttpClient()
		client.httpConnectionManager.params.connectionTimeout = Long.valueOf(serviceToProcess.connecttimeout)
		client.httpConnectionManager.params.soTimeout = Long.valueOf(serviceToProcess.responsetimeout)
		
		log.debug "Connection timeout used for resource " + serviceToProcess.name + ": " + client.httpConnectionManager.params.connectionTimeout
		log.debug "Response timeout used for resource " + serviceToProcess.name + ": " + client.httpConnectionManager.params.soTimeout
		
		def method = new PostMethod(serviceToProcess.url)
		method.setRequestEntity(new StringRequestEntity(payload, 'text/xml', 'utf-8' ))
		method.setRequestHeader("Connection","close");
		
		try {
			def status = client.executeMethod(method)
			
			log.info "Resource: $serviceToProcess.name, HTTP status code: ${status}, URL: $serviceToProcess.url"
			
			if(status == HttpStatus.SC_OK){
				serviceToProcess.serviceAvailable = true
			}else{
				serviceToProcess.serviceAvailable = false
			}
			
			serviceToProcess.serviceMessage = method.getResponseBodyAsString()
			
			
			method.releaseConnection()
		} catch (Exception e) {
			log.error "Exception: " + e.message + " occured. Resource: " + serviceToProcess.name + ", URL " + serviceToProcess.url
			log.error "Exception: $e"
			
			serviceToProcess.serviceAvailable = false
			serviceToProcess.serviceMessage = e.message
		}
	}
}
