<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns:jxb="http://java.sun.com/xml/ns/jaxb">

	<xsl:output method="xml" encoding="UTF-8" indent="yes" />

	<xsl:template match="node() | @*">
		<xsl:copy>
			<xsl:apply-templates select="node() | @*" />
		</xsl:copy>
	</xsl:template>

	<!--xsl:template match="/xs:schema/xs:complexType/xs:sequence/xs:element">
		<xs:element>
			<xsl:copy-of select="@*" />
			<xsl:variable name="parent" select="../../@name" />
			<xsl:variable name="type" select="@type" />
			<xsl:variable name="parentClass">
				<xsl:if test="$parent">
					<xsl:call-template name="capitalize">
						<xsl:with-param name="text" select="$parent" />
					</xsl:call-template>
				</xsl:if>
			</xsl:variable>
			<xsl:variable name="typeClass">
				<xsl:if test="$type">
					<xsl:call-template name="capitalize">
						<xsl:with-param name="text" select="$type" />
					</xsl:call-template>
				</xsl:if>
			</xsl:variable>
			
			<xsl:variable name="isMultiple" select="(@minOccurs and @minOccurs > 1) or (@maxOccurs and @maxOccurs='unbounded')" />
			<xs:annotation>
				<xsl:if test="$isMultiple">
				<xs:documentation>One or more &#60;<xsl:value-of select="$type" />&#62;-elements</xs:documentation>
				</xsl:if>
				<xsl:if test="not($isMultiple)">
				<xs:documentation>A &#60;<xsl:value-of select="$type" />&#62;-element</xs:documentation>
				</xsl:if>				
						<xs:appinfo>
							<jxb:property>
								<xsl:if test="$isMultiple">
								<jxb:javadoc>Gets the list of {@link <xsl:value-of select="$typeClass" />}s for this {@link<xsl:value-of select="$parentClass" />}.</jxb:javadoc>
								</xsl:if>
								<xsl:if test="not($isMultiple)">
								<jxb:javadoc>Gets the {@link <xsl:value-of select="$typeClass" />} for this {@link <xsl:value-of select="$parentClass" />}.</jxb:javadoc>
								</xsl:if>
							</jxb:property>
						</xs:appinfo>
			</xs:annotation>
		</xs:element>
	</xsl:template-->

	<xsl:template match="//xs:attribute/xs:annotation/xs:documentation">
		<xs:documentation>
			<xsl:value-of select="replace(replace(text(),'\{@link ','&#60;'),'\}','&#62;')" />
		</xs:documentation>
		<xs:appinfo>
			<jxb:property>
				<jxb:javadoc>
					<xsl:value-of select="text()" />
				</jxb:javadoc>
			</jxb:property>
		</xs:appinfo>
	</xsl:template>

	<xsl:template name="capitalize">
		<xsl:param name="text" as="xs:string" />
		<xsl:value-of select="concat(upper-case(substring($text,1,1)),substring($text,2))" />
	</xsl:template>

</xsl:stylesheet>
