package nl.infcomtec.jllama;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a request to an Ollama model, allowing customization of model
 * parameters and inclusion of system and user prompts. This Java class
 * encapsulates various options that can be adjusted to modify how the model
 * generates its response.
 */
public class Request {

    /**
     * The name of the model to use for generating a response. This field is
     * required.
     */
    public String model;

    /**
     * The prompt for which the model should generate a response.
     */
    public String prompt;

    /**
     * Specifies the format of the response. Currently, the only supported value
     * is "json".
     */
    public String format;

    /**
     * An array of base64-encoded images, for use with multimodal models such as
     * llava.
     */
    public String[] images;

    /**
     * System prompt that overrides what is defined in the Modelfile, used to
     * specify custom behavior of the model.
     */
    public String system;

    /**
     * The full prompt or prompt template. Overrides what is defined in the
     * Modelfile.
     */
    public String template;

    /**
     * If true, no formatting will be applied to the prompt, and no context will
     * be returned. Use this when specifying a full templated prompt in your
     * request to the API and managing history yourself.
     */
    public Boolean raw = false;

    /**
     * If false, the response will be returned as a single response object,
     * rather than a stream of objects.
     */
    public Boolean stream = false;

    /**
     * Specifies the context tokens for the model to consider when generating a
     * response.
     */
    public Integer[] context;

    /**
     * Specifies the time the Ollama service should keep the model loaded. Can
     * be specified in minutes (m), seconds (s), or hours (h). The default is
     * 5m. A negative value indicates an infinite keep-alive period.
     */
    @JsonProperty(value = "keep_alive")
    public String keepAlive;

    /**
     * Additional model parameters as documented for the Modelfile, such as
     * temperature and other settings that affect the generation.
     */
    public Options options;

    /**
     * Encapsulates various options for adjusting the behavior of the model.
     * These include parameters related to generation such as temperature, token
     * limits, and penalties for repetition.
     */
    public static class Options {

        /**
         * Number of tokens to keep from the input, when generating
         * continuations.
         */
        @JsonProperty(value = "num_keep")
        public Integer numKeep;

        /**
         * Random seed used for generating responses. Setting this value ensures
         * consistent output for identical inputs.
         */
        public Integer seed;

        /**
         * Maximum number of tokens the model is allowed to generate in response
         * to the prompt.
         */
        @JsonProperty(value = "num_predict")
        public Integer numPredict;

        /**
         * Limits the model to considering only the top-k most likely tokens at
         * each step. A lower value increases focus but decreases creativity.
         */
        @JsonProperty(value = "top_k")
        public Integer topK;

        /**
         * At each step of generation, only considers the smallest set of tokens
         * whose cumulative probability exceeds the threshold p. Works with
         * top_k to control the randomness and focus of the generation.
         */
        @JsonProperty(value = "top_p")
        public Double topP;

        /**
         * Tail Free Sampling parameter to reduce the likelihood of low
         * probability tokens being chosen. Higher values increase focus on
         * higher probability tokens.
         */
        @JsonProperty(value = "tfs_z")
        public Double tfsZ;

        /**
         * Adjusts token probabilities to more typical values before sampling.
         * Higher values can make the model output more predictable.
         */
        @JsonProperty(value = "typical_p")
        public Double typicalP;

        /**
         * Determines how many of the most recent tokens to consider for
         * repetition penalties. Helps to avoid repetitive outputs.
         */
        @JsonProperty(value = "repeat_last_n")
        public Integer repeatLastN;

        /**
         * Controls the randomness of the generation. Lower values make the
         * output more deterministic and coherent, while higher values encourage
         * creativity and variability.
         */
        public Double temperature;

        /**
         * Increases the penalty for generating tokens that have been generated
         * recently, to reduce repetition.
         */
        @JsonProperty(value = "repeat_penalty")
        public Double repeatPenalty;

        /**
         * Penalty applied to tokens generated more frequently in the context,
         * to encourage diversity.
         */
        @JsonProperty(value = "presence_penalty")
        public Double presencePenalty;

        /**
         * Penalty for tokens based on their frequency in the dataset, to reduce
         * common phrases and encourage novel generation.
         */
        @JsonProperty(value = "frequency_penalty")
        public Double frequencyPenalty;

