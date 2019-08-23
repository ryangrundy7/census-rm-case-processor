package uk.gov.ons.census.casesvc.service;

import static uk.gov.ons.census.casesvc.utility.JsonHelper.convertObjectToJson;

import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.casesvc.logging.EventLogger;
import uk.gov.ons.census.casesvc.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.casesvc.model.dto.UacDTO;
import uk.gov.ons.census.casesvc.model.entity.Case;
import uk.gov.ons.census.casesvc.model.entity.EventType;
import uk.gov.ons.census.casesvc.model.entity.UacQidLink;

@Service
public class QuestionnaireLinkedService {
  private static final String QUESTIONNAIRE_LINKED = "Questionnaire Linked";

  private final UacService uacService;
  private final CaseService caseService;
  private final EventLogger eventLogger;

  public QuestionnaireLinkedService(
      UacService uacService, CaseService caseService, EventLogger eventLogger) {
    this.uacService = uacService;
    this.caseService = caseService;
    this.eventLogger = eventLogger;
  }

  public void processQuestionnaireLinked(ResponseManagementEvent questionnaireLinkedEvent) {
    UacDTO uac = questionnaireLinkedEvent.getPayload().getUac();
    UacQidLink uacQidLink = uacService.findByQid(uac.getQuestionnaireId());
    Case caze = caseService.getCaseByCaseId(UUID.fromString(uac.getCaseId()));

    // If UAC/QID has been receipted before case, update case
    if (!uacQidLink.isActive() && !caze.isReceiptReceived()) {
      caze.setReceiptReceived(true);
      caseService.saveAndEmitCaseUpdatedEvent(caze);
    }

    uacQidLink.setCaze(caze);
    uacService.saveAndEmitUacUpdatedEvent(uacQidLink);

    eventLogger.logUacQidEvent(
        uacQidLink,
        questionnaireLinkedEvent.getEvent().getDateTime(),
        QUESTIONNAIRE_LINKED,
        EventType.QUESTIONNAIRE_LINKED,
        questionnaireLinkedEvent.getEvent(),
        convertObjectToJson(uac));
  }
}