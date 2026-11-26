export const PAPER_PRODUCTS = [
    { value: 'CASH', label: 'Delivery' },
    { value: 'MIS', label: 'Intraday' },
    { value: 'MTF', label: 'MTF' },
    { value: 'COVER', label: 'Cover' },
];
export function styleUnavailable(product, style) {
    if (product === 'MIS' && style === 'ICEBERG') return 'Intraday Iceberg is unavailable; use Quick or Regular.';
    if (['MTF', 'COVER'].includes(product) && style !== 'REGULAR') return 'MTF and Cover use Regular orders.';
    return '';
}
export function newProductDraft(draft, product) {
    return { ...draft, product, tab: 'REGULAR', kind: 'MARKET', validity: 'DAY', stop: product === 'COVER' ? '10' : '', target: '', exitMode: 'PERCENT', limit: '', trigger: '' };
}
