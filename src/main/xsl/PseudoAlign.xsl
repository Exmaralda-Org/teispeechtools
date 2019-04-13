<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xml="http://www.w3.org/XML/1998/namespace"
                xmlns="http://www.tei-c.org/ns/1.0"
                xmlns:tei="http://www.tei-c.org/ns/1.0"
                xmlns:ids="http://www.ids-mannheim.de/clarin"
                xmlns:array="http://www.w3.org/2005/xpath-functions/array"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map">

  <!-- IDEA:


  -->

  <!-- default template -->
  <xsl:template mode="#all"
                match="@*|*|processing-instruction()|comment()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" mode="#current"/>
    </xsl:copy>
  </xsl:template>
  <!-- default template end -->

  <xsl:template match="@rel-length"/>

</xsl:stylesheet>
