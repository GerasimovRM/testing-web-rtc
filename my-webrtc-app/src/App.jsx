import { useEffect, useState } from "react";
import VideoCall from "./VideoCall";

const App = () => {
  const [socket, setSocket] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const [userId, setUserId] = useState("");
  const [inputValue, setInputValue] = useState("");

  useEffect(() => {
    if (!isConnected || !userId) return;

    const ws = new WebSocket("wss://192.168.0.4:8080/video-call");
    setSocket(ws);

    console.log("UserId:", userId);

    ws.onopen = () => console.log("WebSocket подключен");
    ws.onclose = () => console.log("WebSocket отключен");

    return () => ws.close();
  }, [isConnected, userId]);

  return (
      <div className="p-4">
        <input
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="Введите ваш User ID"
            className="border rounded p-2 mr-2"
        />
        <button
            onClick={() => setUserId(inputValue)}
            disabled={!inputValue.trim()}
            className="bg-blue-500 text-white px-4 py-2 rounded disabled:bg-gray-400"
        >
          Установить User ID
        </button>

        {userId && (
            <>
              <p className="mt-2">User ID: {userId}</p>
              <button
                  onClick={() => setIsConnected(true)}
                  className="bg-green-500 text-white px-4 py-2 rounded mt-2"
              >
                Подключиться
              </button>
            </>
        )}

        {socket ? <VideoCall socket={socket} userId={userId} /> : <p>Ожидание подключения...</p>}
      </div>
  );
};

export default App;
