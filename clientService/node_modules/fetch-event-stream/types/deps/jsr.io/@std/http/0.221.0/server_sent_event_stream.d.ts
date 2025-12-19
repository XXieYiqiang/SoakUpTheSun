/**
 * Represents a message in the Server-Sent Event (SSE) protocol.
 *
 * @see {@link https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#fields}
 */
export interface ServerSentEventMessage {
    /** Ignored by the client. */
    comment?: string;
    /** A string identifying the type of event described. */
    event?: string;
    /** The data field for the message. Split by new lines. */
    data?: string;
    /** The event ID to set the {@linkcode EventSource} object's last event ID value. */
    id?: string | number;
    /** The reconnection time. */
    retry?: number;
}
/**
 * Transforms server-sent message objects into strings for the client.
 *
 * @see {@link https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events}
 *
 * @example
 * ```ts
 * import {
 *   type ServerSentEventMessage,
 *   ServerSentEventStream,
 * } from "@std/http/server-sent-event-stream";
 *
 * const stream = ReadableStream.from<ServerSentEventMessage>([
 *   { data: "hello there" }
 * ]).pipeThrough(new ServerSentEventStream());
 * new Response(stream, {
 *   headers: {
 *     "content-type": "text/event-stream",
 *     "cache-control": "no-cache",
 *   },
 * });
 * ```
 */
export declare class ServerSentEventStream extends TransformStream<ServerSentEventMessage, Uint8Array> {
    constructor();
}
//# sourceMappingURL=server_sent_event_stream.d.ts.map