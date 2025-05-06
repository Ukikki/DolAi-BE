package com.dolai.backend.document.model;

import com.dolai.backend.todo.model.TodoResponseDto;
import lombok.*;
import java.util.List;

@Builder
@Getter
public class DocumentResponseDto {
    private String title;
    private String date;
    private List<String> participants;
    private String duration;
    private String organizer;
    private String detailedContent;
    private String graphs;
    private String notes;
    private List<TodoResponseDto> todoList;
    private String documentUrl;
}