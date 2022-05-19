package com.lingfeng.biz.downloader.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class ResultInfo {
    String url;
    String body;
}
