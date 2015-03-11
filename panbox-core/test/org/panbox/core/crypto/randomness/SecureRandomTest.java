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

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.panbox.core.exception.RandomDataGenerationException;

public class SecureRandomTest {

	private SecureRandomWrapper wrapper;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		this.wrapper = SecureRandomWrapper.getInstance();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testInstantiation() throws RandomDataGenerationException {
		assertNotNull(this.wrapper);
		assertTrue(this.wrapper.equals(SecureRandomWrapper.getInstance()));
	}

	@Test(expected = RandomDataGenerationException.class)
	public void testByteBufferNullException() throws Exception {
		wrapper.nextBytes(null);

	}

	@Test(expected = RandomDataGenerationException.class)
	public void testByteBufferEmptyException() throws Exception {
		wrapper.nextBytes(new byte[0]);
	}

	@Test
	public void testReseeding() throws Exception {
		byte[] buffer = new byte[1024];
		// random bytes may have been generated before, reset seed counter
		wrapper.setSeed(System.nanoTime());

		long nInterations = SecureRandomWrapper.RESEEDING_LIMIT / 1024;
		// nothing bad should happen here
		for (int i = 0; i < nInterations; i++) {
			wrapper.nextBytes(buffer);
		}
		assertTrue(wrapper.nBytesGenerated <= SecureRandomWrapper.RESEEDING_LIMIT);
		long old = wrapper.nBytesGenerated;
		// reset
		wrapper.nextBytes(buffer);
		assertTrue(wrapper.nBytesGenerated < old);
	}

	/**
	 * tests if data delivered by {@link SecureRandomWrapper} is sufficiently
	 * random w.r.t. tests as defined in AIS20/ISO18031
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRandomnessQuality() throws Exception {
		byte[] b = new byte[2500];
		wrapper.nextBytes(b);
		// byte[] nullbyte = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		// 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		// 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		// System.arraycopy(nullbyte, 0, b, 2134, nullbyte.length);

		IRandomnessQualityRanges ais20 = new AIS31Borders();
		IRandomnessQualityRanges ais31 = new ISO18031Borders();
		RandomnessQualityCheck rQuality = new RandomnessQualityCheck();

		long t1 = System.currentTimeMillis();

		// do the AIS20 tests
		rQuality.setBorders(ais20);
		assertTrue(rQuality.monobitTest(b));
		assertTrue(rQuality.pokerTest(b));
		assertTrue(rQuality.runTest(b));
		assertTrue(rQuality.longrunTest(b));
		assertTrue(rQuality.autocorrelationTest(b));

		System.out.println("time delta: " + (System.currentTimeMillis() - t1));
		t1 = System.currentTimeMillis();

		// do the AIS31 tests
		rQuality.setBorders(ais31);
		assertTrue(rQuality.monobitTest(b));
		assertTrue(rQuality.pokerTest(b));
		assertTrue(rQuality.runTest(b));
		assertTrue(rQuality.longrunTest(b));
		assertTrue(rQuality.autocorrelationTest(b));

		System.out.println("time delta: " + (System.currentTimeMillis() - t1));
	}
}
