package com.community.prime.vo;

import lombok.Data;

import java.util.List;

/**
 * 分页结果封装
 */
@Data
public class PageResult {

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页数据
     */
    private List<?> list;

    public PageResult() {
    }

    public PageResult(Long total, List<?> list) {
        this.total = total;
        this.list = list;
    }
}
