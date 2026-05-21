package cn.rbac.server.common.pojo;

import lombok.Data;
import java.io.Serializable;

@Data
public class PageParam implements Serializable {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
