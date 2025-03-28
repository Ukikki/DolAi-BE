package com.dolai.backend.todo.service;

import com.dolai.backend.todo.model.Todo;
import com.dolai.backend.todo.model.TodoDto;
import com.dolai.backend.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;

    public List<TodoDto> getTodosByMeeting(String meetingId) {
        List<Todo> todos = todoRepository.findByMeetingId(meetingId);

        return todos.stream()
                .map(todo -> TodoDto.builder()
                        .title(todo.getTitle())
                        .assignee(todo.getUser().getEmail())  // 또는 이름으로 바꿔도 됨
                        .dueDate(todo.getDueDate() != null
                                ? todo.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                : null)
                        .build())
                .toList();
    }
}
