//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.05.20 at 09:46:06 PM KRAT 
//


package com.github._1c_syntax.mdclasses.jabx.original;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DimensionPlacementType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="DimensionPlacementType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Together"/&gt;
 *     &lt;enumeration value="Separately"/&gt;
 *     &lt;enumeration value="SeparatelyAndInTotalsOnly"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "DimensionPlacementType", namespace = "http://v8.1c.ru/8.2/data/spreadsheet")
@XmlEnum
public enum DimensionPlacementType {

    @XmlEnumValue("Together")
    TOGETHER("Together"),
    @XmlEnumValue("Separately")
    SEPARATELY("Separately"),
    @XmlEnumValue("SeparatelyAndInTotalsOnly")
    SEPARATELY_AND_IN_TOTALS_ONLY("SeparatelyAndInTotalsOnly");
    private final String value;

    DimensionPlacementType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DimensionPlacementType fromValue(String v) {
        for (DimensionPlacementType c: DimensionPlacementType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}