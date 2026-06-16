package com.forge.dc.modules.ai.service;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.modules.ai.dto.TaskPageDTO;
import com.forge.dc.modules.ai.dto.TaskSubmitDTO;
import com.forge.dc.modules.ai.vo.TaskVO;

public interface TaskService {

    TaskVO submit(TaskSubmitDTO dto);

    TaskVO getTask(Long taskId);

    PageResult<TaskVO> page(TaskPageDTO dto);
}
