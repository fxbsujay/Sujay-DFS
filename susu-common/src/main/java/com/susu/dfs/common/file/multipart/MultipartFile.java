package com.susu.dfs.common.file.multipart;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple interface for objects that are sources for an {@link InputStream}.
 *
 * This makes this interface useful as an abstract content source for mail
 * attachments, for example.
 *
 * @author sujay
 * @since 11.10.2022
 * @see java.io.InputStream
 */
public interface MultipartFile {

    /**
     * Return an {@link InputStream} for the content of an underlying resource.
     * <p>It is expected that each call creates a <i>fresh</i> stream.
     * <p>This requirement is particularly important when you consider an API such
     * as JavaMail, which needs to be able to read the stream multiple times when
     * creating mail attachments. For such a use case, it is <i>required</i>
     * that each {@code getInputStream()} call returns a fresh stream.
     * @return the input stream for the underlying resource (must not be {@code null})
     * @throws java.io.FileNotFoundException if the underlying resource does not exist
     * @throws IOException if the content stream could not be opened
     */
    InputStream getInputStream() throws IOException;

    String getName();

    @Nullable
    String getOriginalFilename();

    @Nullable
    String getContentType();

    boolean isEmpty();

    long getSize();

    byte[] getBytes() throws IOException;

    void transferTo(File dest) throws IOException, IllegalStateException;
}
