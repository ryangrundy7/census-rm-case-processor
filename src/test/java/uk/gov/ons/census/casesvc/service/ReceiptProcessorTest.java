package uk.gov.ons.census.casesvc.service;

import static org.mockito.Mockito.*;
import static uk.gov.ons.census.casesvc.service.ReceiptProcessor.QID_RECEIPTED;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import uk.gov.ons.census.casesvc.model.dto.Receipt;
import uk.gov.ons.census.casesvc.model.entity.Case;
import uk.gov.ons.census.casesvc.model.entity.EventType;
import uk.gov.ons.census.casesvc.model.entity.UacQidLink;
import uk.gov.ons.census.casesvc.model.repository.CaseRepository;

public class ReceiptProcessorTest {
  private static final UUID TEST_CASE_ID = UUID.randomUUID();
  private static final String TEST_QID = "test_qid";
  private static final String TEST_UAC = "test_uac";

  @Test
  public void testGoodReceipt() {
    // Given
    UacQidLink expectedUacQidLink = getUacQidLink();

    Case expectedCase = getRandomCase();
    expectedCase.setUacQidLinks(Arrays.asList(expectedUacQidLink));
    CaseRepository caseRepository = mock(CaseRepository.class);
    when(caseRepository.findByCaseId(TEST_CASE_ID)).thenReturn(Optional.of(expectedCase));

    UacProcessor uacProcessor = mock(UacProcessor.class);
    CaseProcessor caseProcessor = mock(CaseProcessor.class);

    // when
    Receipt receipt = new Receipt();
    receipt.setCaseId(TEST_CASE_ID.toString());

    String dateTime = "2016-03-04T11:30Z";
    OffsetDateTime expectedReceiptDateTime = OffsetDateTime.parse(dateTime);
    receipt.setResponseDateTime(expectedReceiptDateTime);

    ReceiptProcessor receiptProcessor =
        new ReceiptProcessor(caseProcessor, caseRepository, uacProcessor);
    receiptProcessor.processReceipt(receipt);

    // then
    verify(uacProcessor, times(1)).emitUacUpdatedEvent(expectedUacQidLink, expectedCase, false);
    verify(uacProcessor, times(1))
        .logEvent(
            expectedUacQidLink,
            QID_RECEIPTED,
            EventType.UAC_UPDATED,
            receipt.getResponseDateTime());
  }

  @Test(expected = RuntimeException.class)
  public void testReceiptedCaseNotFound() {
    // Given
    CaseRepository caseRepository = mock(CaseRepository.class);
    CaseProcessor caseProcessor = mock(CaseProcessor.class);
    when(caseRepository.findById(TEST_CASE_ID)).thenReturn(Optional.empty());

    UacProcessor uacProcessor = mock(UacProcessor.class);

    // Given
    Receipt receipt = new Receipt();
    receipt.setCaseId(TEST_CASE_ID.toString());

    ReceiptProcessor receiptProcessor =
        new ReceiptProcessor(caseProcessor, caseRepository, uacProcessor);
    receiptProcessor.processReceipt(receipt);

    // Then
    // Expected Exception is raised

  }

  private UacQidLink getUacQidLink() {
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setCaze(null);
    uacQidLink.setUniqueNumber(123459876L);
    uacQidLink.setUac(TEST_UAC);
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setQid(TEST_QID);
    return uacQidLink;
  }

  private Case getRandomCase() {
    EasyRandom easyRandom = new EasyRandom();
    Case caze = easyRandom.nextObject(Case.class);
    caze.setCaseId(TEST_CASE_ID);

    return caze;
  }
}