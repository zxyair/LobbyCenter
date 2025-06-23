package org.example.lobbycenter.service;


import org.example.common.pojo.Result;
import org.springframework.stereotype.Service;

@Service
public interface ILobbyMatchService {
    Result joinMatch(String userId);

    Result cancelMatch(String userId);

    Result getMatchQueueStatus();
}
