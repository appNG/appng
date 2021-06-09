<?xml version="1.0" encoding="UTF-8"?><xsl:stylesheet xmlns:appng="http://www.appng.org/schema/platform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="appng xs" version="2.0">
	
	<xsl:output doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" encoding="UTF-8" indent="no" method="xhtml" omit-xml-declaration="yes"/>
	
	<!-- 
		include global xsl templates for developing
	
		frontend will include xsl templates automatically and delete this entries
		if template attribute @delete-includes = true
	-->
	
	
	

	<xsl:variable name="base-url" select="/master/config/base-url"/>
	
<!--[BEGIN] embed 'include-1.xsl'-->
	
<xsl:template match="content-1" mode="html-head" priority="1">
        <h1>include-1</h1>
	</xsl:template>
  
    <!--[END] embed 'include-1.xsl'--><!--[BEGIN] embed 'include-2.xsl'-->
	
<xsl:template match="content-2" mode="html-head" priority="1">
        <h1>include-2</h1>
	</xsl:template>
  
    <!--[END] embed 'include-2.xsl'--><!--[BEGIN] embed 'include-3.xsl'-->
	
<xsl:template match="content-3" mode="html-head" priority="1">
        <h1>include-3</h1>
	</xsl:template>
  
    <!--[END] embed 'include-3.xsl'--></xsl:stylesheet>