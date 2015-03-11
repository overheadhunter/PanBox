package org.panbox.desktop.common.vfs;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

final class UnconsequentialFiles {

	// private static final String[] unconsequential = {".hidden",
	// "autorun.inf", "Thumbs.db", ".DS_STORE", ".Trash", ".Trash-1000",
	// ".xdg-volume-info",
	// ".directory"};

	private static final String[] unconsequential = { ".hidden", "Thumbs.db",
			".DS_STORE", ".Trash", ".Trash-1000", ".xdg-volume-info",
			".directory" };

	static final boolean clearUnconsequentialFiles(final File directory) {
		if (!directory.isDirectory()) {
			return false;
		}
		boolean returnValue = false;
		File f;
		for (final String s : unconsequential) {
			f = new File(directory, s);
			if (f.exists()) {
				returnValue = f.delete() || returnValue;
			}
		}
		return returnValue;
	}

	public static boolean isUnconsequential(String fileName) {
		int nseparators = fileName.startsWith(File.separator) ? StringUtils
				.countMatches(fileName, File.separator) - 1 : StringUtils
				.countMatches(fileName, File.separator);
				
		// TODO: for dolphin bug with .directory.lock use this
		// if(fileName.contains(".directory"))
		// {
		// return true;
		// }
				
		if (nseparators >= 1) {
			return false;
		} else {
			for (String s : unconsequential) {
				if (fileName.contains(s)) {
					return true;
				}
			}
			return false;
		}
	}
}
