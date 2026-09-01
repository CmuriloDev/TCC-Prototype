package com.adapter.prototype.controller;

import com.adapter.prototype.dto.ResumoRequest;
import com.adapter.prototype.dto.ResumoResponse;
import com.adapter.prototype.service.ResumoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resumo")
public class ResumoController {

    private final ResumoService resumoService;

    public ResumoController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @PostMapping
    public ResponseEntity<ResumoResponse> gerarResumo(@RequestBody ResumoRequest request) {
        ResumoResponse response = resumoService.gerarResumo(request);
        return ResponseEntity.ok(response);
    }
}
