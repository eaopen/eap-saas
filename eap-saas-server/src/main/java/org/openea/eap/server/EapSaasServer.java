package org.openea.eap.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 项目的启动类
 */
@ComponentScan(basePackages={"org.openea.eap"
        //,"org.openbpm",
        //,"${eap.info.base-package}.server", "${eap.info.base-package}.module"
},
        excludeFilters={
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        org.openea.eap.framework.web.core.handler.GlobalExceptionHandler.class
                })
        })
@SuppressWarnings("SpringComponentScan") // 忽略 IDEA 无法识别 ${eap.info.base-package}
@SpringBootApplication(scanBasePackages = {"${eap.info.base-package}.server", "${eap.info.base-package}.module"})
public class EapSaasServer {

    public static void main(String[] args) {

        SpringApplication.run(EapSaasServer.class, args);
//        new SpringApplicationBuilder(EapServerApplication.class)
//                .applicationStartup(new BufferingApplicationStartup(20480))
//                .run(args);

    }

}
