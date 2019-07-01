package uk.gov.ons.census.casesvc.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.casesvc.client.UacQidServiceClient;
import uk.gov.ons.census.casesvc.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.casesvc.model.dto.UacQidDTO;
import uk.gov.ons.census.casesvc.model.entity.Case;
import uk.gov.ons.census.casesvc.model.entity.Event;
import uk.gov.ons.census.casesvc.model.entity.EventType;
import uk.gov.ons.census.casesvc.model.entity.UacQidLink;
import uk.gov.ons.census.casesvc.model.repository.EventRepository;
import uk.gov.ons.census.casesvc.model.repository.UacQidLinkRepository;

@RunWith(MockitoJUnitRunner.class)
public class UacProcessorTest {

  @Mock UacQidLinkRepository uacQidLinkRepository;

  @Mock EventRepository eventRepository;

  @Mock RabbitTemplate rabbitTemplate;

  @Mock UacQidServiceClient uacQidServiceClient;

  @InjectMocks UacProcessor underTest;

  @Test
  public void testSaveUacQidLinkEnglandHousehold() {
    // Given
    Case caze = new Case();

    UacQidDTO uacQidDTO = new UacQidDTO();
    uacQidDTO.setUac("testuac");
    uacQidDTO.setQid("01testqid");
    when(uacQidServiceClient.generateUacQid(anyInt())).thenReturn(uacQidDTO);

    // When
    UacQidLink result;
    result = underTest.saveUacQidLink(caze, 1);

    // Then
    assertEquals("01", result.getQid().substring(0, 2));
    verify(uacQidServiceClient).generateUacQid(eq(1));
  }

  @Test
  public void testLogEventWithoutEventMetaDataDateTime() {
    // Given
    UacQidLink uacQuidLink = new UacQidLink();

    // When
    underTest.logEvent(uacQuidLink, "TEST_LOGGED_EVENT", EventType.UAC_UPDATED);

    // Then
    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    assertEquals("TEST_LOGGED_EVENT", eventArgumentCaptor.getValue().getEventDescription());
    assertEquals(EventType.UAC_UPDATED, eventArgumentCaptor.getValue().getEventType());
  }

  @Test
  public void testLogEventWithEventMetaDataDateTime() {
    // Given
    UacQidLink uacQuidLink = new UacQidLink();
    OffsetDateTime now = OffsetDateTime.now();

    // When
    underTest.logEvent(
        uacQuidLink, "TEST_LOGGED_EVENT", EventType.UAC_UPDATED, any(OffsetDateTime.class));

    // Then
    ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventArgumentCaptor.capture());
    assertEquals("TEST_LOGGED_EVENT", eventArgumentCaptor.getValue().getEventDescription());
    assertEquals(EventType.UAC_UPDATED, eventArgumentCaptor.getValue().getEventType());
    assertEquals(
        now.toString().substring(0, 17),
        eventArgumentCaptor.getValue().getEventDate().toString().substring(0, 17));
  }

  @Test
  public void testEmitUacUpdatedEvent() {
    // Given
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setUac("12345");
    Case caze = new Case();
    UUID caseUuid = UUID.randomUUID();
    caze.setCaseId(caseUuid);
    ReflectionTestUtils.setField(underTest, "outboundExchange", "TEST_EXCHANGE");

    // When
    underTest.emitUacUpdatedEvent(uacQidLink, caze);

    // Then
    ArgumentCaptor<ResponseManagementEvent> responseManagementEventArgumentCaptor =
        ArgumentCaptor.forClass(ResponseManagementEvent.class);
    verify(rabbitTemplate)
        .convertAndSend(
            eq("TEST_EXCHANGE"),
            eq("event.uac.update"),
            responseManagementEventArgumentCaptor.capture());
    assertEquals(
        "12345", responseManagementEventArgumentCaptor.getValue().getPayload().getUac().getUac());
  }
}
