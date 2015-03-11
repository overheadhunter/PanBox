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
package org.panbox.desktop.sharemgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.security.UnrecoverableKeyException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.panbox.core.csp.StorageBackendType;
import org.panbox.core.exception.ShareMetaDataException;
import org.panbox.desktop.common.gui.shares.FolderPanboxShare;
import org.panbox.desktop.common.gui.shares.PanboxShare;
import org.panbox.desktop.common.sharemgmt.ShareManagerException;
import org.panbox.desktop.common.sharemgmt.ShareNameAlreadyExistsException;
import org.panbox.desktop.common.sharemgmt.SharePathAlreadyExistsException;

public abstract class GetShareForName extends ShareMgmtTest {

	@Before
	public void setUp() throws Exception {
		super.setUp();

		File test123share = testFolder.newFolder("test123share");
		if (test123share.exists()) {
			// clean test123share directory
			FileUtils.deleteDirectory(test123share);
		}
	}

	@Test
	public void test() {
		try {
			File test123share = testFolder.newFolder("test123share");
			test123share.mkdirs();

			PanboxShare s = new FolderPanboxShare(null,
					test123share.getAbsolutePath(), "Test123", 0);
			manager.addNewShare(s, password);
			debug("added new Share");

			PanboxShare share = manager.getShareForName("Test123");

			assertEquals("Test123", share.getName());
			assertEquals(test123share.getAbsolutePath(), share.getPath());

			manager.removeShare("Test123", test123share.getAbsolutePath(),
					StorageBackendType.FOLDER);
			debug("removed Share");
		} catch (IOException | ShareManagerException
				| ShareNameAlreadyExistsException
				| SharePathAlreadyExistsException | UnrecoverableKeyException
				| ShareMetaDataException e) {
			e.printStackTrace();
			fail("Exception!");
		}
	}

}
