package uk.gov.ons.census.casesvc.config;

import static org.springframework.amqp.core.Binding.DestinationType.QUEUE;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueSetterUpper {
  @Value("${queueconfig.inbound-queue}")
  private String inboundQueue;

  @Value("${queueconfig.case-event-exchange}")
  private String caseEventExchange;

  @Value("${queueconfig.rh-case-queue}")
  private String rhCaseQueue;

  @Value("${queueconfig.rh-case-routing-key}")
  private String rhCaseRoutingKey;

  @Value("${queueconfig.rh-uac-queue}")
  private String rhUacQueue;

  @Value("${queueconfig.rh-uac-routing-key}")
  private String rhUacRoutingKey;

  @Value("${queueconfig.action-scheduler-queue}")
  private String actionSchedulerQueue;

  @Value("${queueconfig.action-scheduler-routing-key-uac}")
  private String actionSchedulerRoutingKeyUac;

  @Value("${queueconfig.action-scheduler-routing-key-case}")
  private String actionSchedulerRoutingKeyCase;

  @Value("${queueconfig.refusal-routing-key}")
  private String caseProcessorRefusalRoutingKeyCase;

  @Value("${queueconfig.unaddressed-inbound-queue}")
  private String unaddressedQueue;

  @Value("${queueconfig.receipt-response-inbound-queue}")
  private String receiptInboundQueue;

  @Value("${queueconfig.refusal-response-inbound-queue}")
  private String refusalInboundQueue;

  @Value("${queueconfig.action-case-queue}")
  private String actionCaseQueue;

  @Bean
  public Queue inboundQueue() {
    return new Queue(inboundQueue, true);
  }

  @Bean
  public Queue unaddressedQueue() {
    return new Queue(unaddressedQueue, true);
  }

  @Bean
  public Queue rhCaseQueue() {
    return new Queue(rhCaseQueue, true);
  }

  @Bean
  public Queue rhUacQueue() {
    return new Queue(rhUacQueue, true);
  }

  @Bean
  public Queue actionSchedulerQueue() {
    return new Queue(actionSchedulerQueue, true);
  }

  @Bean
  public Exchange myTopicExchange() {
    return new TopicExchange(caseEventExchange, true, false);
  }

  @Bean
  public Binding bindingRhCase() {
    return new Binding(rhCaseQueue, QUEUE, caseEventExchange, rhCaseRoutingKey, null);
  }

  @Bean
  public Binding bindingRhUac() {
    return new Binding(rhUacQueue, QUEUE, caseEventExchange, rhUacRoutingKey, null);
  }

  @Bean
  public Binding bindingActionUac() {
    return new Binding(
        actionSchedulerQueue, QUEUE, caseEventExchange, actionSchedulerRoutingKeyUac, null);
  }

  @Bean
  public Binding bindingActionCase() {
    return new Binding(
        actionSchedulerQueue, QUEUE, caseEventExchange, actionSchedulerRoutingKeyCase, null);
  }

  @Bean
  public Binding bindingRefusalCase() {
    return new Binding(
        refusalInboundQueue, QUEUE, caseEventExchange, caseProcessorRefusalRoutingKeyCase, null);
  }

  @Bean
  public Queue receiptInboundQueue() {
    return new Queue(receiptInboundQueue, true);
  }

  @Bean
  public Queue refusalInboundQueue() {
    return new Queue(refusalInboundQueue, true);
  }

  @Bean
  public Queue actionCaseQueue() {
    return new Queue(actionCaseQueue, true);
  }
}
