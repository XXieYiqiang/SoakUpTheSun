"use strict";
// Copyright 2018-2024 the Deno authors. All rights reserved. MIT license.
// This module is browser compatible.
Object.defineProperty(exports, "__esModule", { value: true });
exports.ServerSentEventStream = void 0;
const NEWLINE_REGEXP = /\r\n|\r|\n/;
const encoder = new TextEncoder();
function assertHasNoNewline(value, varName) {
    if (value.match(NEWLINE_REGEXP) !== null) {
        throw new RangeError(`${varName} cannot contain a newline`);
    }
}
/**
 * Converts a server-sent message object into a string for the client.
 *
 * @see {@link https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#event_stream_format}
 */
function stringify(message) {
    const lines = [];
    if (message.comment) {
        assertHasNoNewline(message.comment, "`message.comment`");
        lines.push(`:${message.comment}`);
    }
    if (message.event) {
        assertHasNoNewline(message.event, "`message.event`");
        lines.push(`event:${message.event}`);
    }
    if (message.data) {
        message.data.split(NEWLINE_REGEXP).forEach((line) => lines.push(`data:${line}`));
    }
    if (message.id) {
        assertHasNoNewline(message.id.toString(), "`message.id`");
        lines.push(`id:${message.id}`);
    }
    if (message.retry)
        lines.push(`retry:${message.retry}`);
    return encoder.encode(lines.join("\n") + "\n\n");
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
class ServerSentEventStream extends TransformStream {
    constructor() {
        super({
            transform: (message, controller) => {
                controller.enqueue(stringify(message));
            },
        });
    }
}
exports.ServerSentEventStream = ServerSentEventStream;
