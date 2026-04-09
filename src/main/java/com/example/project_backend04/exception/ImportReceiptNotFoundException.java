package com.example.project_backend04.exception;

/**
 * Exception thrown when an import receipt is not found
 */
public class ImportReceiptNotFoundException extends RuntimeException {
    
    private final Long importReceiptId;
    
    public ImportReceiptNotFoundException(Long importReceiptId) {
        super("Import receipt not found with id: " + importReceiptId);
        this.importReceiptId = importReceiptId;
    }
    
    public Long getImportReceiptId() {
        return importReceiptId;
    }
}
