import { TextLineStream } from './deps/jsr.io/@std/streams/0.221.0/text_line_stream.js';
export function stream(input) {
    let decoder = new TextDecoderStream();
    let split = new TextLineStream({ allowCR: true });
    return input.pipeThrough(decoder).pipeThrough(split);
}
export function split(input) {
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
export function fallback(headers, key, value) {
    let tmp = headers.get(key);
    if (!tmp)
        headers.set(key, value);
}
