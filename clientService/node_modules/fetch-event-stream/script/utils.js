"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.stream = stream;
exports.split = split;
exports.fallback = fallback;
const text_line_stream_js_1 = require("./deps/jsr.io/@std/streams/0.221.0/text_line_stream.js");
function stream(input) {
    let decoder = new TextDecoderStream();
    let split = new text_line_stream_js_1.TextLineStream({ allowCR: true });
    return input.pipeThrough(decoder).pipeThrough(split);
}
function split(input) {
    let rgx = /[:]\s*/;
    let match = rgx.exec(input);
    // ": comment" -> index=0 -> ignore
    let idx = match && match.index;
    if (idx) {
        return [
            input.substring(0, idx),
            input.substring(idx + match[0].length),
        ];
    }
}
function fallback(headers, key, value) {
    let tmp = headers.get(key);
    if (!tmp)
        headers.set(key, value);
}
