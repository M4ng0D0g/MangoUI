package com.myudog.myulib.api.event;

public enum ProcessResult {
    SUCCESS, // 內部處裡成功，但是繼續交給下一位
    CONSUME, // 內部處裡成功或完畢，吸收事件

    PASS,    // 內部處裡失敗或不處理，繼續交給下一位
    FAILED,  // 內部處裡失敗，吸收事件
}
