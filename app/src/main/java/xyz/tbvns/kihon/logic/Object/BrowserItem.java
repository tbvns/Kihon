package xyz.tbvns.kihon.logic.Object;

import lombok.Data;

@Data
public class BrowserItem {
    public static final int TYPE_FOLDER = 0;
    public static final int TYPE_CHAPTER = 1;
    
    public final int type;
    public final String folderName;
    public final ChapterObject chapter;

    public BrowserItem(String folderName) {
        this.type = TYPE_FOLDER;
        this.folderName = folderName;
        this.chapter = null;
    }
    
    public BrowserItem(ChapterObject chapter) {
        this.type = TYPE_CHAPTER;
        this.chapter = chapter;
        this.folderName = null;
    }
    
    public String getDisplayName() {
        return type == TYPE_FOLDER ? folderName : 
               (chapter.title != null ? chapter.title : String.valueOf(chapter.number));
    }

}