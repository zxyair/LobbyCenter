package org.example.lobbycenter.service;


import org.example.lobbycenter.pojo.Result;
import org.springframework.stereotype.Service;

@Service
public interface ILobbyMatchService {
    Result joinMatch(String userId);

    Result cancelMatch(String userId);

    Result getMatchQueueStatus();
}
