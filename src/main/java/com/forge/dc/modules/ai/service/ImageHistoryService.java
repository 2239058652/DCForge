package com.forge.dc.modules.ai.service;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.modules.ai.dto.ImageHistoryPageDTO;
import com.forge.dc.modules.ai.dto.ImageHistorySaveDTO;
import com.forge.dc.modules.ai.vo.ImageHistoryVO;

public interface ImageHistoryService {

    ImageHistoryVO save(ImageHistorySaveDTO dto);

    PageResult<ImageHistoryVO> page(ImageHistoryPageDTO dto);

    void delete(Long id);
}
