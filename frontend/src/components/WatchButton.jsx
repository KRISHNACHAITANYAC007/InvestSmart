import { useState } from "react";

function WatchButton()
{
    const [watched,setWatched] = useState(false);
    return(
      <button onClick={()=>setWatched(!watched)}  >
          {watched ? "hello kc" : "hello sc"}
      </button>
    );
}
export default WatchButton;

