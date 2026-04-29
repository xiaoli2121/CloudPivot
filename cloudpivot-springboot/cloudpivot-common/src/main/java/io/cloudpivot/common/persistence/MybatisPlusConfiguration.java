package io.cloudpivot.common.persistence;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;

@Configuration
@MapperScan(basePackages = {
        "io.cloudpivot.auth.persistence.mapper",
        "io.cloudpivot.iam.persistence.mapper",
        "io.cloudpivot.system.persistence.mapper",
        "io.cloudpivot.metadata.persistence.mapper",
        "io.cloudpivot.plugin.persistence.mapper"
})
public class MybatisPlusConfiguration {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        return new MybatisPlusInterceptor();
    }
}
