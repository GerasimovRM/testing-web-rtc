import { useEffect, useState, useMemo } from "react";
import VideoCall from "./VideoCall";

const App = () => {
  const [socket, setSocket] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const userId = useMemo(() => crypto.randomUUID(), []);

  useEffect(() => {
    if (!isConnected) return;

    const ws = new WebSocket("ws://localhost:8080/video-call");
    setSocket(ws);

    console.log("UserId:", userId);

    ws.onopen = () => console.log("WebSocket подключен");
    ws.onclose = () => console.log("WebSocket отключен");

    return () => ws.close();
  }, [isConnected]);

  return (
    <div>
      <button onClick={() => setIsConnected(true)}>Подключиться</button>
      {socket ? <VideoCall socket={socket} userId={userId} /> : <p>Подключаемся...</p>}
    </div>
  );
};

export default App;
