package com.dolai.backend.document.model;

import com.dolai.backend.todo.model.TodoRequestDto;
import lombok.*;

import java.util.List;

@Builder
@Getter
public class DocumentResponseDto {
    private int version;
    private String title;
    private String date;
    private List<String> participants;
    private String duration;
    private String organizer;
    private String summary;
    private String detailedContent;
    private String graphs;
    private String notes;
    private List<TodoRequestDto> todoList;
    private String documentUrl;
}