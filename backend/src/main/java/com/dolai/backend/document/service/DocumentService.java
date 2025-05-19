package com.dolai.backend.document.service;

import com.dolai.backend.metadata.repository.DocumentMetaDataRepository;
import com.dolai.backend.metadata.service.DocumentMetaDataService;
import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.document.model.Document;
import com.dolai.backend.document.model.enums.FileType;
import com.dolai.backend.document.repository.DocumentPlacementRepository;
import com.dolai.backend.document.repository.DocumentRepository;
import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.convert.out.pdf.PdfConversion;
import org.docx4j.convert.out.pdf.viaXSLFO.Conversion;
import org.docx4j.convert.out.pdf.viaXSLFO.PdfSettings;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentPlacementRepository documentPlacementRepository;
    private final DocumentMetaDataRepository documentMetaDataRepository;
    private final DocumentMetaDataService documentMetaDataService;

    @Transactional
    public void deleteDocument(Long documentId, User user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        // 관련 엔티티 먼저 삭제
        documentPlacementRepository.deleteAllByDocumentId(documentId);
        documentMetaDataRepository.deleteByDocumentId(documentId);

        // 마지막으로 문서 삭제
        documentRepository.delete(document);
    }

    @Transactional
    public Document createDocument(Meeting meeting, String docUrl, String title, User user) {
        String extension = documentMetaDataService.extractExtension(docUrl);
        FileType fileType = FileType.fromExtension(extension);
        String documentName = title + "." + extension;  // 전달받은 제목 사용

        Document document = Document.create(meeting, docUrl, documentName, fileType);
        documentRepository.save(document);

        documentMetaDataService.createAndSaveMetaData(document);

        return document;
    }

    public String getS3FileUrl(Long documentId, User user) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
        return doc.getFileUrl();
    }

    public Document getById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    public byte[] convertDocxToPdf(File docxFile) {
        try {
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(docxFile);

            ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();

            PdfConversion conversion = new Conversion(wordMLPackage);

            conversion.output(pdfOutputStream, new PdfSettings());

            return pdfOutputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("❌ DOCX to PDF 변환 실패", e);
        }
    }
}
