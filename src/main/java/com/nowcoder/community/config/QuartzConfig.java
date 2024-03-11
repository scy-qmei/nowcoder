package com.nowcoder.community.config;

import com.nowcoder.community.jog.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

/**
 * quartz的配置类，用来配置job的详细信息以及对应的触发器！
 */
@Configuration
public class QuartzConfig {

    //设置job的详细信息
    @Bean
    public JobDetailFactoryBean postScoreRefreshJob() {
        JobDetailFactoryBean jobDetailFactoryBean = new JobDetailFactoryBean();
        jobDetailFactoryBean.setJobClass(PostScoreRefreshJob.class);
        //job是否长期存储
        jobDetailFactoryBean.setDurability(true);
        jobDetailFactoryBean.setName("postScoreRefreshJob");
        jobDetailFactoryBean.setGroup("communityJob");
        jobDetailFactoryBean.setRequestsRecovery(true);

        return jobDetailFactoryBean;
    }
    //设置job对应的触发器
    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(/*注入我们通过factoryBean实例化的jobdetailbean*/
            JobDetail postScoreRefreshJob) {
        SimpleTriggerFactoryBean simpleTriggerFactoryBean = new SimpleTriggerFactoryBean();

        simpleTriggerFactoryBean.setJobDetail(postScoreRefreshJob);
        simpleTriggerFactoryBean.setName("postScoreRefreshTrigger");
        simpleTriggerFactoryBean.setGroup("communityTrigger");
        //五分钟触发一次任务
        simpleTriggerFactoryBean.setRepeatInterval(1000 * 60 * 5);
        //存储job运行的数据
        simpleTriggerFactoryBean.setJobDataMap(new JobDataMap());
        return simpleTriggerFactoryBean;
    }
}
