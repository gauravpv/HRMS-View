package com.app.dto;

import lombok.Data;

@Data
public class AjaxBody {

    private String msg;
    /** List rows for grids, or a single object (e.g. dashboard summary). */
    private Object result;
}
