package com.code.back_end.service;

import com.code.back_end.entity.Billing;
import com.code.back_end.entity.Payment;
import com.code.back_end.entity.Stakeholder;
import com.code.back_end.entity.Stall;
import com.code.back_end.repository.BillingRepository;
import com.code.back_end.repository.PaymentRepository;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.repository.StallRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExcelExportService {

    private final PaymentRepository paymentRepository;
    private final BillingRepository billingRepository;
    private final StallRepository stallRepository;
    private final StakeholderRepository stakeholderRepository;

    public ExcelExportService(
            PaymentRepository paymentRepository,
            BillingRepository billingRepository,
            StallRepository stallRepository,
            StakeholderRepository stakeholderRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.billingRepository = billingRepository;
        this.stallRepository = stallRepository;
        this.stakeholderRepository = stakeholderRepository;
    }

    public byte[] exportAIReportExcel() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(out);

            add(zip, "[Content_Types].xml", contentTypes());
            add(zip, "_rels/.rels", rootRels());
            add(zip, "xl/workbook.xml", workbook());
            add(zip, "xl/_rels/workbook.xml.rels", workbookRels());
            add(zip, "xl/styles.xml", styles());
            add(zip, "xl/worksheets/sheet1.xml", sheet(paymentRows()));
            add(zip, "xl/worksheets/sheet2.xml", sheet(occupancyRows()));
            add(zip, "xl/worksheets/sheet3.xml", sheet(revenueRows()));
            add(zip, "xl/worksheets/sheet4.xml", sheet(tenantRows()));
            zip.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to export AI Excel report", ex);
        }
    }

    private List<List<String>> paymentRows() {
        List<List<String>> rows = new java.util.ArrayList<>();
        rows.add(List.of("Receipt No", "Business", "Payment Type", "Amount", "Payment Date", "Billing No"));
        for (Payment payment : paymentRepository.findAll()) {
            rows.add(List.of(
                    safe(payment.getReceiptNo()),
                    payment.getStakeholder() == null ? "" : safe(payment.getStakeholder().getBusinessName()),
                    payment.getPaymentType() == null ? "" : payment.getPaymentType().name(),
                    money(payment.getAmount()),
                    payment.getPaymentDate() == null ? "" : payment.getPaymentDate().toString(),
                    payment.getBilling() == null ? "" : safe(payment.getBilling().getBillingNo())
            ));
        }
        return rows;
    }

    private List<List<String>> occupancyRows() {
        List<List<String>> rows = new java.util.ArrayList<>();
        rows.add(List.of("Stall No", "Stall Type", "Status", "Monthly Rent", "Occupant"));
        for (Stall stall : stallRepository.findAll()) {
            rows.add(List.of(
                    safe(stall.getStallNo()),
                    safe(stall.getStallType()),
                    safe(stall.getStatus()),
                    stall.getMonthlyRent() == null ? "0" : stall.getMonthlyRent().toString(),
                    stall.getOccupant() == null || stall.getOccupant().getStakeholder() == null
                            ? ""
                            : safe(stall.getOccupant().getStakeholder().getBusinessName())
            ));
        }
        return rows;
    }

    private List<List<String>> revenueRows() {
        List<List<String>> rows = new java.util.ArrayList<>();
        rows.add(List.of("Billing No", "Billing Period", "Total Amount", "Paid Amount", "Balance", "Due Date", "Status"));
        for (Billing billing : billingRepository.findAll()) {
            rows.add(List.of(
                    safe(billing.getBillingNo()),
                    safe(billing.getBillingPeriod()),
                    money(billing.getTotalAmount()),
                    money(billing.getPaidAmount()),
                    money(billing.getBalance()),
                    billing.getDueDate() == null ? "" : billing.getDueDate().toString(),
                    safe(billing.getStatus())
            ));
        }
        return rows;
    }

    private List<List<String>> tenantRows() {
        List<List<String>> rows = new java.util.ArrayList<>();
        rows.add(List.of("Business Name", "Owner", "Business Type", "Application Status", "Verified Tenant", "Contact"));
        for (Stakeholder stakeholder : stakeholderRepository.findAll()) {
            rows.add(List.of(
                    safe(stakeholder.getBusinessName()),
                    (safe(stakeholder.getFirstName()) + " " + safe(stakeholder.getLastName())).trim(),
                    safe(stakeholder.getBusinessType()),
                    safe(stakeholder.getApplicationStatus()),
                    String.valueOf(Boolean.TRUE.equals(stakeholder.getVerifiedTenant())),
                    safe(stakeholder.getContact())
            ));
        }
        return rows;
    }

    private String sheet(List<List<String>> rows) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            xml.append("<row r=\"").append(rowIndex + 1).append("\">");
            List<String> row = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                xml.append("<c r=\"").append(columnName(columnIndex)).append(rowIndex + 1)
                        .append("\" t=\"inlineStr\"><is><t>")
                        .append(escapeXml(row.get(columnIndex)))
                        .append("</t></is></c>");
            }
            xml.append("</row>");
        }
        xml.append("</sheetData></worksheet>");
        return xml.toString();
    }

    private String workbook() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    <sheet name="Payments" sheetId="1" r:id="rId1"/>
                    <sheet name="Occupancy" sheetId="2" r:id="rId2"/>
                    <sheet name="Revenue" sheetId="3" r:id="rId3"/>
                    <sheet name="Tenants" sheetId="4" r:id="rId4"/>
                  </sheets>
                </workbook>
                """;
    }

    private String workbookRels() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
                  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
                  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
                  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """;
    }

    private String rootRels() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """;
    }

    private String contentTypes() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
                """;
    }

    private String styles() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
                  <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
                  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
                </styleSheet>
                """;
    }

    private void add(ZipOutputStream zip, String path, String body) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(body.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String columnName(int index) {
        return String.valueOf((char) ('A' + index));
    }

    private String money(java.math.BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String escapeXml(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
