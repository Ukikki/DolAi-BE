package com.dolai.backend.directory.controller;

import com.dolai.backend.directory.service.DirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/directories")
@RequiredArgsConstructor
public class DirectoryController {
    private final DirectoryService directoryService;

}
