package com.code.back_end.controller;

import com.code.back_end.dto.AIReportDTO;
import com.code.back_end.dto.AIReportSummaryDTO;
import com.code.back_end.dto.FinancialInsightsDTO;
import com.code.back_end.dto.OccupancyAnalysisDTO;
import com.code.back_end.dto.PaymentAnalysisDTO;
import com.code.back_end.service.ExcelExportService;
import com.code.back_end.service.PDFExportService;
import com.code.back_end.service.ReportAIService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/ai/reports", "/api/reports"})
public class ReportAIController {

    private final ReportAIService reportAIService;
    private final PDFExportService pdfExportService;
    private final ExcelExportService excelExportService;

    public ReportAIController(
            ReportAIService reportAIService,
            PDFExportService pdfExportService,
            ExcelExportService excelExportService
    ) {
        this.reportAIService = reportAIService;
        this.pdfExportService = pdfExportService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/summary")
    public AIReportSummaryDTO getSummary() {
        return reportAIService.getSummary();
    }

    @GetMapping("/payment-analysis")
    public PaymentAnalysisDTO getPaymentAnalysis() {
        return reportAIService.getPaymentAnalysis();
    }

    @GetMapping("/occupancy-analysis")
    public OccupancyAnalysisDTO getOccupancyAnalysis() {
        return reportAIService.getOccupancyAnalysis();
    }

    @GetMapping("/occupancy-report")
    public AIReportDTO getOccupancyReport() {
        return reportAIService.getOccupancyReport();
    }

    @GetMapping("/financial-insights")
    public FinancialInsightsDTO getFinancialInsights() {
        return reportAIService.getFinancialInsights();
    }

    @GetMapping("/financial-analysis")
    public AIReportDTO getFinancialAnalysis() {
        return reportAIService.getFinancialAnalysis();
    }

    @GetMapping("/billing-analysis")
    public AIReportDTO getBillingAnalysis() {
        return reportAIService.getBillingAnalysis();
    }

    @GetMapping("/contract-analysis")
    public AIReportDTO getContractAnalysis() {
        return reportAIService.getContractAnalysis();
    }

    @GetMapping("/stakeholder-analysis")
    public AIReportDTO getStakeholderAnalysis() {
        return reportAIService.getStakeholderAnalysis();
    }

    @PostMapping("/generate")
    public AIReportDTO generateReport(
            @RequestParam(defaultValue = "occupancy") String reportType
    ) {
        return reportAIService.generateReport(reportType);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("ai-market-report.pdf")
                                .build()
                                .toString()
                )
                .body(pdfExportService.exportAIReportPdf());
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("ai-market-report.xlsx")
                                .build()
                                .toString()
                )
                .body(excelExportService.exportAIReportExcel());
    }
}
