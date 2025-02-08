package xyz.tbvns.kihon.Formats;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import xyz.tbvns.kihon.Constant;
import xyz.tbvns.kihon.fragments.LoadingFragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

public class PdfUtils {

    public static DocumentFile createPdfFromPngs(Context context, List<DocumentFile> pngFiles, String pdfName) {
        PDDocument document = new PDDocument();
        // Use a standard page size, e.g., LETTER (you can change this if needed)
        PDRectangle pageSize = PDRectangle.LETTER;

        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, "Generating PDF...", Toast.LENGTH_LONG).show();
        });

        int max = pngFiles.size();
        float percent = 0;

        try {
            for (DocumentFile pngFile : pngFiles) {
                percent += (float) 1 / max * 100;
                LoadingFragment.progress = 50+(percent/2);
                LoadingFragment.message = "Generating PDF: " + pngFile.getParentFile().getName() + " - " + pngFile.getName();

                // Open the PNG image as bytes
                InputStream imageStream = context.getContentResolver().openInputStream(pngFile.getUri());
                if (imageStream == null) continue;
                byte[] imageBytes = toByteArray(imageStream);
                imageStream.close();

                // Create a PDImageXObject from the image bytes
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes, pngFile.getName());

                // Create a new page for the image
                PDPage page = new PDPage(pageSize);
                document.addPage(page);

                // Calculate scaling so that the image fills the page as much as possible while keeping its aspect ratio.
                float imageWidth = pdImage.getWidth();
                float imageHeight = pdImage.getHeight();
                float scale = Math.min(pageSize.getWidth() / imageWidth, pageSize.getHeight() / imageHeight);
                float drawWidth = imageWidth * scale;
                float drawHeight = imageHeight * scale;
                // Center the image on the page
                float posX = (pageSize.getWidth() - drawWidth) / 2;
                float posY = (pageSize.getHeight() - drawHeight) / 2;

                // Draw the image on the page
                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                contentStream.drawImage(pdImage, posX, posY, drawWidth, drawHeight);
                contentStream.close();
            }

            // Create (or get) the 'rendered' folder inside Constant.ExtractedFile
            DocumentFile renderedFolder = Constant.ExtractedFile.findFile("rendered");
            if (renderedFolder == null) {
                renderedFolder = Constant.ExtractedFile.createDirectory("rendered");
            }
            if (renderedFolder == null) {
                throw new IOException("Could not create or find the rendered folder");
            }

            // Create the PDF file (appending .pdf extension)
            DocumentFile pdfFile = renderedFolder.createFile("application/pdf", pdfName + ".pdf");
            if (pdfFile == null) {
                throw new IOException("Could not create the PDF file");
            }

            // Open an OutputStream to the PDF file and save the document
            OutputStream outputStream = context.getContentResolver().openOutputStream(pdfFile.getUri());
            if (outputStream == null) {
                throw new IOException("Could not open output stream for PDF file");
            }
            document.save(outputStream);
            outputStream.close();
            document.close();
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, String.format(Locale.getDefault(), "PDF created: %s", pdfFile.getUri().toString()), Toast.LENGTH_LONG).show();
            });
            return pdfFile;

        } catch (IOException e) {
            e.printStackTrace();
            try {
                document.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "Error creating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
        return null;
    }

    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
