package org.appng.persistence.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.Dialect;
import org.junit.Assert;
import org.junit.Test;


public class DialectUTF8Test {

	@Test
	public void testMariaDB103() {
		runTest(new MariaDB103DialectUTF8());
	}
	
	@Test
	public void testMySql57() {
		runTest(new MySql57DialectUTF8());
	}
	
	@Test
	public void testMySql8() {
		runTest(new MySql8DialectUTF8());
	}

	private void runTest(Dialect dialect) {
		Assert.assertEquals("varchar(255)", dialect.getTypeName(Types.VARCHAR, 255, -1, -1));
		Assert.assertEquals("varchar(1000)", dialect.getTypeName(Types.VARCHAR, 1000, -1, -1));
		Assert.assertEquals("text", dialect.getTypeName(Types.VARCHAR, 1001, -1, -1));
		Assert.assertEquals("text", dialect.getTypeName(Types.VARCHAR, 16383, -1, -1));
		Assert.assertEquals("mediumtext", dialect.getTypeName(Types.VARCHAR, 16384, -1, -1));
		Assert.assertEquals("mediumtext", dialect.getTypeName(Types.VARCHAR, 4194303, -1, -1));
		Assert.assertEquals("longtext", dialect.getTypeName(Types.VARCHAR, 4194304, -1, -1));
	}
}
