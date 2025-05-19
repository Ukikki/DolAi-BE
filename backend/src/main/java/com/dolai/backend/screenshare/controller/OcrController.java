package com.dolai.backend.screenshare.controller;

import com.dolai.backend.screenshare.service.AzureOcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/azure-ocr")
public class OcrController {

    private final AzureOcrService azureOcrService;

    @PostMapping("/image")
    public Mono<ResponseEntity<String>> recognizeText(@RequestParam("file") MultipartFile file) throws Exception {
        byte[] imageBytes = file.getBytes();
        return azureOcrService.extractTextFromImage(imageBytes)
                .map(ResponseEntity::ok);
    }
}