package com.app.dto;

import java.sql.Timestamp;

public class AjaxError {
    private String errorMsg;
    private Timestamp time;
    
    public String getErrorMsg() {
        return errorMsg;
    }
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
    public Timestamp getTime() {
        return time;
    }
    public void setTime(Timestamp time) {
        this.time = time;
    }
    
    @Override
    public String toString() {
        return "AjaxError [errorMsg=" + errorMsg + ", time=" + time + "]";
    }
    
    
}
