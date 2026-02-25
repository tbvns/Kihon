package xyz.tbvns.kihon.Formats;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import xyz.tbvns.kihon.logic.ProgressManager;
import xyz.tbvns.kihon.Constants;
import xyz.tbvns.kihon.logic.Sorter;

import java.io.*;
import java.util.List;

public class PdfUtils {

    private static final String TEMP_DIR = "kihon_pdf_temp";

    public static DocumentFile createPdfFromPngs(Context context, List<DocumentFile> pngFiles, String pdfName) {
        ProgressManager pm = ProgressManager.getInstance(context);
        pm.startProgress(context, "Generating PDF", "Preparing pages...");

        DocumentFile result = createPdfInternal(context, pngFiles, pdfName, true, null);

        pm.updateProgress(100);
        pm.updateMessage("PDF created successfully!");
        pm.setCurrentTask("Complete");

        try { Thread.sleep(1500); } catch (InterruptedException e) {}
        pm.finishProgress();

        return result;
    }

    public static DocumentFile createPdfFromPngsNoInit(Context context, List<DocumentFile> pngFiles,
                                                       String pdfName, List<Integer> chapterBoundaries) {
        return createPdfInternal(context, pngFiles, pdfName, false, chapterBoundaries);
    }

    private static DocumentFile createPdfInternal(Context context, List<DocumentFile> pngFiles,
                                                  String pdfName, boolean initProgress,
                                                  List<Integer> chapterBoundaries) {
        ProgressManager pm = ProgressManager.getInstance(context);
        MemoryUsageSetting memSettings = MemoryUsageSetting.setupTempFileOnly();
        PDDocument document = new PDDocument(memSettings);
        PDRectangle pageSize = PDRectangle.LETTER;

        List<DocumentFile> sortedFiles = pngFiles;
        if (chapterBoundaries != null && !chapterBoundaries.isEmpty()) {
            sortedFiles = Sorter.sortByChapterThenByMajorMinor(pngFiles, chapterBoundaries);
        }

        int max = sortedFiles.size();
        if (initProgress) {
            pm.setItemsCount(0, max);
        }

        File tempDir = new File(context.getCacheDir(), TEMP_DIR);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        try {
            for (int index = 0; index < sortedFiles.size(); index++) {
                DocumentFile pngFile = sortedFiles.get(index);

                pm.updateMessage("Processing: " + pngFile.getName());
                pm.setCurrentTask("Page " + (index + 1) + " of " + max);
                pm.setItemsCount(index + 1, max);

                InputStream imageStream = context.getContentResolver().openInputStream(pngFile.getUri());
                if (imageStream == null) {
                    pm.updateMessage("Skipping: " + pngFile.getName() + " (cannot read)");
                    continue;
                }

                File tempImageFile = new File(tempDir, "temp_" + index + ".png");
                try (OutputStream tempOut = new FileOutputStream(tempImageFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = imageStream.read(buffer)) != -1) {
                        tempOut.write(buffer, 0, bytesRead);
                    }
                }
                imageStream.close();

                PDImageXObject pdImage = PDImageXObject.createFromFile(
                        tempImageFile.getAbsolutePath(), document);

                PDPage page = new PDPage(pageSize);
                document.addPage(page);

                float imageWidth = pdImage.getWidth();
                float imageHeight = pdImage.getHeight();
                float scale = Math.min(pageSize.getWidth() / imageWidth, pageSize.getHeight() / imageHeight);
                float drawWidth = imageWidth * scale;
                float drawHeight = imageHeight * scale;
                float posX = (pageSize.getWidth() - drawWidth) / 2;
                float posY = (pageSize.getHeight() - drawHeight) / 2;

                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                contentStream.drawImage(pdImage, posX, posY, drawWidth, drawHeight);
                contentStream.close();

                tempImageFile.delete();

                if (initProgress) {
                    int progress = (int) ((float) (index + 1) / max * 100);
                    pm.updateProgress(progress);
                } else {
                    int progress = 60 + (int) ((float) (index + 1) / max * 40);
                    pm.updateProgress(progress);
                }
            }

            pm.updateProgress(initProgress ? 95 : 95);
            pm.updateMessage("Creating PDF file...");
            pm.setCurrentTask("Saving PDF");

            DocumentFile renderedFolder = Constants.ExtractedFile.findFile("rendered");
            if (renderedFolder == null) {
                renderedFolder = Constants.ExtractedFile.createDirectory("rendered");
            }
            if (renderedFolder == null) {
                throw new IOException("Could not create or find the rendered folder");
            }

            DocumentFile pdfFile = renderedFolder.createFile("application/pdf", pdfName + ".pdf");
            if (pdfFile == null) {
                throw new IOException("Could not create the PDF file");
            }

            OutputStream outputStream = context.getContentResolver().openOutputStream(pdfFile.getUri());
            if (outputStream == null) {
                throw new IOException("Could not open output stream for PDF file");
            }

            pm.updateMessage("Writing PDF data...");
            pm.setCurrentTask("Compressing file");
            pm.updateProgress(initProgress ? 98 : 98);

            document.save(outputStream);
            outputStream.close();
            document.close();

            if (initProgress) {
                pm.updateProgress(100);
                pm.updateMessage("PDF created successfully!");
                pm.setCurrentTask("Complete");
            } else {
                pm.updateProgress(100);
            }

            return pdfFile;

        } catch (IOException e) {
            e.printStackTrace();
            pm.updateMessage("Error: " + e.getMessage());
            pm.setCurrentTask("Failed");

            if (initProgress) {
                try { Thread.sleep(2000); } catch (InterruptedException ex) { ex.printStackTrace(); }
            }

            try {
                document.close();
            } catch (IOException ex) {
                ex.printStackTrace();
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