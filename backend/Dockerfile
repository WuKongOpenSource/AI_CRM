FROM registry.cn-beijing.aliyuncs.com/72crm/wk_base:20250331

MAINTAINER wukongcrm

RUN mkdir -p /opt/wk_ai_crm

WORKDIR /opt/wk_ai_crm

ADD ./target/crm-1.0.0.jar ./

ENV JAVA_OPTS="-Dons.client.logLevel=ERROR -XX:+ShowCodeDetailsInExceptionMessages -XX:MaxRAMPercentage=75.0 --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED"

CMD java $JAVA_OPTS -jar crm-1.0.0.jar
