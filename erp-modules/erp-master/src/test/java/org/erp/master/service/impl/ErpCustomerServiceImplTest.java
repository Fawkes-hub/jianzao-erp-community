package org.erp.master.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.extra.spring.SpringUtil;
import io.github.linpeilie.Converter;
import org.dromara.common.core.exception.ServiceException;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.domain.bo.ErpCustomerBo;
import org.erp.master.mapper.ErpCustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.GenericApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ErpCustomerServiceImpl 编码自动生成测试
 */
@Tag("dev")
class ErpCustomerServiceImplTest {

    private static final Converter TEST_CONVERTER = new Converter() {
        @Override
        public <S, T> T convert(S source, Class<T> targetType) {
            T target = BeanUtil.toBean(source, targetType);
            return target;
        }
    };

    static {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getBeanFactory().registerSingleton("converter", TEST_CONVERTER);
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }

    private ErpCustomerMapper mapper;
    private ErpCustomerServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(ErpCustomerMapper.class);
        service = new ErpCustomerServiceImpl(mapper);
    }

    @Test
    void insertBlankCodeGeneratesFromName() {
        when(mapper.selectCount(any())).thenReturn(0L);
        ErpCustomerBo bo = new ErpCustomerBo();
        bo.setName("远达机械");

        service.insert(bo);

        assertEquals("YDJX", captureInserted().getCode());
    }

    @Test
    void insertDuplicateCodeAppendsSequence() {
        when(mapper.selectCount(any())).thenReturn(1L, 0L, 0L);
        ErpCustomerBo bo = new ErpCustomerBo();
        bo.setName("远达机械");

        service.insert(bo);

        assertEquals("YDJX-2", captureInserted().getCode());
    }

    @Test
    void insertKeepsManuallyEnteredCode() {
        when(mapper.selectCount(any())).thenReturn(0L);
        ErpCustomerBo bo = new ErpCustomerBo();
        bo.setName("远达机械");
        bo.setCode("C001");

        service.insert(bo);

        assertEquals("C001", captureInserted().getCode());
    }

    @Test
    void updateBlankCodeKeepsExisting() {
        when(mapper.selectCount(any())).thenReturn(0L);
        ErpCustomer existing = new ErpCustomer();
        existing.setId(9L);
        existing.setCode("OLD");
        when(mapper.selectById(9L)).thenReturn(existing);
        ErpCustomerBo bo = new ErpCustomerBo();
        bo.setId(9L);
        bo.setName("远达机械");

        service.update(bo);

        ArgumentCaptor<ErpCustomer> captor = ArgumentCaptor.forClass(ErpCustomer.class);
        verify(mapper).updateById(captor.capture());
        assertEquals("OLD", captor.getValue().getCode());
    }

    @Test
    void updateWithoutIdRejected() {
        assertThrows(ServiceException.class, () -> service.update(new ErpCustomerBo()));
    }

    private ErpCustomer captureInserted() {
        ArgumentCaptor<ErpCustomer> captor = ArgumentCaptor.forClass(ErpCustomer.class);
        verify(mapper).insert(captor.capture());
        return captor.getValue();
    }
}
