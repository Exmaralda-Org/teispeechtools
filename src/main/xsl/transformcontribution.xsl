<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns="http://www.tei-c.org/ns/1.0" xmlns:tei="http://www.tei-c.org/ns/1.0"  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <!-- changes on 04-02-2009: -->
    <!-- symbol for clitics is now _ instead of + -->
    <!-- symbol for alternative is now / instaed of | -->
    <!-- symbol for breathe is now ° instead of _ -->
    <!-- symbol for unintelligible is now + instead of * -->
    <!-- changes on 06-03-2009: -->
    <!-- removed template for UNINTELLIGIBLE -->

    <xsl:template match="contribution">
        <xsl:element name="seg">
          <xsl:attribute name="type">contribution</xsl:attribute>
            <xsl:copy-of select="@speaker-reference"/>
            <xsl:copy-of select="@start-reference"/>
            <xsl:copy-of select="@end-reference"/>
            <xsl:copy-of select="@parse-level"/>
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="GAT_WORD">
        <xsl:element name="w">
            <!-- changed on 04-02-2009 -->
            <xsl:if test="preceding-sibling::*[1][self::GAT_WORDBOUNDARY and text()='_']">
                <xsl:attribute name="transition">assimilated</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="GAT_PAUSE">
        <xsl:element name="pause">
            <xsl:attribute name="duration">
                <xsl:choose>
                    <xsl:when test="text()='(.)'">micro</xsl:when>
                    <xsl:when test="text()='(-)'">short</xsl:when>
                    <xsl:when test="text()='(--)'">medium</xsl:when>
                    <xsl:when test="text()='(---)'">long</xsl:when>
                    <xsl:otherwise><xsl:value-of select="substring-before(substring-after(text(),'('), ')')"/></xsl:otherwise>
                </xsl:choose>                
            </xsl:attribute>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="GAT_NON_PHO">
        <xsl:element name="non-phonological">
            <xsl:attribute name="description">
                <xsl:value-of select="substring-before(substring-after(text(),'(('), '))')"/>
            </xsl:attribute>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="tei:anchor">
        <xsl:copy>
            <xsl:copy-of select="@*|*"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="GAT_BREATHE">
      <vocal>
        <desc>
          <xsl:variable name="length">
            <xsl:value-of select="string-length()-1"/>
          </xsl:variable>
          <xsl:attribute name="length">
            <xsl:choose>
              <xsl:when test="$length = 1">short</xsl:when>
              <xsl:when test="$length = 2">medium</xsl:when>
              <xsl:when test="$length = 3">long</xsl:when>
            </xsl:choose>
          </xsl:attribute>
          <xsl:attribute name="type">
            <!-- changed on 04-02-2009 -->
            <!-- changed on 06-04-2008 -->
            <xsl:if test="substring(text(),1,1)='°'">in</xsl:if>
            <xsl:if test="substring(text(),string-length(),1)='°'">out</xsl:if>
          </xsl:attribute>
        </desc>
      </vocal>
    </xsl:template>

    <!-- removed on 06-03-2009 -->
    <!-- <xsl:template match="GAT_UNINTELLIGIBLE">
        <xsl:element name="unintelligible">
            <xsl:attribute name="length">
                <xsl:value-of select="string-length() div 3"/>
            </xsl:attribute>
        </xsl:element>
    </xsl:template> -->
    
    <xsl:template match="GAT_UNCERTAIN">
        <unclear>
            <xsl:apply-templates select="GAT_ALTERNATIVE | GAT_WORD"/>
        </unclear>
    </xsl:template>
    
    <!-- TODO: alternative segmentation? -->
    <xsl:template match="GAT_ALTERNATIVE">
        <choice>
            <xsl:apply-templates select="GAT_WORD"/>
        </choice>
    </xsl:template>
    
    <xsl:template match="GAT_WORDBOUNDARY">
        <xsl:apply-templates select="time"/>
    </xsl:template>
    
    <xsl:template match="GAT_WORDBOUNDARY/text()">
        <!-- do nothing -->
    </xsl:template>
    
</xsl:stylesheet>
