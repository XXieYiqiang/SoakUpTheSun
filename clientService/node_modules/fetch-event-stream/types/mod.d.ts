import type { ServerSentEventMessage } from './deps/jsr.io/@std/http/0.221.0/server_sent_event_stream.js';
export type { ServerSentEventMessage };
/**
 * Convert a `Response` body containing Server Sent Events (SSE) into an Async Iterator that yields {@linkcode ServerSentEventMessage} objects.
 *
 * @see {@link https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events}
 *
 * @example
 * ```js
 * // Optional
 * let abort = new AbortController;
 *
 * // Manually fetch a Response
 * let res = await fetch('https://...', {
 *   method: 'POST',
 *   signal: abort.signal,
 *   headers: {
 *     'api-key': 'token <value>',
 *     'content-type': 'application/json',
 *   },
 *   body: JSON.stringify({
 *     stream: true, // <- hypothetical
 *     // ...
 *   })
 * });
 *
 * if (res.ok) {
 *   let stream = events(res, abort.signal);
 *   for await (let event of stream) {
 *     console.log('<<', event.data);
 *   }
 * }
 * ```
 */
export declare function events(res: Response, signal?: AbortSignal | null): AsyncGenerator<ServerSentEventMessage, void, unknown>;
/**
 * Convenience function that will `fetch` with the given arguments and, if ok, will return the {@linkcode events} async iterator.
 *
 * If the response is not ok (status 200-299), the `Response` is thrown.
 *
 * @example
 * ```js
 * // NOTE: throws `Response` if not 2xx status
 * let events = await stream('https://api.openai.com/...', {
 *   method: 'POST',
 *   headers: {
 *     'Authorization': 'Bearer <token>',
 *     'Content-Type': 'application/json',
 *   },
 *   body: JSON.stringify({
 *     stream: true,
 *     // ...
 *   })
 * });
 *
 * for await (let event of events) {
 *   console.log('<<', JSON.parse(event.data));
 * }
 * ```
 */
export declare function stream(input: RequestInfo | URL, init?: RequestInit): Promise<AsyncGenerator<ServerSentEventMessage, void, unknown>>;
//# sourceMappingURL=mod.d.ts.map