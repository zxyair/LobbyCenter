package org.example.lobbycenter.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
//todo 可以再Map中存储更多信息，比如分段、娱乐赛、匹配赛、排位赛等等
/*
* {
  "userId": "123456",
  "timestamp": 1718000000,
  "matchType": "normal"   // 可选，比如普通匹配/排位/自定义
}*/
//目前，为了简单起见，只存储用户ID
public class MatchEvent {

    private String topic;
    private String userId;
//    private Long entityId;
//    private Map<String, Object> data = new HashMap<>();

}