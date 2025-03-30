package com.dolai.backend.document.model.enums;

public enum FileType {
    DOC,
    DOCX,
    PDF,
    TXT,
    PNG,
    JPG,
    JPEG,
    GIF,
    UNKNOWN;

    public static FileType fromExtension(String extension) {
        if (extension == null) return UNKNOWN;
        try {
            return FileType.valueOf(extension.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
