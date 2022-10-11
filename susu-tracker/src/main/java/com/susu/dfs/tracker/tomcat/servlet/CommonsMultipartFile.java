package com.susu.dfs.tracker.tomcat.servlet;

import com.susu.dfs.common.file.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItem;
import javax.annotation.Nullable;
import java.io.*;


/**
 * {@link MultipartFile} implementation for Apache Commons FileUpload.
 *
 * @author sujay
 * @since 11.10.2022
 */
@Slf4j
public class CommonsMultipartFile implements MultipartFile, Serializable {

    private static final byte[] EMPTY_CONTENT = new byte[0];

    /**
     * file source
     */
    private final FileItem fileItem;

    /**
     * file size
     */
    private final long size;


    private boolean preserveFilename = false;


    public CommonsMultipartFile(FileItem fileItem) {
        this.fileItem = fileItem;
        this.size = this.fileItem.getSize();
    }
    @Override
    public InputStream getInputStream() throws IOException {
        if (!isAvailable()) {
            throw new IllegalStateException("File has been moved - cannot be read again");
        }
        InputStream inputStream = this.fileItem.getInputStream();
        return (inputStream != null ? inputStream : new ByteArrayInputStream(EMPTY_CONTENT));
    }

    @Override
    public String getName() {
        return this.fileItem.getFieldName();
    }

    @Nullable
    @Override
    public String getOriginalFilename() {

        String filename = this.fileItem.getName();
        if (filename == null) {
            return "";
        }

        if (this.preserveFilename) {
            return filename;
        }

        int unixSep = filename.lastIndexOf('/');

        int winSep = filename.lastIndexOf('\\');

        int pos = Math.max(winSep, unixSep);

        if (pos != -1)  {
            return filename.substring(pos + 1);
        } else {
            return filename;
        }
    }

    @Nullable
    @Override
    public String getContentType() {
        return this.fileItem.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return (this.size == 0);
    }

    @Override
    public long getSize() {
        return this.size;
    }

    @Override
    public byte[] getBytes() throws IOException {
        if (!isAvailable()) {
            throw new IllegalStateException("File has been moved - cannot be read again");
        }
        byte[] bytes = this.fileItem.get();
        return (bytes != null ? bytes : new byte[0]);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        if (!isAvailable()) {
            throw new IllegalStateException("File has already been moved - cannot be transferred again");
        }

        if (dest.exists() && !dest.delete()) {
            throw new IOException(
                    "Destination file [" + dest.getAbsolutePath() + "] already exists and could not be deleted");
        }

        try {
            this.fileItem.write(dest);
        } catch (FileUploadException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        } catch (IllegalStateException | IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("File transfer failed", ex);
        }
    }


    /**
     * Determine whether the multipart content is still available.
     * If a temporary file has been moved, the content is no longer available.
     */
    protected boolean isAvailable() {
        if (this.fileItem.isInMemory()) {
            return true;
        }
        if (this.fileItem instanceof DiskFileItem) {
            return ((DiskFileItem) this.fileItem).getStoreLocation().exists();
        }
        return (this.fileItem.getSize() == this.size);
    }
}
