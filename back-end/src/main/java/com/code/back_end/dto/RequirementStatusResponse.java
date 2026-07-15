package com.code.back_end.dto;

import java.util.List;

public class RequirementStatusResponse {

    private boolean complete;
    private List<String> requiredDocuments;
    private List<String> submittedDocuments;
    private List<String> missingDocuments;

    public RequirementStatusResponse(
            boolean complete,
            List<String> requiredDocuments,
            List<String> submittedDocuments,
            List<String> missingDocuments
    ) {
        this.complete = complete;
        this.requiredDocuments = requiredDocuments;
        this.submittedDocuments = submittedDocuments;
        this.missingDocuments = missingDocuments;
    }

    public boolean isComplete() {
        return complete;
    }

    public List<String> getRequiredDocuments() {
        return requiredDocuments;
    }

    public List<String> getSubmittedDocuments() {
        return submittedDocuments;
    }

    public List<String> getMissingDocuments() {
        return missingDocuments;
    }
}
