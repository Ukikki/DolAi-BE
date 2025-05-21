package com.dolai.backend.llm;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
public class LlmSummaryDto {
    private List<Section> content;
    private List<String> result;
    private String summary;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Section {
        private String title;
        private String body;
    }
}
