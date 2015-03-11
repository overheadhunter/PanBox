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
package org.panbox.desktop.common.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.panbox.Settings;

public class MultiLanguage {
	
	final static String LANGUAGE         = "language";
	final static String SETTINGS_FILE    = "settings.properties";
	final static String DEFAULT_LANGUAGE = "english_EN.lng";
	
	private static MultiLanguage multiLanguage = null;
	
	private String language;
	
	private Properties languageProperties = null;
	
	/**
	 * singleton of MultiLanguage
	 * 
	 * @return
	 */
	public static MultiLanguage getInstance() {
		if (multiLanguage == null) {
			multiLanguage = new MultiLanguage();
		}
		return multiLanguage;
	}
	
	/**
	 * constructor, read current language and then corresponding the languagefile
	 */
	private MultiLanguage() {
		languageProperties  = new Properties();
		
		try {
			language = Settings.getInstance().getLanguage();

			languageProperties.load(new BufferedInputStream(new FileInputStream("lng" + File.separator + language)));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getProperty(String key, String defaultText) {
		return languageProperties.getProperty(key, defaultText);
	}

	private String getProperty(String key, String defaultText, String a) {
		String s = getProperty(key, defaultText);
		return s.replaceFirst("%s", a);
	}

//	private String getProperty(String key, String defaultText, String a, String b) {
//		String s = getProperty(key, defaultText, a);
//		return s.replaceFirst("%s", b); 		
//	}
	
		
	public String getPropMountPointNotEmpty(String a) {return getProperty("mountPointIsNotEmpty", "mountPoint: %s is not empty.", a);}
	public String getPropNoExpectedArguments() {return getProperty("noExpectedArguments", "Expected arguments [backendDirectory] [mountPoint]");}
}
