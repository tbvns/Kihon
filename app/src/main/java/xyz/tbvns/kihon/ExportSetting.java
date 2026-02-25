package xyz.tbvns.kihon;

import androidx.documentfile.provider.DocumentFile;
import xyz.tbvns.Config;

public class ExportSetting implements Config {
    public static boolean REENCODE_IMAGES = false;
    public static int IMAGE_QUALITY = 100;
    public static boolean RESIZE_IMAGES = false;
    public static int IMAGE_SIZE = 100;
    public static boolean GRAYSCALE = false;

    public static float secondaryActionImpact = 0.5F;

    public static boolean USE_COVER = true;
    public static boolean COVER_USE_PAGE = true;
    public static boolean COVER_USE_CUSTOM = false;
    public static int COVER_PAGE_INDEX = 0;
    public static String COVER_CUSTOM_BITMAP_PATH = "";
}
