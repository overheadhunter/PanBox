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
package org.panbox.desktop.linux;

import java.io.File;
import java.net.URL;

/**
 * Created by Dominik Spychalski
 */
public class EnvironmentHandler {

	//describes the type of the runtime environment
	public enum RE_TYPE {IDE, SYSTEM}
	
	private static URL panboxClientSource = null;
	
	private EnvironmentHandler() {
	}

	private static EnvironmentHandler instance;

	public static EnvironmentHandler getInstance() {
		if (instance == null) {
			panboxClientSource = PanboxClient.class.getProtectionDomain().getCodeSource().getLocation();
			instance = new EnvironmentHandler();
		}
		return instance;
	}
	
	public RE_TYPE getEnvironmentType(){
		RE_TYPE ret;
		
		String executable = panboxClientSource.toString().substring(panboxClientSource.toString().lastIndexOf('/') + 1, panboxClientSource.toString().length());

		if (executable.endsWith(".jar")) {
			ret = RE_TYPE.SYSTEM;
		}else{
			ret = RE_TYPE.IDE;
		}
		
		return ret;
	}
	
	public File getExecutionDirectory(){
		File ret = null;
		
		String dir = panboxClientSource.getPath().substring(0, panboxClientSource.getPath().lastIndexOf('/') + 1);
		
		File file = new File(dir);
		if(file.isDirectory() && file.exists()){
			ret = file;
		}
		
		return ret;
	}
	
	public String getExecutable(){
		int beginIndex = panboxClientSource.toString().lastIndexOf("/") + 1;
		int endIndex = panboxClientSource.toString().length();
		
		return panboxClientSource.toString().substring(beginIndex, endIndex);
	}
}
