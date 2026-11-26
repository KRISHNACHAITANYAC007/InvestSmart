// Preview availability is separate from submission confirmation and never grants execution.
export function previewFailure(failure, now = Date.now()) {
    const status = failure.response?.status;
    const retryHeader = failure.response?.headers?.['retry-after'];
    const seconds = Number(retryHeader);
    const date = typeof retryHeader === 'string' ? Date.parse(retryHeader) : NaN;
    const delay = [429,503].includes(status) ? (Number.isFinite(seconds) && seconds > 0 ? seconds * 1000 : Number.isFinite(date) ? Math.max(1000, date - now) : status === 429 ? 60000 : 15000) : status === 500 ? 30000 : 15000;
    return {
        message: status === 429 ? 'Preview requests are temporarily limited. Wait for the countdown before retrying.'
            : status === 503 ? failure.response.data?.message || 'Fresh executable market data is unavailable. Retry when prices are available.'
            : status === 500 ? 'The estimate could not be calculated. Retry shortly; no order was placed.'
            : failure.response?.data?.message || 'Preview could not be reached. Check your connection and retry.',
        retryAt: now + delay,
    };
}

export function previewIsFresh(value, now = Date.now()) {
    return value != null && Number.isFinite(Date.parse(value.expiresAt)) && Date.parse(value.expiresAt) > now;
}
