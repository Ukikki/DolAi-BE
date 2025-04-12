package com.dolai.backend.todo.service;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import com.dolai.backend.meeting.model.Meeting;
import com.dolai.backend.meeting.repository.MeetingRepository;
import com.dolai.backend.todo.model.Todo;
import com.dolai.backend.todo.model.TodoRequestDto;
import com.dolai.backend.todo.model.TodoResponseDto;
import com.dolai.backend.todo.model.enums.Status;
import com.dolai.backend.todo.repository.TodoRepository;
import com.dolai.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;
    private final MeetingRepository meetingRepository;
    public List<TodoResponseDto> getTodosByMeeting(String meetingId) {
        return todoRepository.findByMeetingId(meetingId).stream()
                .map(TodoResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TodoResponseDto> getTodosByUser(String userId) {
        List<Todo> todos = todoRepository.findByUserId(userId);
        return todos.stream()
                .map(TodoResponseDto::from)
                .toList();
    }

    @Transactional
    public void createTodo(TodoRequestDto dto, User user) {
        Meeting meeting = findMeetingIfPresent(dto);
        Todo todo = Todo.create(user, dto, meeting);
        todoRepository.save(todo);
    }

    // meeting 조회
    private Meeting findMeetingIfPresent(TodoRequestDto dto) {
        return Optional.ofNullable(dto.getMeetingId())
                .flatMap(meetingRepository::findById)
                .orElse(null);
    }

    @Transactional
    public void updateTodoStatus(Long todoId, Status status) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new CustomException(ErrorCode.TODO_NOT_FOUND));

        todo.setStatus(status);
        todoRepository.save(todo);
    }

    @Transactional
    public void deleteTodo(Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new CustomException(ErrorCode.TODO_NOT_FOUND));

        todoRepository.delete(todo);
    }

}
