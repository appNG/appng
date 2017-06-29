<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ait="http://www.appng.org/plugin" xmlns:xs="http://www.w3.org/2001/XMLSchema" 
	exclude-result-prefixes="ait xs">
	
	<xsl:output method="xhtml" encoding="UTF-8" indent="no" 
		doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" omit-xml-declaration="yes"/>
	
	<!-- 
		include global xsl templates for developing
	
		frontend will include xsl templates automatically and delete this entries
		if template attribute @delete-includes = true
	-->
	<xsl:include href="include-3.xsl" />
	<xsl:include href="include-4.xsl"/>
	

	<xsl:variable name="base-url" select="/master/config/base-url"/>
	
</xsl:stylesheet>