package com.forge.dc.modules.ai.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageGenerationVO {

    private String imageUrl;
    private String revisedPrompt;
}
