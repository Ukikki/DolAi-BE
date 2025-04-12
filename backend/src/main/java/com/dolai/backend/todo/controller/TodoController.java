package com.dolai.backend.todo.controller;

import com.dolai.backend.common.success.SuccessDataResponse;
import com.dolai.backend.common.success.SuccessMessageResponse;
import com.dolai.backend.todo.model.TodoRequestDto;
import com.dolai.backend.todo.model.TodoResponseDto;
import com.dolai.backend.todo.model.TodoStatusUpdateRequestDto;
import com.dolai.backend.todo.service.TodoService;
import com.dolai.backend.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/todo")
public class TodoController {

    private final TodoService todoService;

    @GetMapping
    public ResponseEntity<?> getMyTodos(@AuthenticationPrincipal User user) {
        List<TodoResponseDto> todos = todoService.getTodosByUser(user.getId());
        return ResponseEntity.ok(new SuccessDataResponse<>(todos));
    }

    @PostMapping
    public ResponseEntity<?> createTodo(@RequestBody @Valid TodoRequestDto request,
                                        @AuthenticationPrincipal User user) {
        todoService.createTodo(request, user);
        return ResponseEntity.ok(new SuccessMessageResponse("To-do item added successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateTodoStatus(@PathVariable("id") Long id,
                                              @RequestBody @Valid TodoStatusUpdateRequestDto request) {
        todoService.updateTodoStatus(id, request.getStatus());
        return ResponseEntity.ok(new SuccessMessageResponse("To-do status updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTodo(@PathVariable("id") Long id) {
        todoService.deleteTodo(id);
        return ResponseEntity.ok(new SuccessMessageResponse("To-do item deleted successfully"));
    }
}