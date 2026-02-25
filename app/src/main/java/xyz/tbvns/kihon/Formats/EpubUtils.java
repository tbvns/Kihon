package xyz.tbvns.kihon.Formats;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.documentfile.provider.DocumentFile;
import xyz.tbvns.kihon.logic.ProgressManager;
import xyz.tbvns.kihon.ExportSetting;
import xyz.tbvns.kihon.Constants;
import xyz.tbvns.kihon.logic.Sorter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class EpubUtils {

    private static final String TEMP_DIR = "kihon_epub_temp";

    /**
     * Generate EPUB and show progress in its own activity.
     * Use this when calling standalone.
     */
    public static DocumentFile generateEpub(Context context, List<DocumentFile> pngFiles,
                                            String epubName) {
        ProgressManager pm = ProgressManager.getInstance();
        pm.startProgress(context, "Generating EPUB", "Preparing files...");

        DocumentFile result = generateEpubInternal(context, pngFiles, epubName, true, null);

        pm.updateProgress(100);
        pm.updateMessage("EPUB created successfully!");
        pm.setCurrentTask("Complete");

        try { Thread.sleep(1500); } catch (InterruptedException e) {}
        pm.finishProgress();

        return result;
    }

    /**
     * Generate EPUB WITHOUT starting/finishing progress activity.
     * Use this when called from within an existing export flow (ExportOptionsActivity).
     * Progress should be managed by the caller (60-100% range).
     *
     * @param chapterBoundaries List of indices where chapters begin (e.g., [0, 15, 32])
     *                          If null, all files are treated as one sequence
     */
    public static DocumentFile generateEpubNoInit(Context context, List<DocumentFile> pngFiles,
                                                  String epubName, List<Integer> chapterBoundaries) {
        return generateEpubInternal(context, pngFiles, epubName, false, chapterBoundaries);
    }

    /**
     * Internal EPUB generation with streaming and chapter-aware sorting.
     * @param initProgress true if this started the progress (show completion), false otherwise
     * @param chapterBoundaries chapter break indices; if provided, files are sorted within each chapter
     */
    private static DocumentFile generateEpubInternal(Context context, List<DocumentFile> pngFiles,
                                                     String epubName, boolean initProgress,
                                                     List<Integer> chapterBoundaries) {
        ProgressManager pm = ProgressManager.getInstance();

        List<DocumentFile> sortedFiles = pngFiles;
        if (chapterBoundaries != null && !chapterBoundaries.isEmpty()) {
            sortedFiles = Sorter.sortByChapterThenByMajorMinor(pngFiles, chapterBoundaries);
        }

        File tempDir = new File(context.getCacheDir(), TEMP_DIR);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File tempEpubFile = new File(tempDir, "temp.epub");

        try {
            FileOutputStream fos = new FileOutputStream(tempEpubFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            if (initProgress) {
                pm.updateMessage("Writing EPUB metadata...");
                pm.setCurrentTask("Creating MIME type");
            }

            byte[] mimetypeBytes = "application/epub+zip".getBytes(StandardCharsets.US_ASCII);
            ZipEntry mimetypeEntry = new ZipEntry("mimetype");
            mimetypeEntry.setMethod(ZipEntry.STORED);
            mimetypeEntry.setSize(mimetypeBytes.length);
            mimetypeEntry.setCompressedSize(mimetypeBytes.length);
            CRC32 crc = new CRC32();
            crc.update(mimetypeBytes);
            mimetypeEntry.setCrc(crc.getValue());
            zos.putNextEntry(mimetypeEntry);
            zos.write(mimetypeBytes);
            zos.closeEntry();

            pm.updateProgress(initProgress ? 5 : 60);

            if (initProgress) {
                pm.setCurrentTask("Creating container");
            }
            ZipEntry containerEntry = new ZipEntry("META-INF/container.xml");
            zos.putNextEntry(containerEntry);
            String containerXml =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n" +
                            "   <rootfiles>\n" +
                            "      <rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n" +
                            "   </rootfiles>\n" +
                            "</container>";
            zos.write(containerXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            pm.updateProgress(initProgress ? 10 : 62);

            if (initProgress) {
                pm.updateMessage("Building index...");
                pm.setCurrentTask("Creating index XHTML");
            }

            StringBuilder indexXhtml = new StringBuilder();
            indexXhtml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                    .append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" ")
                    .append("\"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n")
                    .append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n")
                    .append("<head>\n")
                    .append("<title>").append(epubName).append("</title>\n")
                    .append("</head>\n")
                    .append("<body>\n");

            int max = sortedFiles.size();
            for (int i = 0; i < sortedFiles.size(); i++) {
                DocumentFile pngFile = sortedFiles.get(i);
                indexXhtml.append("<img src=\"images/image").append(i)
                        .append(".png\" alt=\"").append(pngFile.getName())
                        .append("\"/><br/>\n");
            }
            indexXhtml.append("</body>\n")
                    .append("</html>");

            ZipEntry indexEntry = new ZipEntry("OEBPS/index.xhtml");
            zos.putNextEntry(indexEntry);
            zos.write(indexXhtml.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            pm.updateProgress(initProgress ? 15 : 64);

            StringBuilder manifestItems = new StringBuilder();
            StringBuilder metadataItems = new StringBuilder();

            String coverImageId = null;
            boolean hasCover = false;

            if (ExportSetting.USE_COVER) {
                if (ExportSetting.COVER_USE_PAGE &&
                        ExportSetting.COVER_PAGE_INDEX < sortedFiles.size()) {
                    if (initProgress) {
                        pm.setCurrentTask("Setting cover image");
                    }
                    coverImageId = "cover-image";
                    int coverPageIndex = ExportSetting.COVER_PAGE_INDEX;
                    manifestItems.append("<item id=\"").append(coverImageId)
                            .append("\" href=\"images/image").append(coverPageIndex)
                            .append(".png\" media-type=\"image/png\" ")
                            .append("properties=\"cover-image\"/>\n");
                    metadataItems.append("<meta name=\"cover\" content=\"")
                            .append(coverImageId).append("\"/>\n");
                    hasCover = true;

                } else if (ExportSetting.COVER_USE_CUSTOM &&
                        ExportSetting.COVER_CUSTOM_BITMAP_PATH != null) {
                    if (initProgress) {
                        pm.setCurrentTask("Adding custom cover");
                    }
                    coverImageId = "cover-image";
                    manifestItems.append("<item id=\"").append(coverImageId)
                            .append("\" href=\"images/cover.png\" ")
                            .append("media-type=\"image/png\" ")
                            .append("properties=\"cover-image\"/>\n");
                    metadataItems.append("<meta name=\"cover\" content=\"")
                            .append(coverImageId).append("\"/>\n");
                    hasCover = true;
                }
            }

            if (initProgress) {
                pm.setCurrentTask("Creating content package");
            }
            StringBuilder contentOpf = new StringBuilder();
            contentOpf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                    .append("<package xmlns=\"http://www.idpf.org/2007/opf\" ")
                    .append("unique-identifier=\"BookId\" version=\"2.0\">\n")
                    .append("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n")
                    .append("<dc:title>").append(epubName).append("</dc:title>\n")
                    .append("<dc:language>en</dc:language>\n")
                    .append("<dc:identifier id=\"BookId\">urn:uuid:")
                    .append(UUID.randomUUID().toString()).append("</dc:identifier>\n")
                    .append(metadataItems.toString())
                    .append("</metadata>\n")
                    .append("<manifest>\n")
                    .append("<item id=\"index\" href=\"index.xhtml\" ")
                    .append("media-type=\"application/xhtml+xml\"/>\n")
                    .append(manifestItems);

            for (int i = 0; i < sortedFiles.size(); i++) {
                contentOpf.append("<item id=\"img").append(i)
                        .append("\" href=\"images/image").append(i)
                        .append(".png\" media-type=\"image/png\"/>\n");
            }

            contentOpf.append("</manifest>\n")
                    .append("<spine toc=\"ncx\">\n")
                    .append("<itemref idref=\"index\"/>\n")
                    .append("</spine>\n")
                    .append("</package>\n");

            ZipEntry opfEntry = new ZipEntry("OEBPS/content.opf");
            zos.putNextEntry(opfEntry);
            zos.write(contentOpf.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            pm.updateProgress(initProgress ? 20 : 65);

            // Write custom cover image if used
            if (hasCover && ExportSetting.COVER_USE_CUSTOM &&
                    !ExportSetting.COVER_CUSTOM_BITMAP_PATH.isEmpty()) {
                if (initProgress) {
                    pm.updateMessage("Writing cover image...");
                    pm.setCurrentTask("Processing cover");
                }

                Bitmap coverBitmap = BitmapFactory.decodeFile(
                        ExportSetting.COVER_CUSTOM_BITMAP_PATH);

                if (coverBitmap != null) {
                    ZipEntry coverEntry = new ZipEntry("OEBPS/images/cover.png");
                    zos.putNextEntry(coverEntry);
                    coverBitmap.compress(Bitmap.CompressFormat.PNG, 100, zos);
                    zos.closeEntry();
                    coverBitmap.recycle();
                }
                pm.updateProgress(initProgress ? 25 : 67);
            }

            // Write all page images - stream directly from source
            if (initProgress) {
                pm.updateMessage("Adding images to EPUB...");
            }
            for (int i = 0; i < sortedFiles.size(); i++) {
                DocumentFile pngFile = sortedFiles.get(i);
                pm.updateMessage("Adding image: " + pngFile.getName());
                pm.setCurrentTask("Image " + (i + 1) + " of " + max);
                pm.setItemsCount(i + 1, max);

                InputStream imageStream = context.getContentResolver()
                        .openInputStream(pngFile.getUri());
                if (imageStream == null) continue;

                ZipEntry imageEntry = new ZipEntry("OEBPS/images/image" + i + ".png");
                zos.putNextEntry(imageEntry);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = imageStream.read(buffer)) != -1) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                imageStream.close();

                // Update progress proportionally based on whether we initialized
                if (initProgress) {
                    int imageProgress = 25 + (int) ((float) (i + 1) / max * 70);
                    pm.updateProgress(imageProgress);
                } else {
                    int imageProgress = 67 + (int) ((float) (i + 1) / max * 33);
                    pm.updateProgress(imageProgress);
                }
            }

            pm.updateProgress(initProgress ? 95 : 98);
            if (initProgress) {
                pm.updateMessage("Finalizing EPUB...");
            }
            pm.setCurrentTask("Saving file");

            zos.close();
            fos.close();

            // Copy from temp file to final location
            DocumentFile renderedFolder = Constants.ExtractedFile.findFile("rendered");
            if (renderedFolder == null) {
                renderedFolder = Constants.ExtractedFile.createDirectory("rendered");
            }
            if (renderedFolder == null) {
                throw new IOException("Could not create or find the rendered folder");
            }

            DocumentFile epubFile = renderedFolder.createFile("application/epub+zip",
                    epubName + ".epub");
            if (epubFile == null) {
                throw new IOException("Could not create the EPUB file");
            }

            OutputStream outputStream = context.getContentResolver()
                    .openOutputStream(epubFile.getUri());
            if (outputStream == null) {
                throw new IOException("Could not open output stream for EPUB file");
            }

            // Stream temp file to final location
            try (FileInputStream tempInput = new FileInputStream(tempEpubFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = tempInput.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            outputStream.close();

            // Only show final completion if we initialized progress
            if (initProgress) {
                pm.updateProgress(100);
                pm.updateMessage("EPUB created successfully!");
                pm.setCurrentTask("Complete");
            } else {
                pm.updateProgress(100);
            }

            return epubFile;

        } catch (IOException e) {
            e.printStackTrace();
            pm.updateMessage("Error: " + e.getMessage());
            pm.setCurrentTask("Failed");

            if (initProgress) {
                try { Thread.sleep(2000); } catch (InterruptedException ex) {}
            }
        } finally {
            cleanupTempDirectory(tempDir);
        }

        return null;
    }

    private static void cleanupTempDirectory(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dir.delete();
        }
    }
}