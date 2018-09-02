package sleuth.webmvc;

import brave.Tracing;
import brave.jms.JmsTracing;
import brave.propagation.CurrentTraceContext;
import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListenerConfigurer;

@Configuration
class JmsConfiguration {

  @Bean JmsTracing jmsTracing(Tracing tracing) {
    return JmsTracing.create(tracing);
  }

  // Setup basic JMS functionality
  @Bean BeanPostProcessor connectionFactoryDecorator(JmsTracing jmsTracing) {
    return new BeanPostProcessor() {
      @Override public Object postProcessAfterInitialization(Object bean, String beanName)
          throws BeansException {
        if (jmsTracing == null) return bean;
        if (bean instanceof ConnectionFactory) {
          return jmsTracing.connectionFactory((ConnectionFactory) bean);
        } else if (bean instanceof XAConnectionFactory) {
          return jmsTracing.xaConnectionFactory((XAConnectionFactory) bean);
        }
        return bean;
      }
    };
  }

  /** Choose the tracing endpoint registry */
  @Bean
  TracingJmsListenerEndpointRegistry registry(JmsTracing jmsTracing, CurrentTraceContext current) {
    return new TracingJmsListenerEndpointRegistry(jmsTracing, current);
  }

  /** Setup the tracing endpoint registry */
  @Bean
  public JmsListenerConfigurer configureTracing(TracingJmsListenerEndpointRegistry registry) {
    return registrar -> registrar.setEndpointRegistry(registry);
  }
}
