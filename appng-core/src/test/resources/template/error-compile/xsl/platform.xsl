<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ait="http://aiticon.de"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="ait xs">

	<xsl:output method="xml" omit-xml-declaration="yes" indent="no"  />

	<xsl:tmplate match="/">
		<xsl:copy-of select="." />
	</xsl:tmplate>

</xsl:stylesheet>