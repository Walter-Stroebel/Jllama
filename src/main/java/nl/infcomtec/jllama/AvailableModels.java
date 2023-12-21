package nl.infcomtec.jllama;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * JSON class.
 *
 * @author walter
 */
public class AvailableModels {

    public static class Details {

        public String format;
        public String family;
        public String[] families;
        @JsonProperty(value = "parameter_size")
        public String parameterSize;
        @JsonProperty(value = "quantization_level")
        public String quantizationLevel;
    }

    public static class AvailableModel {

        public String name;
        @JsonProperty(value = "modified_at")
        public LocalDateTime modifiedAt;
        public long size;
        public String digest;
        public Details details;
    }
    public AvailableModel[] models;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AvailableModels{");
        for (AvailableModel m : models) {
            sb.append("\n");
            sb.append(m.name).append(", ");
            sb.append(m.modifiedAt).append(", ");
            sb.append("+/- ").append(Math.round(m.size / 1E9)).append(" GB");
        }
        sb.append("\n}");
        return sb.toString();
    }

}
