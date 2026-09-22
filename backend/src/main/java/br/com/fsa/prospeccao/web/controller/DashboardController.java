package br.com.fsa.prospeccao.web.controller;

import br.com.fsa.prospeccao.service.DashboardService;
import br.com.fsa.prospeccao.web.dto.DashboardResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardResponse resumo() {
        return service.resumo();
    }
}
