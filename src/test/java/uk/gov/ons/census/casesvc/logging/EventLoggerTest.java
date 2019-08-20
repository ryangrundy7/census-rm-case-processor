package uk.gov.ons.census.casesvc.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static uk.gov.ons.census.casesvc.testutil.DataUtils.convertJsonToFulfilmentRequestDTO;
import static uk.gov.ons.census.casesvc.testutil.DataUtils.convertJsonToRefusalDTO;

import java.util.UUID;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.census.casesvc.model.dto.EventDTO;
import uk.gov.ons.census.casesvc.model.dto.FulfilmentRequestDTO;
import uk.gov.ons.census.casesvc.model.dto.RefusalDTO;
import uk.gov.ons.census.casesvc.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.casesvc.model.entity.Case;
import uk.gov.ons.census.casesvc.model.entity.Event;
import uk.gov.ons.census.casesvc.model.entity.EventType;
import uk.gov.ons.census.casesvc.model.entity.UacQidLink;
import uk.gov.ons.census.casesvc.model.repository.EventRepository;

@RunWith(MockitoJUnitRunner.class)
public class EventLoggerTest {

  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final String TEST_JSON = "{test: json}";

  @Mock EventRepository eventRepository;

  @InjectMocks EventLogger underTest;

  private final EasyRandom easyRandom = new EasyRandom();

  @Test
  public void testLogEvent() {
    // Given
    UacQidLink uacQidLink = new UacQidLink();
    Case caze = new Case();
    UUID caseUuid = UUID.randomUUID();
    caze.setCaseId(caseUuid);
    uacQidLink.setCaze(caze);

    // When
    underTest.logEvent(uacQidLink, "TEST_LOGGED_EVENT", EventType.UAC_UPDATED, TEST_JSON);

    // Then
    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    assertEquals("TEST_LOGGED_EVENT", eventArgumentCaptor.getValue().getEventDescription());
    assertEquals(EventType.UAC_UPDATED, eventArgumentCaptor.getValue().getEventType());
  }

  @Test
  public void testLogEventAddressed() {
    // Given
    UacQidLink uacQidLink = new UacQidLink();
    Case caze = new Case();
    UUID caseUuid = UUID.randomUUID();
    caze.setCaseId(caseUuid);
    uacQidLink.setCaze(caze);

    // When
    underTest.logEvent(uacQidLink, "TEST_LOGGED_EVENT", EventType.UAC_UPDATED, "", new EventDTO());

    // Then
    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    assertThat(eventArgumentCaptor.getValue().getCaseId()).isEqualTo(caseUuid);
  }

  @Test
  public void testLogEventUnaddressed() {
    // Given
    UacQidLink uacQidLink = new UacQidLink();

    // When
    underTest.logEvent(uacQidLink, "TEST_LOGGED_EVENT", EventType.UAC_UPDATED, "", new EventDTO());

    // Then
    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    assertThat(eventArgumentCaptor.getValue().getCaseId()).isNull();
  }

  @Test
  public void testLogRefusalEventWithTransactionId() {
    // Given
    RefusalDTO expectedRefusal = easyRandom.nextObject(RefusalDTO.class);
    expectedRefusal.getCollectionCase().setId(TEST_CASE_ID.toString());
    EventDTO event = new EventDTO();
    event.setTransactionId(UUID.randomUUID());

    // When
    underTest.logRefusalEvent(
        new Case(), "TEST_LOGGED_EVENT", EventType.REFUSAL_RECEIVED, expectedRefusal, event);

    // Then
    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    RefusalDTO actualRefusal =
        convertJsonToRefusalDTO(eventArgumentCaptor.getValue().getEventPayload());

    assertThat(actualRefusal.getType()).isEqualTo(expectedRefusal.getType());
    assertThat(actualRefusal.getReport()).isEqualTo(expectedRefusal.getReport());
    assertThat(actualRefusal.getAgentId()).isEqualTo(expectedRefusal.getAgentId());
    assertThat(actualRefusal.getCollectionCase().getId())
        .isEqualTo(expectedRefusal.getCollectionCase().getId());
  }

