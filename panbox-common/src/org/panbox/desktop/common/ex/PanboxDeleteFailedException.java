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
package org.panbox.desktop.common.ex;

/**
 * @author rw
 *
 */
public class PanboxDeleteFailedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6756125420069520050L;

	/**
	 * 
	 */
	public PanboxDeleteFailedException() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public PanboxDeleteFailedException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public PanboxDeleteFailedException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public PanboxDeleteFailedException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public PanboxDeleteFailedException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	
	
}
