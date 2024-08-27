package org.example.config;

import lombok.extern.slf4j.Slf4j;
import org.example.task.MonitorJobBean;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置定时任务
 */
@Slf4j
@Configuration
public class QuartzConfiguration {
    @Bean
    public JobDetail jobDetailFactoryBean() {
        return JobBuilder.newJob(MonitorJobBean.class)
                .withIdentity("monitor-task")
                .storeDurably() //持久化存贮
                .build();
    }

    // 定时任务1的具体执行
    @Bean
    public Trigger cronTriggerFactoryBean(JobDetail detail) {
        CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule("*/10 * * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(detail)
                .withIdentity("monitor-trigger")
                .withSchedule(cron)
                .build();
    }
}
