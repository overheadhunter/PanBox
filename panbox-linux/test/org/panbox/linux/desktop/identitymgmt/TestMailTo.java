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
package org.panbox.linux.desktop.identitymgmt;

import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.TestCase;

import org.panbox.desktop.common.utils.DesktopApi;

public class TestMailTo extends TestCase {

	protected static void setUpBeforeClass() throws Exception {
	}

	protected static void tearDownAfterClass() throws Exception {
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

//	public void testMailTo() {
//		Desktop desktop;
//		if (Desktop.isDesktopSupported() && (desktop = Desktop.getDesktop()).isSupported(Desktop.Action.MAIL)) {
//			URI mailto;
//			try {
//				mailto = new URI(
//						"mailto:john@example.com?subject=Hello%20World");
//				desktop.mail(mailto);
//			} catch (URISyntaxException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		} else {
//			// TODO fallback to some Runtime.exec(..) voodoo?
//			throw new RuntimeException(
//					"desktop doesn't support mailto; mail is dead anyway ;)");
//		}
//	}
	
	public void testAPI()
	{
		//to test, please place a file in your home directory named "testfile.txt"
		try {
			
			String attachment = System.getProperty("user.home")+"/testfile.txt";
			
			DesktopApi.browse(new URI("mailto:john@example.com?subject=Hello%20World&body=This%20is%20a%20test%20message&attachment="+attachment));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
//	public void testMail()
//	{
//	try {
//	      // If the user specified a mailhost, tell the system about it.
////	      if (args.length >= 1) System.getProperties().put("mail.host", args[0]);
//
//		System.getProperties().put("mail.host", "localhost");
//		
//	      // A Reader stream to read from the console
////	      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//
//	      // Ask the user for the from, to, and subject lines
////	      System.out.print("From: ");
////	      String from = in.readLine();
////	      System.out.print("To: ");
////	      String to = in.readLine();
////	      System.out.print("Subject: ");
////	      String subject = in.readLine();
//
//	      // Establish a network connection for sending mail
//	      URL u = new URL("mailto:" + "test@bla.de");      // Create a mailto: URL 
//	      URLConnection c = u.openConnection(); // Create a URLConnection for it
//	      c.setDoInput(false);                  // Specify no input from this URL
//	      c.setDoOutput(true);                  // Specify we'll do output
//	      System.out.println("Connecting...");  // Tell the user what's happening
//	      System.out.flush();                   // Tell them right now
//	      c.connect();                          // Connect to mail host
//	      PrintWriter out =                     // Get output stream to mail host
//	        new PrintWriter(new OutputStreamWriter(c.getOutputStream()));
//
//	      // Write out mail headers.  Don't let users fake the From address
//	      out.println("From: \"" + "My name" + "\" <" +
//	                  System.getProperty("user.name") + "@" + 
//	                  InetAddress.getLocalHost().getHostName() + ">");
//	      out.println("To: " + "test@bla.de");
//	      out.println("Subject: " + "Subject test");
//	      out.println();  // blank line to end the list of headers
//
////	      // Now ask the user to enter the body of the message
////	      System.out.println("Enter the message. " + 
////	                         "End with a '.' on a line by itself.");
////	      // Read message line by line and send it out.
////	      String line;
////	      for(;;) {
////	        line = in.readLine();
////	        if ((line == null) || line.equals(".")) break;
////	        out.println(line);
////	      }
//
//	      // Close the stream to terminate the message 
//	      out.close();
//	      // Tell the user it was successfully sent.
//	      System.out.println("Message sent.");
//	      System.out.flush();
//	    }
//	    catch (Exception e) {  // Handle any exceptions, print error message.
//	      System.err.println(e);
//	      System.err.println("Usage: java SendMail [<mailhost>]");
//	    }
//	}
}
