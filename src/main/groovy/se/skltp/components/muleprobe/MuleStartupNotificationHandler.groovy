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
package se.skltp.components.muleprobe

import groovy.util.logging.Slf4j;

import org.mule.api.context.notification.MuleContextNotificationListener
import org.mule.context.notification.MuleContextNotification
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;

@Slf4j
class MuleStartupNotificationHandler implements MuleContextNotificationListener<MuleContextNotification>{
	
	RecursiveResourceBundle rrb = new RecursiveResourceBundle("mule-probe-config","mule-probe-config-override")

	@Override
	public void onNotification(MuleContextNotification notification) {
		
		if (notification.getType().equalsIgnoreCase(MuleContextNotification.TYPE_INFO)
		&& notification.getAction() == MuleContextNotification.CONTEXT_STARTED) {
			
			log.info "############### Properties in mule-probe-config ####################"
		
			for(String key : rrb.properties.stringPropertyNames()) {
				log.info "Property: $key, value: " + rrb.getString(key);
			}
			log.info "####################################################################"
			
		}
	}
}
