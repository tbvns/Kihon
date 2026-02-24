package xyz.tbvns.kihon.logic;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;
import xyz.tbvns.kihon.Settings;
import java.util.ArrayList;
import java.util.Arrays;

public class FilesLogic {
    private static Context context;

    public static void init(Context ctx) {
        context = ctx;
    }

    public static ArrayList<String> listSources() {
        if (context == null || Settings.mihonPath == null || Settings.mihonPath.isEmpty())
            return new ArrayList<>();

        DocumentFile dir = DocumentFile.fromTreeUri(context, Uri.parse(Settings.mihonPath));
        dir = dir != null ? dir.findFile("downloads") : null;
        return dir != null && dir.isDirectory() ?
                new ArrayList<>(Arrays.asList(Arrays.stream(dir.listFiles())
                        .filter(DocumentFile::isDirectory)
                        .map(DocumentFile::getName)
                        .toArray(String[]::new))) :
                new ArrayList<>();
    }
}