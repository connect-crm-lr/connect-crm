package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.DashboardResponse;
import br.com.lrrepresentacoes.connectcrm.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Padrão Singleton: bean do Spring, uma única instância na aplicação.
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // GET /dashboard: vendas do mês, negociações por etapa, pendências de hoje e estoque baixo.
    @GetMapping
    public DashboardResponse resumo() {
        return dashboardService.resumo();
    }
}
