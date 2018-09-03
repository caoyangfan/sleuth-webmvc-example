package sleuth.webmvc;

import brave.Span;
import brave.jms.JmsTracing;
import brave.propagation.CurrentTraceContext;
import javax.jms.Message;
import javax.jms.MessageListener;

/** This wraps the message listener in a child span */
final class TracingMessageListener implements MessageListener {

  final MessageListener delegate;
  final JmsTracing jmsTracing;
  final CurrentTraceContext current;

  TracingMessageListener(MessageListener delegate, JmsTracing jmsTracing,
      CurrentTraceContext current) {
    this.delegate = delegate;
    this.jmsTracing = jmsTracing;
    this.current = current;
  }

  @Override public void onMessage(Message message) {
    Span span = jmsTracing.nextSpan(message).name("on-message").start();
    try (CurrentTraceContext.Scope ws = current.newScope(span.context())) {
      delegate.onMessage(message);
    } catch (RuntimeException | Error e) {
      span.error(e);
      throw e;
    } finally {
      span.finish();
    }
  }
}
