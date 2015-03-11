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
 *         implements the borders for statistical randomness tests as defined in
 *         ISO18031:2005
 */
public class ISO18031Borders implements IRandomnessQualityRanges {

	public int[] getAutocorrelationBorders() {
		return new int[] { 2326, 2674 };
	}

	public int getBitstreamLength() {
		return 2500;
	}

	public int getLongrunsBorder() {
		return 26;
	}

	public int[] getMonobitBorders() {
		return new int[] { 9725, 10275 };
	}

	public double[] getPokerBorders() {
		return new double[] { 2.16, 46.17 };
	}

	public int[][] getRunsBorders() {
		int[] a = new int[] { 2315, 2685 };
		int[] b = new int[] { 1114, 1386 };
		int[] c = new int[] { 527, 723 };
		int[] d = new int[] { 240, 384 };
		int[] e = new int[] { 103, 209 };
		int[] f = new int[] { 103, 209 };
		return new int[][] { a, b, c, d, e, f };
	}

}
