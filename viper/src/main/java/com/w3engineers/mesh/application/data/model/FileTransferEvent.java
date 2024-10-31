package com.w3engineers.mesh.application.data.model;

public class FileTransferEvent extends Event {
    private String fileMessageId;
    private String errorMessage;
    private boolean isSuccess;

    public String getFileMessageId() {
        return fileMessageId;
    }

    public void setFileMessageId(String fileMessageId) {
        this.fileMessageId = fileMessageId;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage(){
        return this.errorMessage;
    }
}
