package uk.gov.ons.census.casesvc.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonHelper {
  private static final ObjectMapper objectMapper;

  static {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  public static String convertObjectToJson(Object receipt) {
    try {
      return objectMapper.writeValueAsString(receipt);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed converting Object To Json", e);
    }
  }
}
