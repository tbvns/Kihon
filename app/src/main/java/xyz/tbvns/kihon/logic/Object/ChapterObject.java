package xyz.tbvns.kihon.logic.Object;

import androidx.documentfile.provider.DocumentFile;
import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.codehaus.stax2.XMLInputFactory2;

import javax.annotation.Nullable;

@JacksonXmlRootElement(localName = "ComicInfo")
public class ChapterObject {
    @JsonProperty("Title")
    public String title;

    @JsonProperty("Series")
    public String series;

    @JsonProperty("Number")
    public int number;

    @JsonProperty("Summary")
    public String summary;

    @JsonProperty("Writer")
    @Nullable
    public String writer;

    @JsonProperty("Penciller")
    @Nullable
    public String penciller;

    @JsonProperty("Genre")
    @Nullable
    public String genre;

    @JsonProperty("Translator")
    @Nullable
    public String translator;

    @JsonProperty("Web")
    @Nullable
    public String web;

    @JacksonXmlProperty(localName = "PublishingStatusTachiyomi", namespace = "http://www.w3.org/2001/XMLSchema")
    @Nullable
    public String publishingStatus;

    @JacksonXmlProperty(localName = "Categories", namespace = "http://www.w3.org/2001/XMLSchema")
    @Nullable
    public String categories;

    @JacksonXmlProperty(localName = "SourceMihon", namespace = "http://www.w3.org/2001/XMLSchema")
    @Nullable
    public String sourceMihon;

    @JsonIgnore
    public DocumentFile file;

    public static ChapterObject fromString(String xml, DocumentFile file) throws JsonProcessingException {
        ObjectMapper xmlMapper = new XmlMapper();
        ChapterObject obj = xmlMapper.readValue(xml, ChapterObject.class);
        obj.file = file;
        return obj;
    }
}