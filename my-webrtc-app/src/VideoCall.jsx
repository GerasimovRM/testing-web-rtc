import {useEffect, useRef, useState} from "react";

const VideoCall = ({socket, userId}) => {
    const [peers, setPeers] = useState({});
    const localVideoRef = useRef(null);
    const peerConnections = useRef({});
    const iceCandidates = useRef({});

    useEffect(() => {
        const initStream = async () => {
            try {
                const stream = await navigator.mediaDevices.getUserMedia({video: true, audio: true});
                if (localVideoRef.current) {
                    localVideoRef.current.srcObject = stream;
                }

                console.log("Запрос доступа к видео: УСПЕХ");

                // Обработка WebSocket подключения
                if (socket.readyState === WebSocket.OPEN) {
                    console.log("WebSocket подключен, отправляем join");
                    socket.send(JSON.stringify({type: "join", userId}));
                } else {
                    socket.onopen = () => {
                        console.log("WebSocket подключен, отправляем join");
                        socket.send(JSON.stringify({type: "join", userId}));
                    };
                }

                socket.onmessage = async (event) => {
                    const message = JSON.parse(event.data);
                    switch (message.type) {
                        case "new-user":
                            if (message.userId !== userId) {
                                await createOffer(message.userId, stream);
                            } else {
                                console.log("Skip new-user because it's me");
                            }
                            break;
                        case "offer":
                            await handleOffer(message.targetUserId, message.sdp, stream);
                            break;
                        case "answer":
                            handleAnswer(message.targetUserId, message.sdp);
                            break;
                        case "candidate":
                            handleCandidate(message.targetUserId, message.candidate);
                            break;
                        case "user-left":
                            removePeer(message.userId);
                            break;
                        default:
                            console.log("Неизвестный тип сообщения:", message.type);
                    }
                };
            } catch (error) {
                console.error("Ошибка получения видео:", error);
            }
        };

        initStream();
    }, [socket, userId]);

    // Функция для создания PeerConnection
    const createPeerConnection = (userId) => {
        const pc = new RTCPeerConnection({
            iceServers: [{urls: "stun:stun.l.google.com:19302"}],
        });

        pc.onicecandidate = (event) => {
            if (event.candidate) {
                console.log(`ICE candidate для ${userId}:`, event.candidate);
                socket.send(JSON.stringify({
                    type: "candidate",
                    targetUserId: userId,
                    candidate: event.candidate,
                }));
                if (!iceCandidates.current[userId]) {
                    iceCandidates.current[userId] = [];
                }
                iceCandidates.current[userId].push(event.candidate);
            }
        };

        pc.ontrack = (event) => {
            setPeers((prev) => ({...prev, [userId]: event.streams[0]}));
        };

        return pc;
    };

    // Функция для создания Offer
    const createOffer = async (targetUserId, stream) => {
        try {
            const pc = createPeerConnection(targetUserId);
            peerConnections.current[targetUserId] = pc;

            stream.getTracks().forEach((track) => pc.addTrack(track, stream));

            const offer = await pc.createOffer();
            await pc.setLocalDescription(offer);

            console.log(`Отправляем offer для ${targetUserId}`);
            socket.send(JSON.stringify({type: "offer", targetUserId, sdp: offer}));
        } catch (error) {
            console.error("Ошибка при создании offer:", error);
        }
    };

    // Функция для обработки входящего offer
    const handleOffer = async (fromUserId, sdp, stream) => {
        try {
            const pc = createPeerConnection(fromUserId);
            peerConnections.current[fromUserId] = pc;

            stream.getTracks().forEach((track) => pc.addTrack(track, stream));

            await pc.setRemoteDescription(new RTCSessionDescription(sdp));
            const answer = await pc.createAnswer();
            await pc.setLocalDescription(answer);

            console.log(`Отправляем answer для ${fromUserId}`);
            socket.send(JSON.stringify({type: "answer", targetUserId: fromUserId, sdp: answer}));

            // Добавляем отложенные кандидаты
            if (iceCandidates.current[fromUserId]) {
                iceCandidates.current[fromUserId].forEach((candidate) => {
                    pc.addIceCandidate(new RTCIceCandidate(candidate));
                });
                delete iceCandidates.current[fromUserId]; // Очистить после добавления кандидатов
            }
        } catch (error) {
            console.error("Ошибка при обработке offer:", error);
        }
    };

    // Функция для обработки входящего answer
    const handleAnswer = (fromUserId, sdp) => {
        try {
            peerConnections.current[fromUserId]?.setRemoteDescription(new RTCSessionDescription(sdp));
        } catch (error) {
            console.error("Ошибка при обработке answer:", error);
        }
    };

    // Функция для обработки ICE кандидатов
    const handleCandidate = (fromUserId, candidate) => {
        try {
            console.log(`Добавляем ICE candidate для ${fromUserId}`);
            peerConnections.current[fromUserId]?.addIceCandidate(new RTCIceCandidate(candidate));

        } catch (error) {
            console.error("Ошибка при добавлении ICE candidate:", error);
        }
    };

    // Функция для удаления пира
    const removePeer = (userId) => {
        try {
            peerConnections.current[userId]?.close();
            setPeers((prev) => {
                const updatedPeers = {...prev};
                delete updatedPeers[userId];
                return updatedPeers;
            });
        } catch (error) {
            console.error("Ошибка при удалении пира:", error);
        }
    };

    return (
        <div className="grid grid-cols-3 gap-4 p-4">
            <div className="relative">
                <video ref={localVideoRef} autoPlay playsInline muted
                       className="w-full h-auto rounded-lg border border-gray-500"/>
                <p className="absolute bottom-2 left-2 bg-black text-white text-sm px-2 py-1 rounded">Вы</p>
            </div>

            {Object.entries(peers).map(([peerId, stream]) => (
                <div key={peerId} className="relative">
                    <video ref={(video) => video && (video.srcObject = stream)} autoPlay playsInline
                           className="w-full h-auto rounded-lg border border-gray-500"/>
                    <p className="absolute bottom-2 left-2 bg-black text-white text-sm px-2 py-1 rounded">{peerId}</p>
                </div>
            ))}
        </div>
    );
};

export default VideoCall;
