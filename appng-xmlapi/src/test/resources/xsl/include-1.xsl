<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ait="http://www.appng.org/plugin" xmlns:xs="http://www.w3.org/2001/XMLSchema" 
    exclude-result-prefixes="ait xs">
  
    <xsl:template match="content-1" mode="html-head" priority="1">
        <h1>include-1</h1>
	</xsl:template>
	
</xsl:stylesheet>
