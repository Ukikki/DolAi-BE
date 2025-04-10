package com.dolai.backend.todo.service;

import com.dolai.backend.todo.model.Todo;
import com.dolai.backend.todo.model.TodoRequestDto;
import com.dolai.backend.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;

    public List<TodoRequestDto> getTodosByMeeting(String meetingId) {
        List<Todo> todos = todoRepository.findByMeetingId(meetingId);

        return todos.stream()
                .map(todo -> TodoRequestDto.builder()
                        .title(todo.getTitle())
                        .assignee(todo.getUser().getEmail())
                        .dueDate(todo.getDueDate() != null
                                ? todo.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                : null)
                        .build())
                .toList();
    }
}
