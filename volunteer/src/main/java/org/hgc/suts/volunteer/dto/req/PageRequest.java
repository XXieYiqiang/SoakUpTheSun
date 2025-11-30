package org.hgc.suts.volunteer.dto.req;

import lombok.Data;

@Data
public class PageRequest {


    private int current = 1;


    private int pageSize = 10;


    private String sortField;


    private String sortOrder = "descend";
}