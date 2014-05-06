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

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.riv.itintegration.monitoring.v1.PingForConfigurationType;

public class PingForConfigurationRequestTransformer extends AbstractMessageTransformer{
	
	private static final Logger log = LoggerFactory.getLogger(PingForConfigurationRequestTransformer.class);

    /**
     * Message aware transformer that ...
     */
    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {

        // Perform any message aware processing here, otherwise delegate as much as possible to pojoTransform() for easier unit testing
        return pojoTransform(message.getPayload(), outputEncoding);
    }

	/**
     * Simple pojo transformer method that can be tested with plain unit testing...
	 */
	protected Object pojoTransform(Object src, String encoding) throws TransformerException {

		log.debug("Transforming payload: {}", src);

		PingForConfigurationType reqOut = new PingForConfigurationType();
		reqOut.setServiceContractNamespace("urn:riv:itintegration:monitoring:PingForConfigurationResponder:1");
		reqOut.setLogicalAddress("LogicalAddress");
		
		Object[] reqOutList = new Object[] {"LogicalAddress", reqOut};

		log.debug("Transformed payload: {}, serviceContractNamespace: {}", reqOutList, reqOut.getServiceContractNamespace());
		
		return reqOutList;
	}

}
