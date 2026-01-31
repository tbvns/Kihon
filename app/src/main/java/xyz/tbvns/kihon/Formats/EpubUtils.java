package xyz.tbvns.kihon.Formats;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.documentfile.provider.DocumentFile;
import xyz.tbvns.kihon.Config.ExportSetting;
import xyz.tbvns.kihon.Constants;
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

    public static DocumentFile generateEpub(Context context, List<DocumentFile> pngFiles,
                                            String epubName) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, "Generating EPUB...", Toast.LENGTH_LONG).show()
        );

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            // Write mimetype
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

            // Write container.xml
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

            // Build index XHTML
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
                LoadingFragment.progress += (float) (((1 / max) * 100) / 2) *
                        ExportSetting.secondaryActionImpact;
                LoadingFragment.message = "Adding image: " + pngFile.getName();
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

            // Build manifest items and metadata
            StringBuilder manifestItems = new StringBuilder();
            StringBuilder metadataItems = new StringBuilder();

            // Handle cover image
            String coverImageId = null;
            boolean hasCover = false;

            if (ExportSetting.USE_COVER) {
                if (ExportSetting.COVER_USE_PAGE &&
                        ExportSetting.COVER_PAGE_INDEX < pngFiles.size()) {
                    // Use a page as cover
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
                    // Use custom image as cover
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

            // Build content.opf
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

            // Add all page images to manifest
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

            ZipEntry opfEntry = new ZipEntry("OEBPS/content.opf");
            zos.putNextEntry(opfEntry);
            zos.write(contentOpf.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Write custom cover image if used
            if (hasCover && ExportSetting.COVER_USE_CUSTOM &&
                    !ExportSetting.COVER_CUSTOM_BITMAP_PATH.isEmpty()) {
                LoadingFragment.message = "Writing cover image";
                LoadingFragment.progress += 2;

                Bitmap coverBitmap = BitmapFactory.decodeFile(
                        ExportSetting.COVER_CUSTOM_BITMAP_PATH);

                if (coverBitmap != null) {
                    ZipEntry coverEntry = new ZipEntry("OEBPS/images/cover.png");
                    zos.putNextEntry(coverEntry);
                    coverBitmap.compress(Bitmap.CompressFormat.PNG, 100, zos);
                    zos.closeEntry();
                    coverBitmap.recycle();
                }
            }

            // Write all page images
            for (int i = 0; i < pngFiles.size(); i++) {
                DocumentFile pngFile = pngFiles.get(i);
                LoadingFragment.message = "Writing image: " + pngFile.getName();
                LoadingFragment.progress += (float) (((1 / max) * 100) / 2) *
                        ExportSetting.secondaryActionImpact;

                InputStream imageStream = context.getContentResolver()
                        .openInputStream(pngFile.getUri());
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

            // Save the EPUB file
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

            outputStream.write(epubBytes);
            outputStream.close();

            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context,
                            String.format(Locale.getDefault(),
                                    "EPUB created: %s",
                                    epubFile.getUri().toString()),
                            Toast.LENGTH_LONG).show()
            );

            return epubFile;

        } catch (IOException e) {
            e.printStackTrace();
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "Error creating EPUB: " + e.getMessage(),
                            Toast.LENGTH_LONG).show()
            );
        }
        return null;
    }
}