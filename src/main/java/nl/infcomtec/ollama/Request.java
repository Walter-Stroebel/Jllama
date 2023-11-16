package nl.infcomtec.ollama;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request JSON.
 */
public class Request {

    /**
     * (required) the model name
     */
    public String model;
    /**
     * the prompt to generate a response for
     */
    public String prompt;
    /**
     * the format to return a response in. Currently the only accepted value is
     * "json"
     */
    public String format;
    /**
     * system prompt (overrides what is defined in the Modelfile)
     */
    public String system;
    /**
     * the full prompt or prompt template (overrides what is defined in the
     * Modelfile)
     */
    public String template;
    /**
     * if true no formatting will be applied to the prompt and no context will
     * be returned. You may choose to use the raw parameter if you are
     * specifying a full templated prompt in your request to the API, and are
     * managing history yourself.
     */
    public Boolean raw = false;
    /**
     * if false the response will be returned as a single response object,
     * rather than a stream of objects
     */
    public Boolean stream = false;
    public Integer[] context;
    /**
     * additional model parameters listed in the documentation for the Modelfile
     * such as temperature
     */
    public Options options;

    public static class Options {

        @JsonProperty(value = "num_keep")
        public Integer numKeep;
        public Integer seed;
        @JsonProperty(value = "num_predict")
        public Integer numPredict;
        @JsonProperty(value = "top_k")
        public Integer topK;
        @JsonProperty(value = "top_p")
        public Double topP;
        @JsonProperty(value = "tfs_z")
        public Double tfsZ;
        @JsonProperty(value = "typical_p")
        public Double typicalP;
        @JsonProperty(value = "repeat_last_n")
        public Integer repeatLastN;
        public Double temperature;
        @JsonProperty(value = "repeat_penalty")
        public Double repeatPenalty;
        @JsonProperty(value = "presence_penalty")
        public Double presencePenalty;
        @JsonProperty(value = "frequency_penalty")
        public Double frequencyPenalty;
        public Integer mirostat;
        @JsonProperty(value = "mirostat_tau")
        public Double mirostatTau;
        @JsonProperty(value = "mirostat_eta")
        public Double mirostatEta;
        @JsonProperty(value = "penalize_newline")
        public Boolean penalizeNewline;
        public String[] stop;
        public Boolean numa;
        @JsonProperty(value = "num_ctx")
        public Integer numCtx;
        @JsonProperty(value = "num_batch")
        public Integer numBatch;
        @JsonProperty(value = "num_gqa")
        public Integer numGqa;
        @JsonProperty(value = "num_gpu")
        public Integer numGpu;
        @JsonProperty(value = "main_gpu")
        public Integer mainGpu;
        @JsonProperty(value = "low_vram")
        public Boolean lowVram;
        @JsonProperty(value = "f16_kv")
        public Boolean f16Kv;
        @JsonProperty(value = "logits_all")
        public Boolean logitsAll;
        @JsonProperty(value = "vocab_only")
        public Boolean vocabOnly;
        @JsonProperty(value = "use_mmap")
        public Boolean useMmap;
        @JsonProperty(value = "use_mlock")
        public Boolean useMlock;
        @JsonProperty(value = "embedding_only")
        public Boolean embeddingOnly;
        @JsonProperty(value = "rope_frequency_base")
        public Double ropeFrequencyBase;
        @JsonProperty(value = "rope_frequency_scale")
        public Double ropeFrequencyScale;
        @JsonProperty(value = "num_thread")
        public Integer numThread;
    }

}
