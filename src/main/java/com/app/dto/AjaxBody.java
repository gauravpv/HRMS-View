package com.app.dto;

import java.util.List;

public class AjaxBody {
    
    String msg;
    List<?> result;
    
    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public List<?> getResult() {
        return result;
    }
    public void setResult(List<?> result) {
        this.result = result;
    }
    
    @Override
    public String toString() {
        return "AjaxResponseBody [msg=" + msg + ", result=" + result + "]";
    }
 
    
    
}
