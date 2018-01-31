package org.appng.tools.os;

import org.junit.Assert;
import org.junit.Test;

public class OperatingSystemTest {

	@Test
	public void testOsDetection() {
		Assert.assertEquals(OperatingSystem.LINUX, OperatingSystem.detect("Debian GNU/Linux Stretch"));
		Assert.assertEquals(OperatingSystem.LINUX, OperatingSystem.detect("Unix"));
		Assert.assertEquals(OperatingSystem.LINUX, OperatingSystem.detect("Minix"));
		Assert.assertEquals(OperatingSystem.WINDOWS, OperatingSystem.detect("Microsoft Windows Server 2016"));
		Assert.assertEquals(OperatingSystem.MACOSX, OperatingSystem.detect("Mac OS X"));
		Assert.assertEquals(OperatingSystem.OTHER, OperatingSystem.detect("MS DOS 5.0"));
		Assert.assertEquals(OperatingSystem.OTHER, OperatingSystem.detect(""));
		Assert.assertEquals(OperatingSystem.OTHER, OperatingSystem.detect(null));

		OperatingSystem os = OperatingSystem.detect();
		if (OperatingSystem.isLinux()) {
			Assert.assertEquals(OperatingSystem.LINUX, os);
			Assert.assertEquals(true, OperatingSystem.isOs(OperatingSystem.LINUX));
			Assert.assertEquals(false, OperatingSystem.isOs(OperatingSystem.WINDOWS));
			Assert.assertEquals(false, OperatingSystem.isOs(OperatingSystem.MACOSX));
			Assert.assertEquals(false, OperatingSystem.isOs(OperatingSystem.OTHER));
		}
		if (OperatingSystem.isWindows()) {
			Assert.assertEquals(OperatingSystem.WINDOWS, os);
			Assert.assertEquals(false, OperatingSystem.isOs(OperatingSystem.LINUX));
			Assert.assertEquals(true, OperatingSystem.isOs(OperatingSystem.WINDOWS));
			Assert.assertEquals(false, OperatingSystem.isOs(OperatingSystem.MACOSX));
			Assert.assertEquals(false, OperatingSystem.isOs(OperatingSystem.OTHER));
		}
		if (OperatingSystem.isMac()) {
			Assert.assertEquals(OperatingSystem.MACOSX, os);
			Assert.assertEquals(false, OperatingSystem.isOs(OperatingSystem.LINUX));
			Assert.assertEquals(false, OperatingSystem.isOs(OperatingSystem.WINDOWS));
			Assert.assertEquals(true, OperatingSystem.isOs(OperatingSystem.MACOSX));
			Assert.assertEquals(false, OperatingSystem.isOs(OperatingSystem.OTHER));
		}
		if (OperatingSystem.isOther()) {
			Assert.assertEquals(OperatingSystem.OTHER, os);
			Assert.assertEquals(false, OperatingSystem.isOs(OperatingSystem.LINUX));
			Assert.assertEquals(false, OperatingSystem.isOs(OperatingSystem.WINDOWS));
			Assert.assertEquals(false, OperatingSystem.isOs(OperatingSystem.MACOSX));
			Assert.assertEquals(true, OperatingSystem.isOs(OperatingSystem.OTHER));
		}
	}

}
