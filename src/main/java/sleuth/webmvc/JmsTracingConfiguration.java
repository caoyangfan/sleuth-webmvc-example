package sleuth.webmvc;

import brave.Tracing;
import brave.jms.JmsTracing;
import brave.propagation.CurrentTraceContext;
import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.connection.CachingConnectionFactory;

@Configuration
class JmsTracingConfiguration {

  @Bean JmsTracing jmsTracing(Tracing tracing) {
    return JmsTracing.create(tracing);
  }

  // Setup basic JMS functionality
  @Bean BeanPostProcessor connectionFactoryDecorator(BeanFactory beanFactory) {
    return new BeanPostProcessor() {
      @Override public Object postProcessAfterInitialization(Object bean, String beanName)
          throws BeansException {

        // lazy lookup JmsTracing so that the BPP doesn't end up needing to proxy anything.
        JmsTracing jmsTracing;
        try {
          jmsTracing = beanFactory.getBean(JmsTracing.class);
        } catch (BeansException e) {
          return bean; // graceful on failure for any reason.
        }

        // Wrap the caching connection factories instead of its target, because it catches callbacks
        // such as ExceptionListener. If we don't wrap, cached callbacks like this won't be traced.
        if (bean instanceof CachingConnectionFactory) {
          return jmsTracing.connectionFactory((CachingConnectionFactory) bean);
        }
        // We check XA first in case the ConnectionFactory also implements XAConnectionFactory
        if (bean instanceof XAConnectionFactory) {
          return jmsTracing.xaConnectionFactory((XAConnectionFactory) bean);
        } else if (bean instanceof ConnectionFactory) {
          return jmsTracing.connectionFactory((ConnectionFactory) bean);
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
