package com.code.back_end.service;

import com.code.back_end.dto.AIReportDTO;
import com.code.back_end.dto.AIReportSummaryDTO;
import com.code.back_end.dto.FinancialInsightsDTO;
import com.code.back_end.dto.OccupancyAnalysisDTO;
import com.code.back_end.dto.PaymentAnalysisDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PDFExportService {

    private static final int MAX_LINE_LENGTH = 88;

    private final ReportAIService reportAIService;

    public PDFExportService(ReportAIService reportAIService) {
        this.reportAIService = reportAIService;
    }

    public byte[] exportAIReportPdf() {
        AIReportSummaryDTO summary = reportAIService.getSummary();
        FinancialInsightsDTO financial = reportAIService.getFinancialInsights();
        OccupancyAnalysisDTO occupancy = reportAIService.getOccupancyAnalysis();
        PaymentAnalysisDTO payment = reportAIService.getPaymentAnalysis();
        AIReportDTO occupancyReport = reportAIService.getOccupancyReport();
        AIReportDTO financialReport = reportAIService.getFinancialAnalysis();
        AIReportDTO billingReport = reportAIService.getBillingAnalysis();
        AIReportDTO contractReport = reportAIService.getContractAnalysis();

        List<String> lines = new ArrayList<>();
        lines.add("AI Market Management Report");
        lines.add("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        lines.add("");
        lines.add("AI Summary");
        addWrapped(lines, summary.getSummary());
        lines.add("Revenue Analysis");
        addWrapped(lines, financialReport.getNarrative());
        lines.add("Expected revenue: PHP " + financial.getTotalBilled());
        lines.add("Collected revenue: PHP " + financial.getTotalCollected());
        lines.add("Outstanding balance: PHP " + financial.getOutstandingBalance());
        lines.add("Collection efficiency: " + financial.getCollectionRate() + "%");
        lines.add("");
        lines.add("Occupancy Analysis");
        addWrapped(lines, occupancyReport.getNarrative());
        lines.add("Total stalls: " + occupancy.getTotalStalls());
        lines.add("Occupied stalls: " + occupancy.getOccupiedStalls());
        lines.add("Vacant stalls: " + occupancy.getVacantStalls());
        lines.add("Occupancy rate: " + occupancy.getOccupancyRate() + "%");
        lines.add("");
        lines.add("Payment Analysis");
        addWrapped(lines, payment.getSummary());
        lines.add("Current month collected: PHP " + payment.getCurrentMonthCollected());
        lines.add("Previous month collected: PHP " + payment.getPreviousMonthCollected());
        lines.add("Month movement: " + payment.getPercentageChange() + "%");
        lines.add("");
        lines.add("Contract Analysis");
        addWrapped(lines, contractReport.getNarrative());
        lines.add("");
        lines.add("Billing Analysis");
        addWrapped(lines, billingReport.getNarrative());
        lines.add("");
        lines.add("Recommendations");
        summary.getRecommendations().forEach(item -> addWrapped(lines, "- " + item));

        return buildPdf(lines);
    }

    private void addWrapped(List<String> lines, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        String[] words = text.replace("\r", " ").replace("\n", " ").split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() + word.length() + 1 > MAX_LINE_LENGTH) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(word);
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
    }

    private byte[] buildPdf(List<String> lines) {
        List<String> objects = new ArrayList<>();
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 11 Tf\n50 780 Td\n14 TL\n");

        for (String line : lines) {
            if (line.isBlank()) {
                content.append("T*\n");
                continue;
            }

            content.append("(")
                    .append(escapePdf(line))
                    .append(") Tj\nT*\n");
        }

        content.append("ET\n");
        byte[] contentBytes = content.toString().getBytes(StandardCharsets.US_ASCII);

        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        objects.add("<< /Length " + contentBytes.length + " >>\nstream\n" + content + "endstream");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n");

        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            write(out, (i + 1) + " 0 obj\n");
            write(out, objects.get(i));
            write(out, "\nendobj\n");
        }

        int xref = out.size();
        write(out, "xref\n0 " + (objects.size() + 1) + "\n");
        write(out, "0000000000 65535 f \n");
        for (Integer offset : offsets) {
            write(out, String.format("%010d 00000 n \n", offset));
        }
        write(out, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n");
        write(out, "startxref\n" + xref + "\n%%EOF");
        return out.toByteArray();
    }

    private String escapePdf(String value) {
        return value.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private void write(ByteArrayOutputStream out, String value) {
        out.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }
}
