<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns="http://www.tei-c.org/ns/1.0" xmlns:tei="http://www.tei-c.org/ns/1.0"  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <!-- changes on 04-02-2009: -->
    <!-- symbol for clitics is now _ instead of + -->
    <!-- symbol for alternative is now / instaed of | -->
    <!-- symbol for breathe is now ° instead of _ -->
    <!-- symbol for unintelligible is now + instead of * -->
    <!-- changes on 06-03-2009: -->
    <!-- removed template for UNINTELLIGIBLE -->

    <xsl:template match="contribution">
        <xsl:element name="contribution">
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
            <xsl:attribute name="type">
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
        <xsl:element name="incident">
            <xsl:element name="desc">
                <xsl:value-of select="substring-before(substring-after(text(),'(('), '))')"/>
            </xsl:element>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="tei:anchor">
        <xsl:copy>
            <xsl:copy-of select="@*|*"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="GAT_BREATHE">
        <xsl:element name="incident">
            <xsl:attribute name="length">
                <xsl:value-of select="string-length()-1"/>
            </xsl:attribute>
            <xsl:element name="desc">
              breathe
                <!-- changed on 04-02-2009 -->
                <!-- changed on 06-04-2008 -->
                <xsl:if test="substring(text(),1,1)='°'">in</xsl:if>
                <xsl:if test="substring(text(),string-length(),1)='°'">out</xsl:if>
            </xsl:element>
        </xsl:element>
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
        <uncertain>
            <xsl:apply-templates select="GAT_ALTERNATIVE | GAT_WORD"/>
        </uncertain>
    </xsl:template>
    
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

    <!-- TODO: what? -->
    <xsl:template match="GAT_COMMENT_START_ESCAPED">
        <xsl:element name="comment">
            <xsl:attribute name="position">start</xsl:attribute>
            <xsl:attribute name="description">
                <xsl:value-of select="substring(text(),3, string-length()-3)"/>
            </xsl:attribute>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="GAT_COMMENT_END_ESCAPED">
        <xsl:element name="comment">
            <xsl:attribute name="position">end</xsl:attribute>
        </xsl:element>
    </xsl:template>

    <xsl:template match="GAT_PHRASE_BOUNDARY | GAT_PSEUDO_PHRASE_BOUNDARY">
        <xsl:element name="boundary">
            <xsl:attribute name="type">
                <xsl:choose>
                    <xsl:when test="self::GAT_PHRASE_BOUNDARY">final</xsl:when>
                    <xsl:otherwise>pseudo</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:attribute name="movement">
                <xsl:choose>
                    <xsl:when test="contains(text(),'.')">low-fall</xsl:when>
                    <xsl:when test="contains(text(),';')">fall</xsl:when>
                    <xsl:when test="contains(text(),'–')">steady</xsl:when>
                    <xsl:when test="contains(text(),',')">rise</xsl:when>
                    <xsl:when test="contains(text(),'?')">high-rise</xsl:when>
                    <!-- added 24-08-2018 for issue #162: the asterisk -->
                    <xsl:when test="contains(text(),'*')">other</xsl:when>
                    <xsl:otherwise>not-qualified</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:attribute name="latching">
                <xsl:choose>
                    <xsl:when test="contains(text(),'=')">yes</xsl:when>
                    <xsl:otherwise>no</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
        </xsl:element>
        <xsl:apply-templates select="time"/>
    </xsl:template>
    
    <xsl:template match="GAT_STRONG_ACCENT_SYLLABLE | GAT_ACCENT_SYLLABLE">
        <xsl:element name="seg">
            <xsl:attribute name="type">
                <xsl:choose>
                    <xsl:when test="self::GAT_ACCENT_SYLLABLE">accentuated normal</xsl:when>
                    <xsl:otherwise>accentuated strong</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>
    
    <!-- TODO: changed to function for non-German -->
    <xsl:template match="GAT_STRONG_ACCENT_SYLLABLE/text()">
        <!--xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ!'" />
        <xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyzäöü'" /-->
        <xsl:value-of select="lower-case(.)"/>
    </xsl:template>
    
    <!-- TODO: changed to function for non-German -->
    <xsl:template match="GAT_ACCENT_SYLLABLE/text()">
        <!--xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ'" />
        <xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyzäöü'" /-->
        <xsl:value-of select="lower-case(.)"/>
    </xsl:template>
    
    <xsl:template match="GAT_LENGTHENING">
      <xsl:element name="seg">
        <xsl:attribute name="type">lengthening</xsl:attribute>
            <xsl:attribute name="degree">
                <xsl:value-of select="string-length()"/>
            </xsl:attribute>            
        </xsl:element>
    </xsl:template>

    <!-- TODO: what? -->
    <xsl:template match="GAT_LATCHING">
      <vocal>
        <desc>latching</desc>
      </vocal>
    </xsl:template>
    

</xsl:stylesheet>
