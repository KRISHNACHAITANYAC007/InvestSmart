// New agent responses use decimal strings; never convert money through a JS floating point number.
export function agentMoney(value) {
    if (typeof value !== "string" || !/^-?\d+(?:\.\d{1,2})?$/.test(value)) return "Unavailable";
    const negative = value.startsWith("-");
    const [raw, fraction = ""] = value.replace(/^-/, "").split(".");
    const digits = raw.replace(/^0+(?=\d)/, "");
    const tail = digits.slice(-3), prefix = digits.slice(0, -3).replace(/\B(?=(\d{2})+$)/g, ",");
    return `${negative ? "-" : ""}₹${prefix ? `${prefix},` : ""}${tail}.${fraction.padEnd(2, "0")}`;
}

export function createAgentRequestGate() {
    let active = null;
    return {
        begin() { active?.abort(); active = new AbortController(); return active; },
        cancel() { active?.abort(); active = null; },
        isCurrent(request) { return active === request && !request.signal.aborted; },
        finish(request) {
            if (active !== request || request.signal.aborted) return false;
            active = null; return true;
        }
    };
}
export function appendAgentReply(messages, reply) { return [...messages.slice(-5), reply]; }
