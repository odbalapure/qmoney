package com.crio.warmup.stock.dto;

import java.util.Comparator;

public class Include implements Comparator<TotalReturnsDto> {

    public static final com.fasterxml.jackson.annotation.JsonInclude.Include NON_NULL = null;

	@Override
    public int compare(TotalReturnsDto arg0, TotalReturnsDto arg1) {   
        return (int) (arg0.getClosingPrice() - arg1.getClosingPrice());
    }

}

