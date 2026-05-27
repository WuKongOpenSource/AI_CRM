package com.kakarote.syncdata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sync")
public class SyncProperties {

    @Min(1)
    private int batchSize = 500;

    private boolean dryRun;

    private boolean truncateBeforeSync;

    private boolean populateSearchIndex = true;

    @NotBlank
    private String resetPassword = "ChangeMe@123";

    @Valid
    private DataSourceProperties oldCrm = new DataSourceProperties();

    @Valid
    private DataSourceProperties target = new DataSourceProperties();

    @Valid
    private IncrementalProperties incremental = new IncrementalProperties();

    @Valid
    private RocketMqProperties rocketmq = new RocketMqProperties();

    /**
     * 获取分页批量大小。
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 设置分页批量大小。
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * 判断是否只做源库统计而不写目标库。
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * 设置是否启用 dry-run 模式。
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * 判断同步前是否删除当前映射创建过的目标数据。
     */
    public boolean isTruncateBeforeSync() {
        return truncateBeforeSync;
    }

    /**
     * 设置同步前是否删除当前映射创建过的目标数据。
     */
    public void setTruncateBeforeSync(boolean truncateBeforeSync) {
        this.truncateBeforeSync = truncateBeforeSync;
    }

    /**
     * 判断同步完成后是否刷新全局搜索索引。
     */
    public boolean isPopulateSearchIndex() {
        return populateSearchIndex;
    }

    /**
     * 设置同步完成后是否刷新全局搜索索引。
     */
    public void setPopulateSearchIndex(boolean populateSearchIndex) {
        this.populateSearchIndex = populateSearchIndex;
    }

    /**
     * 获取迁移用户写入的新默认密码。
     */
    public String getResetPassword() {
        return resetPassword;
    }

    /**
     * 设置迁移用户写入的新默认密码。
     */
    public void setResetPassword(String resetPassword) {
        this.resetPassword = resetPassword;
    }

    /**
     * 获取老 WK CRM 数据源配置。
     */
    public DataSourceProperties getOldCrm() {
        return oldCrm;
    }

    /**
     * 设置老 WK CRM 数据源配置。
     */
    public void setOldCrm(DataSourceProperties oldCrm) {
        this.oldCrm = oldCrm;
    }

    /**
     * 获取目标 AI CRM 数据源配置。
     */
    public DataSourceProperties getTarget() {
        return target;
    }

    /**
     * 设置目标 AI CRM 数据源配置。
     */
    public void setTarget(DataSourceProperties target) {
        this.target = target;
    }

    /**
     * 获取增量同步配置。
     */
    public IncrementalProperties getIncremental() {
        return incremental;
    }

    /**
     * 设置增量同步配置。
     */
    public void setIncremental(IncrementalProperties incremental) {
        this.incremental = incremental;
    }

    public RocketMqProperties getRocketmq() {
        return rocketmq;
    }

    public void setRocketmq(RocketMqProperties rocketmq) {
        this.rocketmq = rocketmq;
    }

    public static class DataSourceProperties {
        @NotBlank
        private String jdbcUrl;

        @NotBlank
        private String username;

        private String password;

        @Min(1)
        private int maximumPoolSize = 5;

        /**
         * 获取 JDBC 地址。
         */
        public String getJdbcUrl() {
            return jdbcUrl;
        }

        /**
         * 设置 JDBC 地址。
         */
        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        /**
         * 获取数据库账号。
         */
        public String getUsername() {
            return username;
        }

        /**
         * 设置数据库账号。
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * 获取数据库密码。
         */
        public String getPassword() {
            return password;
        }

        /**
         * 设置数据库密码。
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * 获取连接池最大连接数。
         */
        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        /**
         * 设置连接池最大连接数。
         */
        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }
    }

    public static class IncrementalProperties {
        @Valid
        private MqProperties mq = new MqProperties();

        /**
         * 获取 MQ 增量同步配置。
         */
        public MqProperties getMq() {
            return mq;
        }

        /**
         * 设置 MQ 增量同步配置。
         */
        public void setMq(MqProperties mq) {
            this.mq = mq;
        }
    }

    public static class MqProperties {
        private boolean enabled;
        private String topic = "wk-crm-binlog";
        private String consumerGroup = "ai-crm-sync-data";

        /**
         * 判断是否启用 MQ 增量同步预留入口。
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用 MQ 增量同步预留入口。
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取 MQ topic。
         */
        public String getTopic() {
            return topic;
        }

        /**
         * 设置 MQ topic。
         */
        public void setTopic(String topic) {
            this.topic = topic;
        }

        /**
         * 获取 MQ consumer group。
         */
        public String getConsumerGroup() {
            return consumerGroup;
        }

        /**
         * 设置 MQ consumer group。
         */
        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }
    }

    public static class RocketMqProperties {
        private boolean enabled;
        private String nameServer = "127.0.0.1:9876";
        private String topic = "crm-aicrm-sync-events";

        @Valid
        private DirectionConsumerProperties crmToAicrm = new DirectionConsumerProperties(
                null, "sync-data-crm-to-aicrm", "crm-to-aicrm");

        @Valid
        private DirectionProducerProperties aicrmToCrm = new DirectionProducerProperties(
                null, "sync-data-aicrm-to-crm", "aicrm-to-crm");

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getNameServer() {
            return nameServer;
        }

        public void setNameServer(String nameServer) {
            this.nameServer = nameServer;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public DirectionConsumerProperties getCrmToAicrm() {
            return crmToAicrm;
        }

        public void setCrmToAicrm(DirectionConsumerProperties crmToAicrm) {
            this.crmToAicrm = crmToAicrm;
        }

        public DirectionProducerProperties getAicrmToCrm() {
            return aicrmToCrm;
        }

        public void setAicrmToCrm(DirectionProducerProperties aicrmToCrm) {
            this.aicrmToCrm = aicrmToCrm;
        }
    }

    public static class DirectionConsumerProperties {
        private String topic;
        private String consumerGroup;
        private String tag = "crm-to-aicrm";

        public DirectionConsumerProperties() {
        }

        public DirectionConsumerProperties(String topic, String consumerGroup) {
            this(topic, consumerGroup, "crm-to-aicrm");
        }

        public DirectionConsumerProperties(String topic, String consumerGroup, String tag) {
            this.topic = topic;
            this.consumerGroup = consumerGroup;
            this.tag = tag;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }
    }

    public static class DirectionProducerProperties {
        private String topic;
        private String producerGroup;
        private String tag = "aicrm-to-crm";

        public DirectionProducerProperties() {
        }

        public DirectionProducerProperties(String topic, String producerGroup) {
            this(topic, producerGroup, "aicrm-to-crm");
        }

        public DirectionProducerProperties(String topic, String producerGroup, String tag) {
            this.topic = topic;
            this.producerGroup = producerGroup;
            this.tag = tag;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getProducerGroup() {
            return producerGroup;
        }

        public void setProducerGroup(String producerGroup) {
            this.producerGroup = producerGroup;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }
    }
}
