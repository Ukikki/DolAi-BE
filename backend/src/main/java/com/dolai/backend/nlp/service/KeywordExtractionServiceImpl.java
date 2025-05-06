package com.dolai.backend.nlp.service;

import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.springframework.stereotype.Service;
import scala.collection.Seq;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KeywordExtractionServiceImpl implements KeywordExtractionService {

    @Override
    public List<String> extract(String text) {
        // 1. Normalize and tokenize
        CharSequence normalized = OpenKoreanTextProcessorJava.normalize(text);
        Seq<org.openkoreantext.processor.tokenizer.KoreanTokenizer.KoreanToken> tokens =
                OpenKoreanTextProcessorJava.tokenize(normalized);

        // 2. Convert to Java List and filter only Nouns
        return OpenKoreanTextProcessorJava.tokensToJavaKoreanTokenList(tokens).stream()
                .filter(token -> token.getPos().toString().equals("Noun"))
                .map(token -> token.getText().toString())
                .distinct() // 중복 제거
                .collect(Collectors.toList());
    }
}