        /**
         * Enables Mirostat sampling to control the model's perplexity, with
         * higher values encouraging more creativity.
         */
        public Integer mirostat;

        /**
         * Adjusts the Mirostat algorithm's tolerance for deviation from the
         * target perplexity.
         */
        @JsonProperty(value = "mirostat_tau")
        public Double mirostatTau;

        /**
         * Learning rate for the Mirostat algorithm, controlling how quickly it
         * adjusts based on feedback.
         */
        @JsonProperty(value = "mirostat_eta")
        public Double mirostatEta;

        /**
         * If true, penalizes the generation of new lines, encouraging more
         * compact outputs.
         */
        @JsonProperty(value = "penalize_newline")
        public Boolean penalizeNewline;

        /**
         * Sequences that, when generated, signal the model to stop generating
         * further tokens.
         */
        public String[] stop;

        /**
         * If true, enables Non-Uniform Memory Access optimization for better
         * performance on compatible systems.
         */
        public Boolean numa;

        /**
         * Sets the maximum number of tokens the model can use as context when
         * generating a response.
         */
        @JsonProperty(value = "num_ctx")
        public Integer numCtx;

        /**
         * Specifies the number of predictions (batches) to generate
         * simultaneously. Higher values require more computational resources.
         */
        @JsonProperty(value = "num_batch")
        public Integer numBatch;

        /**
         * Number of GQA groups in the transformer layer, required for specific
         * models.
         */
        @JsonProperty(value = "num_gqa")
        public Integer numGqa;

        /**
         * Specifies how many layers of the model should be processed on the
         * GPU. Can significantly affect performance and resource usage.
         */
        @JsonProperty(value = "num_gpu")
        public Integer numGpu;

        /**
         * Designates the main GPU for processing when multiple GPUs are
         * available. Relevant for systems with more than one GPU.
         */
        @JsonProperty(value = "main_gpu")
        public Integer mainGpu;

        /**
         * If true, optimizes processing for systems with low VRAM, potentially
         * reducing the model's memory footprint.
         */
        @JsonProperty(value = "low_vram")
        public Boolean lowVram;

        /**
         * If true, uses 16-bit floating-point for key/values in self-attention
         * layers, reducing memory usage at a potential cost to accuracy.
         */
        @JsonProperty(value = "f16_kv")
        public Boolean f16Kv;

        /**
         * If true, returns logits for all tokens, not just the generated text.
         * Useful for detailed analysis of model predictions.
         */
        @JsonProperty(value = "logits_all")
        public Boolean logitsAll;

        /**
         * If true, restricts model predictions to tokens found in the
         * vocabulary, potentially improving performance with a slight impact on
         * flexibility.
         */
        @JsonProperty(value = "vocab_only")
        public Boolean vocabOnly;

        /**
         * If true, uses memory-mapped files for model weights, reducing initial
         * load time at the expense of higher disk IO during generation.
         */
        @JsonProperty(value = "use_mmap")
        public Boolean useMmap;

        /**
         * If true, locks model weights in memory, preventing them from being
         * swapped to disk. Can improve performance at the cost of increased
         * memory usage.
         */
        @JsonProperty(value = "use_mlock")
        public Boolean useMlock;

        /**
         * If true, only initializes model embeddings, significantly reducing
         * memory usage. Suitable for tasks that only require embedding vectors.
         */
        @JsonProperty(value = "embedding_only")
        public Boolean embeddingOnly;

        /**
         * Base frequency for calculating rope penalties, which can adjust the
         * likelihood of certain tokens based on their frequency.
         */
        @JsonProperty(value = "rope_frequency_base")
        public Double ropeFrequencyBase;

        /**
         * Scale for rope frequency penalties, adjusting the impact of the base
         * frequency on token likelihood.
         */
        @JsonProperty(value = "rope_frequency_scale")
        public Double ropeFrequencyScale;

        /**
         * Specifies the number of threads the model should use for computation.
         * Adjusting this can affect performance and CPU usage.
         */
        @JsonProperty(value = "num_thread")
        public Integer numThread;
    }

}
