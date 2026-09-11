package com.adapter.prototype.exception;

import com.adapter.prototype.dto.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TextoInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarTextoInvalido(TextoInvalidoException ex) {
        ErroResponse corpo = new ErroResponse(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo);
    }

    @ExceptionHandler(ProvedorIndisponivelException.class)
    public ResponseEntity<ErroResponse> tratarProvedorIndisponivel(ProvedorIndisponivelException ex) {
        ErroResponse corpo = new ErroResponse(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(corpo);
    }
}
