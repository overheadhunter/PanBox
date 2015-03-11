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
package org.panbox.core.crypto.randomness;

/**
 * @author palige
 * 
 *         implements the borders for statistical randomness tests T1 - T5 as
 *         defined in AIS20/AIS31 "A proposal for: Functionality classes for random
 *         number generators“, Version 2.0
 */
final class AIS31Borders implements IRandomnessQualityRanges {

	public int[] getAutocorrelationBorders() {
		return new int[] { 2326, 2674 };
	}

	public int getBitstreamLength() {
		return 2500;
	}

	public int getLongrunsBorder() {
		return 33;
	}

	public int[] getMonobitBorders() {
		return new int[] { 9654, 10346 };
	}

	public double[] getPokerBorders() {
		return new double[] { 1.03, 57.4 };
	}

	public int[][] getRunsBorders() {
		int[] a = new int[] { 2267, 2733 };
		int[] b = new int[] { 1079, 1421 };
		int[] c = new int[] { 502, 748 };
		int[] d = new int[] { 233, 402 };
		int[] e = new int[] { 90, 223 };
		int[] f = new int[] { 90, 223 };
		return new int[][] { a, b, c, d, e, f };
	}

}
