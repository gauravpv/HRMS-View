package com.app.dto;

import java.util.ArrayList;
import java.util.List;

public class StringListDto {
    
    
    List<String> stringList = new ArrayList<>();

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    @Override
    public String toString() {
        return "StringListDto [stringList=" + stringList + "]";
    }
    
    
}
