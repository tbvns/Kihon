package xyz.tbvns.kihon.logic.Object;

import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.codehaus.stax2.XMLInputFactory2;

@JacksonXmlRootElement(localName = "ComicInfo")
public class ChapterObject {
    @JsonProperty("Title")
    public String title;

    @JsonProperty("Series")
    public String series;

    @JsonProperty("Number")
    public String number;

    @JsonProperty("Summary")
    public String summary;

    @JsonProperty("Writer")
    public String writer;

    @JsonProperty("Penciller")
    public String penciller;

    @JsonProperty("Genre")
    public String genre;

    @JsonProperty("Web")
    public String web;

    @JacksonXmlProperty(localName = "PublishingStatusTachiyomi", namespace = "http://www.w3.org/2001/XMLSchema")
    public String publishingStatus;

    @JacksonXmlProperty(localName = "Categories", namespace = "http://www.w3.org/2001/XMLSchema")
    public String categories;

    @JacksonXmlProperty(localName = "SourceMihon", namespace = "http://www.w3.org/2001/XMLSchema")
    public String sourceMihon;

    public static ChapterObject fromString(String xml) throws JsonProcessingException {
        ObjectMapper xmlMapper = new XmlMapper();
        return xmlMapper.readValue(xml, ChapterObject.class);
    }
}