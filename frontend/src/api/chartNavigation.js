export const CHART_PERIODS=['1D','1W','1M','6M','1Y','5Y','ALL'];
export const chartPeriod=value=>CHART_PERIODS.includes(value)?value:'1D';
export function chartViewPath(symbol,instrumentKey,period,full=false){const query=new URLSearchParams();if(instrumentKey)query.set('instrumentKey',instrumentKey);query.set('period',chartPeriod(period));return '/market/chart/'+encodeURIComponent(symbol)+(full?'/full':'')+'?'+query.toString();}
