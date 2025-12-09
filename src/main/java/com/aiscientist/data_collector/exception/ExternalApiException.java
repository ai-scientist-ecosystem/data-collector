package com.aiscientist.data_collector.exception;

public class ExternalApiException extends RuntimeException {
    
    private final String apiName;
    private final int statusCode;
    
    public ExternalApiException(String apiName, int statusCode, String message) {
        super("API %s failed with status %d: %s".formatted(apiName, statusCode, message));
        this.apiName = apiName;
        this.statusCode = statusCode;
    }
    
    public ExternalApiException(String apiName, String message, Throwable cause) {
        super("API %s failed: %s".formatted(apiName, message), cause);
        this.apiName = apiName;
        this.statusCode = 0;
    }
    
    public String getApiName() {
        return apiName;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}
