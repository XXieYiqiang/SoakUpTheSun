"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.events = events;
exports.stream = stream;
const utils = __importStar(require("./utils.js"));
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
async function* events(res, signal) {
    // TODO: throw error?
    if (!res.body)
        return;
    let iter = utils.stream(res.body);
    let line, reader = iter.getReader();
    let event;
    for (;;) {
        if (signal && signal.aborted) {
            return reader.cancel();
        }
        line = await reader.read();
        if (line.done)
            return;
        if (!line.value) {
            if (event)
                yield event;
            event = undefined;
            continue;
        }
        let [field, value] = (utils.split(line.value) || []);
        if (field === 'data') {
            event ||= {};
            event[field] = event[field] ? (event[field] + '\n' + value) : value;
        }
        else if (field === 'event') {
            event ||= {};
            event[field] = value;
        }
        else if (field === 'id') {
            event ||= {};
            event[field] = String(+value) === value ? +value : value;
        }
        else if (field === 'retry') {
            event ||= {};
            event[field] = +value || undefined;
        }
    }
}
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
async function stream(input, init) {
    let req = new Request(input, init);
    utils.fallback(req.headers, 'Accept', 'text/event-stream');
    utils.fallback(req.headers, 'Content-Type', 'application/json');
    let r = await fetch(req);
    if (!r.ok)
        throw r;
    return events(r, req.signal);
}
