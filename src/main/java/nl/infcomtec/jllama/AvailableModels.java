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
        @JsonProperty(value = "parent_model")
        public String parentModel;
    }

    public static class AvailableModel {

        public String name;
        public String model;
        @JsonProperty(value = "modified_at")
        public LocalDateTime modifiedAt;
        @JsonProperty(value = "expires_at")
        public LocalDateTime expiresAt;
        public long size;
        @JsonProperty(value = "size_vram")
        public long sizeVRAM;
        public String digest;
        public Details details;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("AvailableModel{");
            sb.append("name=").append(name);
            sb.append(", model=").append(model);
            sb.append(", modifiedAt=").append(modifiedAt);
            sb.append(", expiresAt=").append(expiresAt);
            sb.append(", size=").append(size);
            sb.append(", sizeVRAM=").append(sizeVRAM);
            sb.append(", digest=").append(digest);
            sb.append(", details=").append(details);
            sb.append('}');
            return sb.toString();
        }
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
