package com.patres.alina.common.message;

/**
 * Represents an image attachment pasted by the user into the chat input.
 * The image data is stored as a Base64-encoded PNG string.
 *
 * @param base64Data Base64-encoded PNG image data (without the data URI prefix)
 * @param mimeType   MIME type of the image (e.g. "image/png")
 */
public record ImageAttachment(
        String base64Data,
        String mimeType
) {

    public ImageAttachment(String base64Data) {
        this(base64Data, "image/png");
    }

    /**
     * Returns a data URI suitable for embedding in HTML img tags.
     */
    public String toDataUri() {
        return "data:" + mimeType + ";base64," + base64Data;
    }
}
