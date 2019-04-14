<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xml="http://www.w3.org/XML/1998/namespace"
                xmlns:tei="http://www.tei-c.org/ns/1.0">

  <!-- default template -->
  <xsl:template match="@*|*|processing-instruction()|comment()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  <!-- default template end -->

  <xsl:template match="@rel-length"/>

</xsl:stylesheet>
