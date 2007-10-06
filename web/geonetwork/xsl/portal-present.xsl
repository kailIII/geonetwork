<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet 	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
						xmlns:geonet="http://www.fao.org/geonetwork" 
						xmlns:xalan="http://xml.apache.org/xalan"
						exclude-result-prefixes="xsl xalan geonet">

	<xsl:include href="metadata.xsl"/>
	<xsl:include href="utils.xsl"/>

	<xsl:template match="/root">
		<xsl:variable name="output">
			<xsl:apply-templates select="response/@*" mode="copy"/>
			<xsl:apply-templates select="response/*" mode="brief"/>
		</xsl:variable>

		<response>
			<xsl:apply-templates select="xalan:nodeset($output)/*" mode="strip"/>
		</response>
	</xsl:template>
	
	<!-- ============================================================== -->

	<xsl:template match="geonet:info/title" mode="strip"/>

	<xsl:template match="@*|node()" mode="strip">
		 <xsl:copy>
			  <xsl:apply-templates select="@*|node()" mode="strip"/>
		 </xsl:copy>
	</xsl:template>

</xsl:stylesheet>
