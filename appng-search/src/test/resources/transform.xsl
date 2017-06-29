<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema"
	exclude-result-prefixes="#all">
	<xsl:output method="xhtml" omit-xml-declaration="yes" />
	<xsl:template match="/">
		<xsl:variable name="results" select="search/results"/>
		<html>
			<div>
				<h1>Showing page <xsl:value-of select="$results/pagination/page +1 " /> of <xsl:value-of select="$results/pagination/numberOfPages" /></h1>
				<xsl:for-each select="$results/data">
					<h2>
						<xsl:value-of select="title" />
					</h2>
					<p><xsl:value-of select="fields/field[@name='customfield']" /></p>
					<a href="{link}">
						<xsl:value-of select="link" />
					</a>
					<p>
						<xsl:value-of select="fragment" disable-output-escaping="yes" />
					</p>
					<hr />
				</xsl:for-each>
			</div>
		</html>
	</xsl:template>
</xsl:stylesheet>