package org.erp.print.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * 打印模板 VO（列表/详情，不含 content 大字段时用于列表）
 */
@Data
public class PrintTemplateVo {

    private Long id;

    private String code;

    private String name;

    private String scene;

    private Long customerId;

    private String customerName;

    private String engine;

    private String variablesJson;

    private String content;

    private String paper;

    private Integer version;

    private Integer isDefault;

    private Integer status;

    private Date createTime;
}
