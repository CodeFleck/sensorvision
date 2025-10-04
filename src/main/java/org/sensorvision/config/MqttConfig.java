package org.sensorvision.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.sensorvision.config.MqttProperties.Broker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

@Configuration
@EnableConfigurationProperties({MqttProperties.class, SimulatorProperties.class})
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqttConfig {

    @Bean
    public MqttConnectOptions mqttConnectOptions(MqttProperties properties) {
        Broker broker = properties.getBroker();
        Assert.hasText(broker.getUrl(), "mqtt.broker.url must be provided");
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{broker.getUrl()});
        if (broker.getUsername() != null) {
            options.setUserName(broker.getUsername());
        }
        if (broker.getPassword() != null) {
            options.setPassword(broker.getPassword().toCharArray());
        }
        options.setAutomaticReconnect(true);
        options.setCleanSession(broker.isCleanSession());
        return options;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttConnectOptions options) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttInboundAdapter(MqttProperties properties,
                                              MqttPahoClientFactory mqttClientFactory,
                                              MessageChannel mqttInboundChannel) {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                properties.getClientId() + "-inbound",
                mqttClientFactory,
                "sensorvision/devices/+/telemetry"
        );
        adapter.setCompletionTimeout(properties.getBroker().getCompletionTimeout());
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInboundChannel);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutboundHandler(MqttProperties properties, MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(
                properties.getClientId() + "-outbound",
                mqttClientFactory
        );
        handler.setAsync(true);
        handler.setDefaultTopic("sensorvision/devices/broadcast/commands");
        handler.setDefaultQos(1);
        return handler;
    }
}