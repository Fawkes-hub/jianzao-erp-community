package org.dromara.common.oss.constant;

import org.dromara.common.core.constant.GlobalConstants;

import java.util.Arrays;
import java.util.List;

/**
 * 对象存储常量
 *
 * @author Lion Li
 */
public interface OssConstant {

    /**
     * 默认配置KEY
     */
    String DEFAULT_CONFIG_KEY = GlobalConstants.GLOBAL_REDIS_KEY + "sys_oss:default_config";

    /**
     * 预览列表资源开关Key
     */
    String PEREVIEW_LIST_RESOURCE_KEY = "sys.oss.previewListResource";

    /**
     * 系统数据ids
     */
    List<Long> SYSTEM_DATA_IDS = Arrays.asList(1L, 2L, 3L, 4L);

    /**
     * 云服务商（endpoint 命中任意关键字即按云厂商规则处理：虚拟主机样式 {@code <bucket>.<endpoint>}、无需路径样式）
     * <p>bitiful：缤纷云 S4，endpoint 为 s3.bitiful.net，网关只接受虚拟主机样式，故必须纳入本清单</p>
     */
    String[] CLOUD_SERVICE = new String[] {"aliyun", "qcloud", "qiniu", "obs", "bitiful"};

    /**
     * https 状态
     */
    String IS_HTTPS = "Y";

}
