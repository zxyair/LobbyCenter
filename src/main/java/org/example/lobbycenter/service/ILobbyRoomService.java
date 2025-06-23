package org.example.lobbycenter.service;

import org.example.common.pojo.Result;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public interface ILobbyRoomService {
    Result createRoom(String ownerId, Integer maxPlayers);

    Result joinRoom(String userId, String roomId);

    Result startGame(String roomId) throws IOException;

    Result leaveRoom(String userId, String roomId);

    Result dismissRoom(String roomId);

    Result listRoom(Integer page);
}
