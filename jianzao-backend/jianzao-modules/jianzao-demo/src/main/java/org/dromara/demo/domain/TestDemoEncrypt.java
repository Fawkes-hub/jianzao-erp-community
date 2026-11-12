package org.dromara.demo.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.encrypt.annotation.EncryptField;
import org.dromara.common.encrypt.enumd.AlgorithmType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_demo")
public class TestDemoEncrypt extends TestDemo {

    /**
     * key键
     */
    // @EncryptField(algorithm=AlgorithmType.SM2, privateKey = "REPLACE_WITH_YOUR_PRIVATE_KEY", publicKey = "REPLACE_WITH_YOUR_PUBLIC_KEY")
    @EncryptField(algorithm = AlgorithmType.RSA, privateKey = "REPLACE_WITH_YOUR_PRIVATE_KEY", publicKey = "REPLACE_WITH_YOUR_PUBLIC_KEY")
    private String testKey;

    /**
     * 值
     */
    // @EncryptField // 什么也不写走默认yml配置
    // @EncryptField(algorithm = AlgorithmType.SM4, password = "10rfylhtccpuyke5")
    @EncryptField(algorithm = AlgorithmType.AES, password = "10rfylhtccpuyke5")
    private String value;

}
