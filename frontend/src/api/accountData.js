export function createAccountDataLoader(client,onChange,{includeOrders=false,includeWatchlist=false}={}) {
    let state={portfolio:null,orders:[],watchlist:[],loading:true,refreshing:false,error:"",watchlistError:"",refreshInfo:null};
    let disposed=false, sequence=0, listController, priceWork;
    const priceController=new AbortController();
    function update(changes){ if(!disposed){state={...state,...changes};onChange(state);} }
    async function load(){
        if(disposed)return; const request=++sequence; listController?.abort(); listController=new AbortController();
        const signal=listController.signal;
        const responses=await Promise.allSettled([client.get("/portfolio/me",{signal}),includeOrders?client.get("/orders/me",{signal}):Promise.resolve(null),includeWatchlist?client.get("/watchlist/me",{signal}):Promise.resolve(null)]);
        if(disposed||request!==sequence)return;
        const changes={loading:false,error:responses[0].status==="rejected"?"Could not load your account. Showing the last loaded values.":""};
        if(responses[0].status==="fulfilled")changes.portfolio=responses[0].value.data;
        if(includeOrders&&responses[1].status==="fulfilled")changes.orders=responses[1].value.data;
        if(includeOrders&&responses[1].status==="rejected")changes.error="Could not refresh recent orders. Showing the last loaded values.";
        if(includeWatchlist){ changes.watchlistError=responses[2].status==="rejected"?"Could not load your watchlist.":""; if(responses[2].status==="fulfilled")changes.watchlist=responses[2].value.data; }
        update(changes);
    }
    function refresh(force=false){
        if(disposed)return Promise.resolve();
        const membership=load();
        if(!priceWork){
            update({refreshing:true});
            priceWork=(async()=>{
                try{const {data}=await client.post("/market/quotes/refresh",{}, {params:{force},signal:priceController.signal});update({refreshInfo:{...data,failed:false}});}
                catch{update({refreshInfo:{failed:true,staleSymbols:[]}});}
                if(!disposed)await load();
            })().finally(()=>{priceWork=null;update({refreshing:false});});
        }
        return Promise.all([membership,priceWork]);
    }
    return {refresh,reload:load,dispose(){disposed=true;sequence++;listController?.abort();priceController.abort();}};
}