  @Test
  public void testLogRefusalEventWithoutTransactionId() {
    // Given
    RefusalDTO expectedRefusal = easyRandom.nextObject(RefusalDTO.class);
    expectedRefusal.getCollectionCase().setId(TEST_CASE_ID.toString());

    // When
    underTest.logRefusalEvent(
        new Case(),
        "TEST_LOGGED_EVENT",
        EventType.REFUSAL_RECEIVED,
        expectedRefusal,
        new EventDTO());

    // Then
    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    RefusalDTO actualRefusal =
        convertJsonToRefusalDTO(eventArgumentCaptor.getValue().getEventPayload());

    assertThat(actualRefusal.getType()).isEqualTo(expectedRefusal.getType());
    assertThat(actualRefusal.getReport()).isEqualTo(expectedRefusal.getReport());
    assertThat(actualRefusal.getAgentId()).isEqualTo(expectedRefusal.getAgentId());
    assertThat(actualRefusal.getCollectionCase().getId())
        .isEqualTo(expectedRefusal.getCollectionCase().getId());
  }

  @Test
  public void testLogFulfilmentRequestEventWithTransactionId() {
    // Given
    ResponseManagementEvent managementEvent = easyRandom.nextObject(ResponseManagementEvent.class);
    EventDTO fulfilmentRequestEvent = managementEvent.getEvent();
    FulfilmentRequestDTO fulfilmentRequestPayload =
        managementEvent.getPayload().getFulfilmentRequest();
    fulfilmentRequestPayload.setCaseId(UUID.randomUUID().toString());
    fulfilmentRequestEvent.setTransactionId(UUID.randomUUID());

    // When
    underTest.logFulfilmentRequestedEvent(
        new Case(),
        UUID.fromString(fulfilmentRequestPayload.getCaseId()),
        fulfilmentRequestEvent.getDateTime(),
        "Fulfilment Request Received",
        EventType.FULFILMENT_REQUESTED,
        fulfilmentRequestPayload,
        fulfilmentRequestEvent);

    // Then
    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    FulfilmentRequestDTO actualFulfilment =
        convertJsonToFulfilmentRequestDTO(eventArgumentCaptor.getValue().getEventPayload());

    assertThat(actualFulfilment.getCaseId()).isEqualTo(fulfilmentRequestPayload.getCaseId());
    assertThat(actualFulfilment.getFulfilmentCode())
        .isEqualTo(fulfilmentRequestPayload.getFulfilmentCode());
  }

  @Test
  public void testLogFulfilmentRequestEventWithoutTransactionId() {
    // Given
    ResponseManagementEvent managementEvent = easyRandom.nextObject(ResponseManagementEvent.class);
    EventDTO fulfilmentRequestEvent = managementEvent.getEvent();
    fulfilmentRequestEvent.setTransactionId(null);
    FulfilmentRequestDTO fulfilmentRequestPayload =
        managementEvent.getPayload().getFulfilmentRequest();
    fulfilmentRequestPayload.setCaseId(UUID.randomUUID().toString());

    // When
    underTest.logFulfilmentRequestedEvent(
        new Case(),
        UUID.fromString(fulfilmentRequestPayload.getCaseId()),
        fulfilmentRequestEvent.getDateTime(),
        "Fulfilment Request Received",
        EventType.FULFILMENT_REQUESTED,
        fulfilmentRequestPayload,
        fulfilmentRequestEvent);

    // Then
    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    FulfilmentRequestDTO actualFulfilment =
        convertJsonToFulfilmentRequestDTO(eventArgumentCaptor.getValue().getEventPayload());

    assertThat(actualFulfilment.getCaseId()).isEqualTo(fulfilmentRequestPayload.getCaseId());
    assertThat(actualFulfilment.getFulfilmentCode())
        .isEqualTo(fulfilmentRequestPayload.getFulfilmentCode());
  }
}