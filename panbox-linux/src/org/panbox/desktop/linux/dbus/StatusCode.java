/*
 * 
 *               Panbox - encryption for cloud storage 
 *      Copyright (C) 2014-2015 by Fraunhofer SIT and Sirrix AG 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additonally, third party code may be provided with notices and open source
 * licenses from communities and third parties that govern the use of those
 * portions, and any licenses granted hereunder do not alter any rights and
 * obligations you may have under such open source licenses, however, the
 * disclaimer of warranty and limitation of liability provisions of the GPLv3 
 * will apply to all the product.
 * 
 */
package org.panbox.desktop.linux.dbus;

import java.io.IOException;
import java.security.UnrecoverableKeyException;

import org.apache.log4j.Logger;
import org.panbox.core.identitymgmt.exceptions.ContactExistsException;
import org.panbox.desktop.common.sharemgmt.ShareManagerException;
import org.panbox.desktop.common.sharemgmt.ShareNameAlreadyExistsException;
import org.panbox.desktop.common.sharemgmt.SharePathAlreadyExistsException;

/**
 * @author Dominik Spychalski
 *
 */
public class StatusCode {
	//code = [7-4][3-0] -> [component-id][status code]
	
	//component-id
	public static final byte GENERAL = 				0b0000;
	public static final byte SHAREMANAGER = 		0b0001;
	public static final byte IDENTITYMANAGER = 		0b0010;
	public static final byte DBUS =					0b0011;
	public static final byte VFS =					0b0100;
	public static final byte CRYPTO = 				0b0101;
	public static final byte GUI = 					0b0110;
	public static final byte DEVICEMANAGER = 		0b0111;
	public static final byte KEYMANAGER = 			0b1000;
	public static final byte PAIRING = 				0b1001;
	public static final byte CLI =                  0b1010;
	public static final byte ADDRESSBOOKMANAGER = 	0b1100; 
	
	//codes
	public static final byte OK = 					0b0000;
	public static final byte ILLEGALARGUMENT = 		0b0001;
	public static final byte UNKNOWN = 				0b0010;
	public static final byte DEVICEEXISTS = 		0b0011;
	public static final byte SQLERROR = 			0b0100;
	public static final byte SHAREEXISTS = 			0b0101;
	public static final byte UNRECOVERABLEKEY = 	0b0110;
	public static final byte SEQUENCENUMBER =		0b0111;
	public static final byte SHAREMETADATA = 		0b1000;
	public static final byte SHARENOTEXISTS = 		0b1001;
	public static final byte CONTACTEXISTS = 		0b1010;
	public static final byte IOERROR = 				0b1011;
	public static final byte PINVERIFICATIONFAILED = 	0b1100;
	
	//pre defined OK-return codes
	public static final byte GENERAL_OK = buildcode(GENERAL, OK);
	public static final byte SHAREMANAGER_OK = buildcode(SHAREMANAGER, OK);
	public static final byte IDENTITYMANAGER_OK = buildcode(IDENTITYMANAGER, OK);
	public static final byte DBUS_OK = buildcode(DBUS, OK);
	public static final byte VFS_OK = buildcode(VFS, OK);
	public static final byte CRYPTO_OK = buildcode(CRYPTO, OK);
	public static final byte GUI_OK = buildcode(GUI, OK);
	public static final byte DEVICEMANAGER_OK = buildcode(DEVICEMANAGER, OK);
	public static final byte KEYMANAGER_OK = buildcode(KEYMANAGER, OK);
	public static final byte PAIRING_OK = buildcode(PAIRING, OK);
	public static final byte ADDRESSBOOKMANAGER_OK = buildcode(ADDRESSBOOKMANAGER, OK);
	
	
	private static byte getComponentCode(Exception e){
		byte component_id = GENERAL;
		
		if(e.getStackTrace().length >= 1){
			String throwingClass = e.getStackTrace()[0].getClassName().toLowerCase();
			
			if(throwingClass.contains("dbus")){
				component_id = DBUS;
			}
			
			if(throwingClass.contains("identitymanager")){
				component_id = IDENTITYMANAGER;
			}
			
			if(throwingClass.contains("addressbookmanager")){
				component_id = ADDRESSBOOKMANAGER;
			}
			
			if(throwingClass.contains("sharemgmt")){
				component_id = SHAREMANAGER;
			}
			
			if(throwingClass.contains("vfs")){
				component_id = VFS;
			}
			
			if(throwingClass.contains("gui")){
				component_id = GUI;
			}
			
			if(throwingClass.contains("crypto")){
				component_id = CRYPTO;
			}
			
			if(throwingClass.contains("devicemgmt")){
				component_id = DEVICEMANAGER;
			}
		
			if(throwingClass.contains("keymgmt")){
				component_id = KEYMANAGER;
			}
			
			if(throwingClass.contains("pairing")){
				component_id = PAIRING;
			}
			
			if(throwingClass.contains("cli")){
				component_id = CLI;
			}
		}
		return component_id;
	}
	
	private static byte getCode(Exception e){
		byte code = UNKNOWN;
		
		if(e instanceof IllegalArgumentException){
			code = ILLEGALARGUMENT;
		}
		
		if(e instanceof UnrecoverableKeyException){
			code = UNRECOVERABLEKEY;
		}
		
		if(e instanceof ShareManagerException && (e.getMessage().toLowerCase().contains("device"))){
			code = DEVICEEXISTS;
		}else if (e instanceof ShareManagerException){
			code = SQLERROR;
		}
		
		if (e instanceof ShareNameAlreadyExistsException || e instanceof SharePathAlreadyExistsException){
			code = SHAREEXISTS;
		}
		
		if (e instanceof ContactExistsException){
			code = CONTACTEXISTS;
		}
		
		if(e instanceof IOException){
			code = IOERROR;
		}
		
		return code;
	}
	
	public static byte get(Exception e){
		byte component_id = getComponentCode(e);
		byte code = getCode(e);
		
		return buildcode(component_id, code);
	}
	
	public static byte getAndLog(Logger logger, Exception e){
		byte code = StatusCode.get(e);
		logger.error("StatusCode: " + StatusCode.toString(code) + " StackTrace: " + StatusCode.getStackTrace(e));
		return code;
	}
	
	public static byte buildAndLog(Logger logger, byte component_code, byte status_code, String message){
		byte code = buildcode(component_code, status_code);
		logger.error("StatusCode: " + StatusCode.toString(code) + "Message: " + message);
		return code;
	}
	
	public static byte getComponentID(byte code){
		return (byte)(code >> 4);
	}
	
	public static byte getCode(byte code){
		byte tmp = (byte)(code >> 4);
		tmp = (byte)(tmp & 0xFF);
		
		return (byte)(tmp << 4);
	}
	
	public static byte buildcode(byte component_id, byte code){
		return (byte)((component_id << 4 ) + code);
	}
	
	public static String toString(byte code){
		return String.format("%8s", Integer.toBinaryString(code & 0xFF)).replace(' ', '0');
	}
	
	public static String getStackTrace(Exception e){
		StringBuilder sb = new StringBuilder();
		StackTraceElement[] elems = e.getStackTrace();
		
		for(int i = 0; i < elems.length; i++){
			sb.append(elems[i]);
		}
		return sb.toString();
	}
}
