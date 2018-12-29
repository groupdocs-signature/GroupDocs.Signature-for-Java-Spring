package com.groupdocs.ui.signature.model.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * TextXmlEntity
 *
 * @author Aspose Pty Ltd
 */
@XmlRootElement(name="TextXmlEntity", namespace="TextXmlEntity")
@XmlAccessorType(XmlAccessType.FIELD)
public class TextXmlEntity extends XmlEntity {
    private String encodedImage;

    private String backgroundColor = "rgb(255,255,255)";
    private String fontColor = "rgb(0,0,0)";
    private String font;
    private int fontSize;
    private Boolean bold;
    private Boolean italic;
    private Boolean underline;

    public String getEncodedImage() {
        return encodedImage;
    }

    public void setEncodedImage(String encodedImage) {
        this.encodedImage = encodedImage;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public String getFont() {
        return font;
    }

    public void setFont(String font) {
        this.font = font;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public Boolean getBold() {
        return bold;
    }

    public void setBold(Boolean bold) {
        this.bold = bold;
    }

    public Boolean getItalic() {
        return italic;
    }

    public void setItalic(Boolean italic) {
        this.italic = italic;
    }

    public Boolean getUnderline() {
        return underline;
    }

    public void setUnderline(Boolean underline) {
        this.underline = underline;
    }
}
