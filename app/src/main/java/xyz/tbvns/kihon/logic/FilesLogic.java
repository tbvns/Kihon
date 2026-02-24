package xyz.tbvns.kihon.logic;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;
import com.tom_roush.pdfbox.io.IOUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import xyz.tbvns.kihon.Settings;
import xyz.tbvns.kihon.logic.Object.ChapterObject;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    public static ArrayList<String> listFolder(String targetName) {
        if (context == null || Settings.mihonPath == null || Settings.mihonPath.isEmpty())
            return new ArrayList<>();

        DocumentFile root = DocumentFile.fromTreeUri(context, Uri.parse(Settings.mihonPath));
        DocumentFile downloads = root != null ? root.findFile("downloads") : null;

        // Find the actual folder we want to look inside
        DocumentFile targetDir = findFolderRecursively(downloads, targetName);

        if (targetDir != null && targetDir.isDirectory()) {
            return new ArrayList<>(Arrays.asList(Arrays.stream(targetDir.listFiles())
                    .filter(DocumentFile::isDirectory)
                    .map(DocumentFile::getName)
                    .toArray(String[]::new)));
        }
        return new ArrayList<>();
    }

    private static DocumentFile findFolderRecursively(DocumentFile parent, String name) {
        if (parent == null) return null;
        if (name.equals(parent.getName())) return parent;

        DocumentFile directChild = parent.findFile(name);
        if (directChild != null) return directChild;

        for (DocumentFile file : parent.listFiles()) {
            if (file.isDirectory()) {
                DocumentFile found = findFolderRecursively(file, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static ArrayList<ChapterObject> listChapters(String sourceName, String mangaName) {
        if (context == null || Settings.mihonPath == null || Settings.mihonPath.isEmpty())
            return new ArrayList<>();

        DocumentFile baseDir = DocumentFile.fromTreeUri(context, Uri.parse(Settings.mihonPath));
        if (baseDir == null) return new ArrayList<>();

        DocumentFile downloadsDir = baseDir.findFile("downloads");
        if (downloadsDir == null) return new ArrayList<>();

        DocumentFile sourceDir = downloadsDir.findFile(sourceName);
        if (sourceDir == null) return new ArrayList<>();

        DocumentFile mangaDir = sourceDir.findFile(mangaName);
        if (mangaDir == null || !mangaDir.isDirectory()) return new ArrayList<>();

        ArrayList<ChapterObject> chapters = new ArrayList<>();
        DocumentFile[] files = mangaDir.listFiles();

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<ChapterObject>> futures = new ArrayList<>();

        for (DocumentFile file : files) {
            if (file.isDirectory()) continue;

            String fileName = file.getName();
            if (fileName == null || !fileName.toLowerCase().endsWith(".cbz")) continue;

            futures.add(executor.submit(() -> processChapter(file, fileName)));
        }

        for (Future<ChapterObject> future : futures) {
            try {
                ChapterObject result = future.get();
                if (result != null) {
                    chapters.add(result);
                    Log.i("TAG", "Found XML in: " + result);
                }
            } catch (Exception e) {
                Log.e("TAG", "Error getting result", e);
            }
        }

        executor.shutdown();
        return chapters;
    }

    private static ChapterObject processChapter(DocumentFile file, String fileName) {
        try {
            File tempFile = new File(context.getCacheDir(), fileName);

            try (InputStream input = context.getContentResolver().openInputStream(file.getUri());
                 FileOutputStream output = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
            }

            ZipFile zipFile = new ZipFile(tempFile);
            ZipArchiveEntry xmlEntry = zipFile.getEntry("ComicInfo.xml");

            ChapterObject chapterObject = null;

            if (xmlEntry != null) {
                try (InputStream xmlStream = zipFile.getInputStream(xmlEntry)) {
                    byte[] xmlBuffer = new byte[(int) xmlEntry.getSize()];
                    xmlStream.read(xmlBuffer);
                    chapterObject = ChapterObject.fromString(new String(xmlBuffer));
                }
            }

            zipFile.close();
            tempFile.delete();

            return chapterObject;

        } catch (Exception e) {
            Log.e("TAG", "Error reading CBZ: " + fileName, e);
            return null;
        }
    }
}