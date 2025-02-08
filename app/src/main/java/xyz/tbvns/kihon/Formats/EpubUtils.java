package xyz.tbvns.kihon.Formats;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.documentfile.provider.DocumentFile;
import xyz.tbvns.kihon.Constant;
import xyz.tbvns.kihon.fragments.LoadingFragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class EpubUtils {

    public static DocumentFile generateEpub(Context context, List<DocumentFile> pngFiles, String epubName) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, "Generating EPUB...", Toast.LENGTH_LONG).show()
        );

        try {
            // Create an in-memory output stream to build the EPUB (a ZIP file)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            // 1. Write the mimetype file (must be the first entry and uncompressed)
            byte[] mimetypeBytes = "application/epub+zip".getBytes(StandardCharsets.US_ASCII);
            ZipEntry mimetypeEntry = new ZipEntry("mimetype");
            // For a stored (uncompressed) entry you must set size, CRC, etc.
            mimetypeEntry.setMethod(ZipEntry.STORED);
            mimetypeEntry.setSize(mimetypeBytes.length);
            mimetypeEntry.setCompressedSize(mimetypeBytes.length);
            CRC32 crc = new CRC32();
            crc.update(mimetypeBytes);
            mimetypeEntry.setCrc(crc.getValue());
            zos.putNextEntry(mimetypeEntry);
            zos.write(mimetypeBytes);
            zos.closeEntry();

            // 2. Write META-INF/container.xml
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

            // 3. Build the index.xhtml content (the main HTML file that displays the images)
            StringBuilder indexXhtml = new StringBuilder();
            indexXhtml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                    .append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" ")
                    .append("\"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n")
                    .append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n")
                    .append("<head>\n")
                    .append("<title>").append(epubName).append("</title>\n")
                    .append("</head>\n")
                    .append("<body>\n");

            int max = pngFiles.size();
            for (int i = 0; i < pngFiles.size(); i++) {
                DocumentFile pngFile = pngFiles.get(i);
                // Update progress (like in your PDF generator)
                float percent = (float)(i + 1) / max * 100;
                LoadingFragment.progress = 50 + (percent / 4);
                LoadingFragment.message = "Adding image: " + pngFile.getName();
                // Reference the image file which will be stored in OEBPS/images/
                indexXhtml.append("<img src=\"images/image").append(i)
                        .append(".png\" alt=\"").append(pngFile.getName())
                        .append("\"/><br/>\n");
            }
            indexXhtml.append("</body>\n")
                    .append("</html>");

            // Write the index.xhtml file in OEBPS/
            ZipEntry indexEntry = new ZipEntry("OEBPS/index.xhtml");
            zos.putNextEntry(indexEntry);
            zos.write(indexXhtml.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 4. Build the content.opf file with minimal metadata and a manifest listing all items.
            StringBuilder contentOpf = new StringBuilder();
            contentOpf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                    .append("<package xmlns=\"http://www.idpf.org/2007/opf\" unique-identifier=\"BookId\" version=\"2.0\">\n")
                    .append("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n")
                    .append("<dc:title>").append(epubName).append("</dc:title>\n")
                    .append("<dc:language>en</dc:language>\n")
                    .append("<dc:identifier id=\"BookId\">urn:uuid:").append(UUID.randomUUID().toString()).append("</dc:identifier>\n")
                    .append("</metadata>\n")
                    .append("<manifest>\n")
                    .append("<item id=\"index\" href=\"index.xhtml\" media-type=\"application/xhtml+xml\"/>\n");
            // Add manifest entries for each image
            for (int i = 0; i < pngFiles.size(); i++) {
                contentOpf.append("<item id=\"img").append(i)
                        .append("\" href=\"images/image").append(i)
                        .append(".png\" media-type=\"image/png\"/>\n");
            }
            contentOpf.append("</manifest>\n")
                    .append("<spine toc=\"ncx\">\n")
                    .append("<itemref idref=\"index\"/>\n")
                    .append("</spine>\n")
                    .append("</package>\n");

            // Write the content.opf file in OEBPS/
            ZipEntry opfEntry = new ZipEntry("OEBPS/content.opf");
            zos.putNextEntry(opfEntry);
            zos.write(contentOpf.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 5. Write each PNG image into OEBPS/images/
            for (int i = 0; i < pngFiles.size(); i++) {
                DocumentFile pngFile = pngFiles.get(i);
                LoadingFragment.message = "Writing image: " + pngFile.getName();
                float percent = (float)(i + 1) / max * 100;
                LoadingFragment.progress = 75 + (percent / 4);
                InputStream imageStream = context.getContentResolver().openInputStream(pngFile.getUri());
                if (imageStream == null) continue;

                ZipEntry imageEntry = new ZipEntry("OEBPS/images/image" + i + ".png");
                zos.putNextEntry(imageEntry);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = imageStream.read(buffer)) != -1) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                imageStream.close();
            }

            zos.close();
            byte[] epubBytes = baos.toByteArray();

            // 6. Save the EPUB file in the "rendered" folder (like your PDF generator)
            DocumentFile renderedFolder = Constant.ExtractedFile.findFile("rendered");
            if (renderedFolder == null) {
                renderedFolder = Constant.ExtractedFile.createDirectory("rendered");
            }
            if (renderedFolder == null) {
                throw new IOException("Could not create or find the rendered folder");
            }
            DocumentFile epubFile = renderedFolder.createFile("application/epub+zip", epubName + ".epub");
            if (epubFile == null) {
                throw new IOException("Could not create the EPUB file");
            }
            OutputStream outputStream = context.getContentResolver().openOutputStream(epubFile.getUri());
            if (outputStream == null) {
                throw new IOException("Could not open output stream for EPUB file");
            }
            outputStream.write(epubBytes);
            outputStream.close();

            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, String.format(Locale.getDefault(), "EPUB created: %s", epubFile.getUri().toString()), Toast.LENGTH_LONG).show()
            );

            return epubFile;

        } catch (IOException e) {
            e.printStackTrace();
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "Error creating EPUB: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        }
        return null;
    }
}
