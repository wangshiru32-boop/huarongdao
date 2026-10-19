package com.example.huarongdao.score;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 通关后提交的原始数据。积分由服务端计算，客户端不能传 score。 */
public record ScoreRequest(
        @Min(value = 0, message = "阵型编号不合法。") @Max(value = 7, message = "阵型编号不合法。")
        int preset,
        @Min(value = 1, message = "步数不合法。") @Max(value = 2000, message = "步数不合法。")
        int moves,
        @Min(value = 1, message = "用时无效。") @Max(value = 86400, message = "用时无效。")
        int seconds) {
}
