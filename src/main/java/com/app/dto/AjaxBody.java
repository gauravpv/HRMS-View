package com.app.dto;

import java.util.List;

import lombok.Data;

@Data
public class AjaxBody {

    private String msg;
    private List<?> result;
}
