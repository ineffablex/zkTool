package com.mytool.zktool.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI zkToolOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ZooKeeper配置管理工具 API")
                        .description("提供ZooKeeper节点的管理和操作功能的RESTful API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ZkTool Team")
                                .email("support@zktool.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}