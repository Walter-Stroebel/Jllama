package nl.infcomtec.jllama;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Responses can be partial (one word) or full. This is the base or partial
 * response.
 */
public class StreamedResponse {

    public String model;
    @JsonProperty(value = "created_at")
    public LocalDateTime createdAt;
    /**
     * if streamed, this will contain the a partial response. Else this will be
     * the full response
     */
    public String response;
    public boolean done;

}
