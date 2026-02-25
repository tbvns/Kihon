package xyz.tbvns.kihon.logic;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;
import com.tom_roush.pdfbox.io.IOUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import xyz.tbvns.kihon.Settings;
import xyz.tbvns.kihon.logic.Object.ChapterObject;
import xyz.tbvns.kihon.logic.Object.RenderedFile;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

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

        ExecutorService executor = Executors.newFixedThreadPool(20);
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
                }
            } catch (Exception e) {
                Log.e("TAG", "Error getting result", e);
            }
        }

        executor.shutdown();
        chapters.sort(Comparator.comparingInt(c -> c.number));

        return chapters;
    }

    private static ChapterObject processChapter(DocumentFile file, String fileName) {
        try (ParcelFileDescriptor pfd = context.getContentResolver()
                .openFileDescriptor(file.getUri(), "r")) {

            if (pfd == null) {
                Log.e("TAG", "CBZ: null pfd for " + fileName);
                return null;
            }

            try (FileChannel channel = new FileInputStream(pfd.getFileDescriptor()).getChannel();
                 ZipFile zipFile = new ZipFile(channel)) {

                ZipArchiveEntry xmlEntry = zipFile.getEntry("ComicInfo.xml");
                if (xmlEntry == null) {
                    Log.w("TAG", "CBZ: no ComicInfo.xml in " + fileName);
                    return null;
                }

                try (InputStream xmlStream = zipFile.getInputStream(xmlEntry)) {
                    byte[] xmlBytes = IOUtils.toByteArray(xmlStream);
                    return ChapterObject.fromString(new String(xmlBytes, StandardCharsets.UTF_8), file);
                }
            }

        } catch (Exception e) {
            Log.e("TAG", "CBZ: error reading " + fileName, e);
        }
        return null;
    }

    public static ArrayList<RenderedFile> listRendered() {
        if (context == null || Settings.mihonPath == null || Settings.mihonPath.isEmpty())
            return new ArrayList<>();

        DocumentFile root = DocumentFile.fromTreeUri(context, Uri.parse(Settings.mihonPath));
        if (root == null) return new ArrayList<>();

        DocumentFile extracted = root.findFile("extracted");
        if (extracted == null || !extracted.isDirectory()) return new ArrayList<>();

        DocumentFile rendered = extracted.findFile("rendered");
        if (rendered == null || !rendered.isDirectory()) return new ArrayList<>();

        ArrayList<RenderedFile> results = new ArrayList<>();

        for (DocumentFile file : rendered.listFiles()) {
            if (file.isDirectory()) continue;

            String fileName = file.getName();
            if (fileName == null) continue;

            String lowerName = fileName.toLowerCase();
            String extension = null;

            if (lowerName.endsWith(".pdf")) {
                extension = "pdf";
            } else if (lowerName.endsWith(".epub")) {
                extension = "epub";
            }

            if (extension == null) continue;
            String displayName = fileName.substring(0, fileName.lastIndexOf('.'));
            long sizeBytes = file.length();
            long lastModified = file.lastModified();

            results.add(new RenderedFile(displayName, extension, sizeBytes, lastModified, file));
        }

        results.sort(Comparator.comparing(f -> f.name));
        return results;
    }

    public static void cleanupExtractedFolder() {
        cleanupExtractedFolder(null);
    }

    public static void cleanupExtractedFolder(ProgressManager progressManager) {
        if (context == null || Settings.mihonPath == null || Settings.mihonPath.isEmpty()) {
            Log.w("TAG", "Cleanup: context or path not initialized");
            return;
        }

        try {
            DocumentFile root = DocumentFile.fromTreeUri(context, Uri.parse(Settings.mihonPath));
            if (root == null) {
                Log.e("TAG", "Cleanup: root directory is null");
                return;
            }

            DocumentFile extracted = root.findFile("extracted");
            if (extracted == null || !extracted.isDirectory()) {
                Log.w("TAG", "Cleanup: extracted directory not found");
                return;
            }

            DocumentFile[] files = extracted.listFiles();
            if (files == null || files.length == 0) {
                return;
            }

            List<DocumentFile> foldersToDelete = new ArrayList<>();
            for (DocumentFile file : files) {
                if (!file.isDirectory()) continue;

                String folderName = file.getName();
                if (folderName == null || folderName.equals("rendered")) continue;

                foldersToDelete.add(file);
            }

            if (foldersToDelete.isEmpty()) {
                return;
            }

            if (progressManager != null) {
                progressManager.setItemsCount(0, foldersToDelete.size());
            }

            int totalFolders = foldersToDelete.size();
            AtomicInteger completedFolders = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(4);
            List<Future<?>> deletionFutures = new ArrayList<>();

            for (DocumentFile folder : foldersToDelete) {
                deletionFutures.add(executor.submit(() -> {
                    String folderName = folder.getName();
                    try {
                        deleteDirectoryRecursive(folder);
                        Log.d("TAG", "Cleanup: deleted folder '" + folderName + "'");
                    } catch (Exception e) {
                        Log.e("TAG", "Cleanup: failed to delete folder '" + folderName + "'", e);
                    }

                    int completed = completedFolders.incrementAndGet();

                    if (progressManager != null) {
                        progressManager.setItemsCount(completed, totalFolders);
                    }
                }));
            }

            for (Future<?> future : deletionFutures) {
                try {
                    future.get();
                } catch (Exception e) {
                    Log.e("TAG", "Cleanup: error during parallel deletion", e);
                }
            }

            executor.shutdown();

            Log.i("TAG", "Cleanup: removed " + completedFolders.get() + " folders from extracted directory");

        } catch (Exception e) {
            Log.e("TAG", "Cleanup: error during cleanup", e);
        }
    }

    private static void deleteDirectoryRecursive(DocumentFile dir) {
        if (!dir.isDirectory()) {
            dir.delete();
            return;
        }

        DocumentFile[] files = dir.listFiles();
        if (files != null) {
            for (DocumentFile file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursive(file);
                } else {
                    file.delete();
                }
            }
        }

        dir.delete();
    }
}