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
 *         Interface defines valid ranges for randomness tests
 * 
 */
public interface IRandomnessQualityRanges {

	/**
	 * returns the number of bytes the bitstream the tests are running on has to
	 * contain
	 * 
	 * @return number of bytes of the bitstream
	 */
	public int getBitstreamLength();

	/**
	 * returns the borders for the monobittest <br>
	 * int[0]: the lower border <br>
	 * int[1]: the upper border
	 * 
	 * @return the borders for the monobittest
	 */
	public int[] getMonobitBorders();

	/**
	 * returns the borders for the pokertest <br>
	 * double[0]: the lower border <br>
	 * double[1]: the upper border
	 * 
	 * @return the borders for the bordertest
	 */
	public double[] getPokerBorders();

	/**
	 * returns the borders for the runstest <br>
	 * int[i][0]: the lower border for the i-th run-type <br>
	 * int[i][1]: the upper border for the i-th run-type
	 * 
	 * @return the borders for the runstest
	 */
	public int[][] getRunsBorders();

	/**
	 * returns the border for the longrunstest
	 * 
	 * @return the borders for the longrunsrunstest
	 */
	public int getLongrunsBorder();

	/**
	 * returns the borders and parameters for the autocorrelationtest <br>
	 * int[0]: the lower border <br>
	 * int[1]: the upper border
	 * 
	 * @return the borders and parameters for the autocorrelationtest
	 */
	public int[] getAutocorrelationBorders();

}
