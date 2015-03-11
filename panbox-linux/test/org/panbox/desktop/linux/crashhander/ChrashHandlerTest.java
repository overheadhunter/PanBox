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
package org.panbox.desktop.linux.crashhander;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.panbox.desktop.linux.ProcessHandler;

import junit.framework.TestCase;

/**
 * @author Dominik Spychalski
 * 
 */

public class ChrashHandlerTest extends TestCase{

	String ps_aux_headline_1 = "USER       PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND";
	String ps_aux_headline_2 = "USER       %CPU %MEM    VSZ   RSS TTY  PID    STAT START   TIME COMMAND";
	String ps_aux_line_1 = "spychal+  2290  0.0  0.2 308784  8444 ?        Sl   15:28   0:00 /usr/lib/x86_64-linux-gnu/xfce4/panel/wrapper-1.0 /usr/lib/x86_64-linux-gnu/xfce4/panel/plugins/libsystray.so 6 16777261 systray Notification Area Area where notification ico";
	String ps_aux_line_2 = "spychal+  4711  0.0  0.2 308784  8444 ?        Sl   15:28   0:00 /usr/lib/x86_64-linux-gnu/xfce4/panel/wrapper-1.0 /usr/lib/x86_64-linux-gnu/xfce4/panel/plugins/libsystray.so 6 16777261 systray Notification Area Area where notification ico";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetPID() {
		
		assertEquals(1, ProcessHandler.getInstance().getPIDColumnIndex(ps_aux_headline_1));
		assertEquals(6, ProcessHandler.getInstance().getPIDColumnIndex(ps_aux_headline_2));	
	
		assertEquals(2290, ProcessHandler.getInstance().getPIDFromCoulumn(ps_aux_line_1, ProcessHandler.getInstance().getPIDColumnIndex(ps_aux_headline_1)));
		assertEquals(4711, ProcessHandler.getInstance().getPIDFromCoulumn(ps_aux_line_2, ProcessHandler.getInstance().getPIDColumnIndex(ps_aux_headline_1)));
	}
	
	
	
	
}
