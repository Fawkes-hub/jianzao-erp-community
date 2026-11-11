package org.erp.print.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.erp.master.mapper.ErpCustomerMapper;
import org.erp.print.domain.PrintTemplate;
import org.erp.print.domain.bo.PrintTemplateBo;
import org.erp.print.mapper.PrintTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 打印模板服务测试：清洗入库 / 编码唯一 / 默认互斥 / 停用 / 解析顺序
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrintTemplateServiceImplTest {

    private static final String SAFE_HTML = "<html><body><h1>送货单</h1></body></html>";

    @Mock
    private PrintTemplateMapper templateMapper;

    @Mock
    private ErpCustomerMapper customerMapper;

    private PrintTemplateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PrintTemplateServiceImpl(templateMapper, customerMapper, new TemplateSanitizer());
    }

    private PrintTemplateBo bo(String code, String scene, Integer isDefault) {
        PrintTemplateBo bo = new PrintTemplateBo();
        bo.setCode(code);
        bo.setName("模板" + code);
        bo.setScene(scene);
        bo.setContent(SAFE_HTML);
        bo.setIsDefault(isDefault);
        return bo;
    }

    @Test
    @DisplayName("新增：清洗后入库，版本 1")
    void insertCleansAndSetsVersion() {
        PrintTemplateBo b = bo("T1", "SALE_DELIVERY", 0);
        b.setContent(SAFE_HTML + "<script>alert(1)</script>");
        service.insert(b);
        ArgumentCaptor<PrintTemplate> captor = ArgumentCaptor.forClass(PrintTemplate.class);
        verify(templateMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getVersion());
        assertEquals(-1, captor.getValue().getContent().indexOf("<script"));
        assertEquals("THYMELEAF", captor.getValue().getEngine());
    }

    @Test
    @DisplayName("新增：编码重复 40902")
    void insertDuplicateCodeRejected() {
        when(templateMapper.selectCount(any())).thenReturn(1L);
        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.insert(bo("T1", "SALE_DELIVERY", 0)));
        assertEquals(40902, ex.getCode());
    }

    @Test
    @DisplayName("新增：非法场景拒绝")
    void insertInvalidSceneRejected() {
        assertThrows(ServiceException.class, () -> service.insert(bo("T1", "HACK", 0)));
        verify(templateMapper, never()).insert(any(PrintTemplate.class));
    }

    @Test
    @DisplayName("新增：设默认清除同场景旧默认")
    void insertDefaultClearsOld() {
        when(templateMapper.selectCount(any())).thenReturn(0L);
        PrintTemplate old = new PrintTemplate();
        old.setId(9L);
        old.setScene("SALE_DELIVERY");
        old.setIsDefault(1);
        when(templateMapper.selectList(any())).thenReturn(List.of(old));
        service.insert(bo("T1", "SALE_DELIVERY", 1));
        assertEquals(0, old.getIsDefault());
        verify(templateMapper, atLeastOnce()).updateById(old);
    }

    @Test
    @DisplayName("编辑：版本 +1")
    void updateIncrementsVersion() {
        PrintTemplate existing = new PrintTemplate();
        existing.setId(1L);
        existing.setCode("T1");
        existing.setScene("SALE_DELIVERY");
        existing.setVersion(3);
        existing.setStatus(1);
        existing.setIsDefault(0);
        when(templateMapper.selectById(1L)).thenReturn(existing);
        when(templateMapper.selectCount(any())).thenReturn(0L);
        service.update(1L, bo("T1", "SALE_DELIVERY", 0));
        assertEquals(4, existing.getVersion());
        verify(templateMapper).updateById(existing);
    }

    @Test
    @DisplayName("编辑：内容含恶意外链被清洗")
    void updateSanitizes() {
        PrintTemplate existing = new PrintTemplate();
        existing.setId(1L);
        existing.setCode("T1");
        existing.setScene("SALE_DELIVERY");
        existing.setVersion(1);
        when(templateMapper.selectById(1L)).thenReturn(existing);
        when(templateMapper.selectCount(any())).thenReturn(0L);
        PrintTemplateBo b = bo("T1", "SALE_DELIVERY", 0);
        b.setContent("<div>x</div><img src=\"http://evil.com/a.png\">");
        service.update(1L, b);
        assertEquals(-1, existing.getContent().indexOf("evil.com"));
    }

    @Test
    @DisplayName("停用：置 status=0，不物理删除")
    void disableSetsStatus() {
        PrintTemplate existing = new PrintTemplate();
        existing.setId(1L);
        existing.setStatus(1);
        when(templateMapper.selectById(1L)).thenReturn(existing);
        service.disable(1L);
        assertEquals(0, existing.getStatus());
        verify(templateMapper).updateById(existing);
        verify(templateMapper, never()).deleteById(1L);
    }

    @Test
    @DisplayName("resolve：指定 templateId 停用则拒绝")
    void resolveDisabledTemplateRejected() {
        PrintTemplate t = new PrintTemplate();
        t.setId(5L);
        t.setStatus(0);
        when(templateMapper.selectById(5L)).thenReturn(t);
        assertThrows(ServiceException.class, () -> service.resolve(5L, "SALE_DELIVERY", null));
    }

    @Test
    @DisplayName("resolve：客户专属优先于默认")
    void resolveCustomerSpecificFirst() {
        when(templateMapper.selectList(any())).thenReturn(List.of());
        assertEquals(null, service.resolve(null, "SALE_DELIVERY", 7L));
    }

    @Test
    @DisplayName("resolve：无模板返回 null（调用方用内置兜底）")
    void resolveReturnsNullWhenNone() {
        when(templateMapper.selectList(any())).thenReturn(List.of());
        assertEquals(null, service.resolve(null, "SALE_DELIVERY", null));
    }

    @Test
    @DisplayName("resolve：命中默认")
    void resolveHitsDefault() {
        PrintTemplate def = new PrintTemplate();
        def.setId(8L);
        def.setScene("STATEMENT");
        def.setIsDefault(1);
        def.setStatus(1);
        when(templateMapper.selectList(any())).thenReturn(List.of(def));
        assertEquals(8L, service.resolve(null, "STATEMENT", 7L).getId());
    }
}
