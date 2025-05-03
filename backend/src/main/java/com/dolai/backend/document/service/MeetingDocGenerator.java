package com.dolai.backend.document.service;

import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.*;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MeetingDocGenerator {

    public void generateDocx(Map<String, String> values, File templateFile, File outputFile) {
        try {
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(templateFile);
            MainDocumentPart mainPart = wordMLPackage.getMainDocumentPart();

            // 줄바꿈 문단 직접 삽입할 키
            insertMultilineText(mainPart, "content", values.get("content"));
            insertMultilineText(mainPart, "result", values.get("result"));

            // 나머지는 기본 텍스트 치환
            manualReplace(mainPart, values);

            try (OutputStream os = new FileOutputStream(outputFile)) {
                wordMLPackage.save(os);
            }
        } catch (Exception e) {
            throw new RuntimeException("문서 생성 중 오류 발생", e);
        }
    }

    private void insertMultilineText(MainDocumentPart mainPart, String key, String multilineText) {
        log.info("🔍 {} 치환 시도. 내용: {}", key, multilineText);

        String placeholder = "${" + key + "}";

        List<Object> paragraphs = getAllElementFromObject(mainPart, P.class);
        for (Object pObj : paragraphs) {
            P paragraph = (P) pObj;
            List<Object> texts = getAllElementFromObject(paragraph, Text.class);

            for (Object tObj : texts) {
                Text text = (Text) tObj;
                if (text.getValue().contains(placeholder)) {
                    log.info("📍 Paragraph 내에 '{}' 발견됨", placeholder);

                    List<P> newParagraphs = createParagraphsFromMultiline(multilineText);

                    Tc cell = findContainingTc(mainPart, text);
                    if (cell != null) {
                        cell.getContent().clear();
                        cell.getContent().addAll(newParagraphs);
                        log.info("✅ {} 셀에 문단 직접 삽입 완료 (역축적)", key);
                    } else {
                        log.error("❌ Tc 셀도 무시함 (역축 실패)");
                    }

                    return;
                }
            }
        }
    }

    private Tc findContainingTc(MainDocumentPart mainPart, Text targetText) {
        List<Object> tcs = getAllElementFromObject(mainPart, Tc.class);
        for (Object obj : tcs) {
            Tc cell = (Tc) obj;
            List<Object> texts = getAllElementFromObject(cell, Text.class);
            for (Object t : texts) {
                if (t == targetText) {
                    return cell;
                }
            }
        }
        return null;
    }

    private List<P> createParagraphsFromMultiline(String text) {
        List<P> paragraphs = new ArrayList<>();
        if (text == null) return paragraphs;

        String[] lines = text.split("<w:br/>|<br>|\\n");

        for (String line : lines) {
            P para = new P();
            R run = new R();

            Text t = new Text();
            t.setValue(line.trim());
            t.setSpace("preserve");
            run.getContent().add(t);

            RPr rPr = new RPr();

            // 👇 여기에서 제목 라인일 경우 스타일 다르게
            if (line.trim().startsWith("■ ")) {
                HpsMeasure size = new HpsMeasure();
                size.setVal(BigInteger.valueOf(24)); // 12pt
                rPr.setSz(size);
                rPr.setSzCs(size);

                BooleanDefaultTrue b = new BooleanDefaultTrue();
                RFonts titleFonts = new RFonts();
                titleFonts.setAscii("나눔스퀘어라운드 ExtraBold");
                titleFonts.setHAnsi("나눔스퀘어라운드 ExtraBold");
                titleFonts.setEastAsia("나눔스퀘어라운드 ExtraBold");
                rPr.setRFonts(titleFonts);
            } else {
                HpsMeasure size = new HpsMeasure();
                size.setVal(BigInteger.valueOf(20)); // 10pt
                rPr.setSz(size);
                rPr.setSzCs(size);

                RFonts bodyFonts = new RFonts();
                bodyFonts.setAscii("나눔스퀘어라운드 Regular");
                bodyFonts.setHAnsi("나눔스퀘어라운드 Regular");
                bodyFonts.setEastAsia("나눔스퀘어라운드 Regular");
                rPr.setRFonts(bodyFonts);
            }

            run.setRPr(rPr);

            // 문단 정렬
            PPr pPr = new PPr();
            Jc jc = new Jc();
            jc.setVal(JcEnumeration.LEFT);
            pPr.setJc(jc);
            para.setPPr(pPr);

            para.getContent().add(run);
            paragraphs.add(para);
        }
        return paragraphs;
    }

    private void manualReplace(MainDocumentPart mainPart, Map<String, String> values) {
        List<Object> texts = getAllElementFromObject(mainPart, Text.class);

        for (Object obj : texts) {
            Text textElement = (Text) obj;
            String text = textElement.getValue();

            for (Map.Entry<String, String> entry : values.entrySet()) {
                String key = "${" + entry.getKey() + "}";

                // content/result는 이미 처리했으니 건너뛰기
                if (entry.getKey().equals("content") || entry.getKey().equals("result")) continue;

                if (text.contains(key)) {
                    textElement.setValue(text.replace(key, entry.getValue()));
                }
            }
        }
    }

    private List<Object> getAllElementFromObject(Object obj, Class<?> toSearch) {
        List<Object> result = new ArrayList<>();
        if (obj instanceof JAXBElement) obj = ((JAXBElement<?>) obj).getValue();

        if (toSearch.isAssignableFrom(obj.getClass())) {
            result.add(obj);
        } else if (obj instanceof ContentAccessor) {
            List<?> children = ((ContentAccessor) obj).getContent();
            for (Object child : children) {
                result.addAll(getAllElementFromObject(child, toSearch));
            }
        }
        return result;
    }
}