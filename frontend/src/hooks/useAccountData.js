import { useCallback, useEffect, useRef, useState } from "react";
import api from "../api/api.js";
import { createAccountDataLoader } from "../api/accountData.js";

export default function useAccountData({includeOrders=false,includeWatchlist=false}={}) {
    const [state,setState]=useState({portfolio:null,orders:[],watchlist:[],loading:true,refreshing:false,error:"",watchlistError:"",refreshInfo:null});
    const loaderRef=useRef(null);
    useEffect(()=>{
        const loader=createAccountDataLoader(api,setState,{includeOrders,includeWatchlist});loaderRef.current=loader;
        const initial=setTimeout(()=>{void loader.refresh();},0);
        let queued=false;
        const reload=()=>{if(queued)return;queued=true;queueMicrotask(()=>{queued=false;void loader.reload();});};
        const interval=setInterval(()=>{if(!document.hidden)void loader.refresh();},60000);
        window.addEventListener("investsmart-account-changed",reload);window.addEventListener("investsmart-watchlist-changed",reload);
        return ()=>{clearTimeout(initial);clearInterval(interval);loader.dispose();window.removeEventListener("investsmart-account-changed",reload);window.removeEventListener("investsmart-watchlist-changed",reload);};
    },[includeOrders,includeWatchlist]);
    const refresh=useCallback((force=true)=>loaderRef.current?.refresh(force),[]);
    const reload=useCallback(()=>loaderRef.current?.reload(),[]);
    return {...state,refresh,reload};
}
