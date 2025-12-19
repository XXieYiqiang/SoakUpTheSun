/** Options for {@linkcode TextLineStream}. */
export interface TextLineStreamOptions {
    /**
     * Allow splitting by `\r`.
     *
     * @default {false}
     */
    allowCR?: boolean;
}
/**
 * Transform a stream into a stream where each chunk is divided by a newline,
 * be it `\n` or `\r\n`. `\r` can be enabled via the `allowCR` option.
 *
 * @example
 * ```ts
 * import { TextLineStream } from "@std/streams/text-line-stream";
 *
 * const res = await fetch("https://example.com");
 * const lines = res.body!
 *   .pipeThrough(new TextDecoderStream())
 *   .pipeThrough(new TextLineStream());
 * ```
 */
export declare class TextLineStream extends TransformStream<string, string> {
    #private;
    /** Constructs a new instance. */
    constructor(options?: TextLineStreamOptions);
}
//# sourceMappingURL=text_line_stream.d.ts.map