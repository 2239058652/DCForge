package com.forge.dc.modules.ai.service;

import com.forge.dc.modules.ai.dto.ImageToImageDTO;
import com.forge.dc.modules.ai.dto.TextToImageDTO;
import com.forge.dc.modules.ai.vo.ImageGenerationVO;

public interface ImageGenerationService {

    ImageGenerationVO textToImage(TextToImageDTO dto) throws java.io.IOException;

    ImageGenerationVO imageToImage(ImageToImageDTO dto) throws java.io.IOException;
}
