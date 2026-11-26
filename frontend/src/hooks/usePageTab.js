import { useSearchParams } from "react-router-dom";
export default function usePageTab(options,fallback){ const [params]=useSearchParams();const value=params.get("tab");return options.includes(value)?value:fallback; }
