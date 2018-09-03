package sleuth.webmvc;

import java.util.Date;
import javax.jms.Queue;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;

@EnableAutoConfiguration
@EnableJms
@Import(JmsTracingConfiguration.class)
public class Backend {

  @JmsListener(destination = "backend")
  public void onMessage() {
    System.err.println(new Date().toString());
  }

  @Bean public Queue queue() {
    return new ActiveMQQueue("backend");
  }

  public static void main(String[] args) {
    SpringApplication.run(Backend.class,
        "--spring.application.name=backend",
        "--server.port=9000"
    );
  }
}
