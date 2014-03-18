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

import static org.soitoolkit.commons.mule.mime.MimeUtil.sendFileAsMultipartHttpPost;
import static se.skltp.components.muleprobe.MuleProbeMuleServer.getAddress;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProbeServiceTestSender {

	private static final Logger log = LoggerFactory.getLogger(ProbeServiceTestSender.class);

	public static void main(String[] args) {
		String url       = getAddress("PROBESERVICE_INBOUND_URL");
    	String inputFile = "src/test/resources/testfiles/probeService/input.txt";
		int timeout      = 5000;

		log.info("Post message to: {}, {} chars", url, inputFile.length());
    	sendFileAsMultipartHttpPost(url, new File(inputFile), "payload", false, timeout);
	}
}