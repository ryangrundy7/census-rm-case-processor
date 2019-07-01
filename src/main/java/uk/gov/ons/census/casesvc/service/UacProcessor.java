package uk.gov.ons.census.casesvc.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.casesvc.client.UacQidServiceClient;
import uk.gov.ons.census.casesvc.model.dto.Event;
import uk.gov.ons.census.casesvc.model.dto.EventType;
import uk.gov.ons.census.casesvc.model.dto.Payload;
import uk.gov.ons.census.casesvc.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.casesvc.model.dto.Uac;
import uk.gov.ons.census.casesvc.model.dto.UacQidDTO;
import uk.gov.ons.census.casesvc.model.entity.Case;
import uk.gov.ons.census.casesvc.model.entity.UacQidLink;
import uk.gov.ons.census.casesvc.model.repository.EventRepository;
import uk.gov.ons.census.casesvc.model.repository.UacQidLinkRepository;
import uk.gov.ons.census.casesvc.utility.EventHelper;
import uk.gov.ons.census.casesvc.utility.Sha256Helper;

@Component
public class UacProcessor {

  private static final String UAC_UPDATE_ROUTING_KEY = "event.uac.update";

  private final UacQidLinkRepository uacQidLinkRepository;
  private final EventRepository eventRepository;
  private final RabbitTemplate rabbitTemplate;
  private final UacQidServiceClient uacQidServiceClient;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  public UacProcessor(
      UacQidLinkRepository uacQidLinkRepository,
      EventRepository eventRepository,
      RabbitTemplate rabbitTemplate,
      UacQidServiceClient uacQidServiceClient) {
    this.rabbitTemplate = rabbitTemplate;
    this.uacQidServiceClient = uacQidServiceClient;
    this.uacQidLinkRepository = uacQidLinkRepository;
    this.eventRepository = eventRepository;
  }

  public UacQidLink saveUacQidLink(Case caze, int questionnaireType) {
    return saveUacQidLink(caze, questionnaireType, null);
  }

  public UacQidLink saveUacQidLink(Case caze, int questionnaireType, UUID batchId) {
    UacQidDTO uacQid = uacQidServiceClient.generateUacQid(questionnaireType);

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setUac(uacQid.getUac());
    uacQidLink.setCaze(caze);
    uacQidLink.setBatchId(batchId);
    uacQidLink.setActive(true);

    uacQidLink.setQid(uacQid.getQid());
    uacQidLinkRepository.save(uacQidLink);

    return uacQidLink;
  }

  public void logEvent(
      UacQidLink uacQidLink,
      String eventDescription,
      uk.gov.ons.census.casesvc.model.entity.EventType eventType) {
    logEvent(uacQidLink, eventDescription, eventType, null);
  }

  public void logEvent(
      UacQidLink uacQidLink,
      String eventDescription,
      uk.gov.ons.census.casesvc.model.entity.EventType eventType,
      OffsetDateTime eventMetaDataDateTime) {
    uk.gov.ons.census.casesvc.model.entity.Event loggedEvent =
        new uk.gov.ons.census.casesvc.model.entity.Event();
    loggedEvent.setId(UUID.randomUUID());

    if (eventMetaDataDateTime != null) {
      loggedEvent.setEventDate(eventMetaDataDateTime);
    }

    loggedEvent.setEventDate(OffsetDateTime.now());
    loggedEvent.setRmEventProcessed(OffsetDateTime.now());
    loggedEvent.setEventDescription(eventDescription);
    loggedEvent.setUacQidLink(uacQidLink);
    loggedEvent.setEventType(eventType);
    eventRepository.save(loggedEvent);
  }

  public void emitUacUpdatedEvent(UacQidLink uacQidLink, Case caze) {
    emitUacUpdatedEvent(uacQidLink, caze, true);
  }

  public void emitUacUpdatedEvent(UacQidLink uacQidLink, Case caze, boolean active) {
    Event event = EventHelper.createEvent(EventType.UAC_UPDATED);

    Uac uac = new Uac();
    uac.setQuestionnaireId(uacQidLink.getQid());
    uac.setUacHash(Sha256Helper.hash(uacQidLink.getUac()));
    uac.setUac(uacQidLink.getUac());
    uac.setActive(active);

    if (caze != null) {
      uac.setCaseId(caze.getCaseId().toString());
      uac.setCaseType(caze.getAddressType());
      uac.setCollectionExerciseId(caze.getCollectionExerciseId());
      uac.setRegion(caze.getRegion());
    }

    Payload payload = new Payload();
    payload.setUac(uac);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setEvent(event);
    responseManagementEvent.setPayload(payload);

    rabbitTemplate.convertAndSend(
        outboundExchange, UAC_UPDATE_ROUTING_KEY, responseManagementEvent);
  }
}
